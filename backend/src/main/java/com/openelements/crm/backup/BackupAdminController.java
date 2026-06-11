package com.openelements.crm.backup;

import com.openelements.spring.base.security.roles.RequiresItAdmin;
import com.openelements.spring.base.services.dbbackup.BackupDownload;
import com.openelements.spring.base.services.dbbackup.BackupMetadata;
import com.openelements.spring.base.services.dbbackup.BackupServiceInfo;
import com.openelements.spring.base.services.dbbackup.BackupTrigger;
import com.openelements.spring.base.services.dbbackup.BackupTriggerResult;
import com.openelements.spring.base.services.dbbackup.DbBackupClient;
import com.openelements.spring.base.services.dbbackup.DbBackupException;
import com.openelements.spring.base.services.dbbackup.DbBackupProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Admin-only REST controller that proxies the {@code DbBackupClient} from
 * {@code com.open-elements:spring-services}. Lets an IT admin verify the backup strategy:
 * show service health and metadata, trigger a backup, list backups, and download a backup file.
 *
 * <p>All upstream failures collapse to a generic HTTP 503 response with body
 * {@code { "error": "Backup-Service nicht verfügbar" }} — the underlying distinction
 * (auth, network, mis-config) is kept in the backend log only.
 */
@RestController
@RequestMapping("/api/admin/backup")
@Tag(name = "Backup Admin", description = "Admin operations for the db-backup-service sidecar")
@SecurityRequirement(name = "oidc")
@RequiresItAdmin
public class BackupAdminController {

    private static final Logger LOG = LoggerFactory.getLogger(BackupAdminController.class);

    private static final String UPSTREAM_UNAVAILABLE_MESSAGE = "Backup-Service nicht verfügbar";

    private static final DateTimeFormatter FILENAME_TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss'Z'").withZone(ZoneOffset.UTC);

    private final DbBackupClient client;
    private final DbBackupProperties properties;

    /**
     * Creates a new {@code BackupAdminController}.
     *
     * @param client     the upstream client; supplied by spring-services autoconfig
     * @param properties the bound {@code openelements.db-backup.*} properties
     */
    public BackupAdminController(final DbBackupClient client, final DbBackupProperties properties) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * Combined health + info for the backup service. Always answers HTTP 200; the four resulting
     * shapes ({@code !configured}, {@code unhealthy}, {@code healthy && info==null},
     * {@code healthy && info!=null}) are interpreted by the frontend.
     *
     * @return the status DTO
     */
    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get backup service status")
    @ApiResponse(responseCode = "200", description = "Status returned")
    @ApiResponse(responseCode = "403", description = "Caller lacks the IT-ADMIN role")
    public BackupStatusDto getStatus() {
        if (isApiTokenBlank()) {
            return new BackupStatusDto(false, false, null);
        }
        final boolean healthy = client.isHealthy();
        if (!healthy) {
            return new BackupStatusDto(true, false, null);
        }
        BackupServiceInfo info = null;
        try {
            info = client.getInfo();
        } catch (final DbBackupException e) {
            LOG.warn("Backup service /info call failed: {}", e.getMessage());
        }
        return new BackupStatusDto(true, true, info);
    }

    /**
     * Triggers a new backup or returns the in-flight job's id when the sidecar reports
     * {@code alreadyRunning}. Upstream failures are translated to HTTP 503 by
     * {@link #handleDbBackupException(DbBackupException)}.
     *
     * @return the trigger result DTO
     */
    @PostMapping(value = "/trigger", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger a backup")
    @ApiResponse(responseCode = "200", description = "Backup triggered or already running")
    @ApiResponse(responseCode = "403", description = "Caller lacks the IT-ADMIN role")
    @ApiResponse(responseCode = "503", description = "Backup service is not reachable")
    public BackupTriggerDto trigger() {
        final BackupTriggerResult result = client.triggerBackup();
        return new BackupTriggerDto(result.job().jobId(), result.alreadyRunning());
    }

    /**
     * Lists all backups as returned by the sidecar (newest first).
     *
     * @return the list of backups, possibly empty
     */
    @GetMapping(value = "/backups", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List backups")
    @ApiResponse(responseCode = "200", description = "List returned (may be empty)")
    @ApiResponse(responseCode = "403", description = "Caller lacks the IT-ADMIN role")
    @ApiResponse(responseCode = "503", description = "Backup service is not reachable")
    public List<BackupItemDto> listBackups() {
        return client.listBackups().stream().map(BackupAdminController::toDto).toList();
    }

    /**
     * Streams a single backup file. The filename includes the backup's {@code createdAt} timestamp
     * (colons replaced by hyphens for Windows compatibility) and id. When the metadata lookup
     * fails the controller falls back to {@code backup-<id>.sql.gz}.
     *
     * <p>If the upstream call fails <em>before</em> the first byte is written the exception
     * propagates and the global handler returns HTTP 503. Failures during streaming truncate the
     * response — the browser sees a short file, which the admin can detect via {@code sha256sum}.
     *
     * @param id the backup identifier
     * @return the streamed response entity
     */
    @GetMapping("/backups/{id}/download")
    @Operation(summary = "Download a backup file")
    @ApiResponse(responseCode = "200", description = "Backup streamed as gzip")
    @ApiResponse(responseCode = "403", description = "Caller lacks the IT-ADMIN role")
    @ApiResponse(responseCode = "503", description = "Backup service is not reachable")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable("id") final String id) {
        final String filename = "backup-" + resolveFilenameSuffix(id) + ".sql.gz";
        final BackupDownload download = client.downloadBackup(id);

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/gzip"));
        if (download.sizeBytes() >= 0) {
            headers.setContentLength(download.sizeBytes());
        }
        headers.setContentDispositionFormData("attachment", filename);

        final StreamingResponseBody body = out -> stream(download, out);
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(body);
    }

    /**
     * Translates every {@link DbBackupException} reaching the controller layer into a single
     * generic HTTP 503 response, per the spec's "no distinct error states" decision.
     *
     * @param e the upstream exception
     * @return the generic 503 response
     */
    @ExceptionHandler(DbBackupException.class)
    public ResponseEntity<Map<String, String>> handleDbBackupException(final DbBackupException e) {
        LOG.warn("Backup service call failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", UPSTREAM_UNAVAILABLE_MESSAGE));
    }

    private String resolveFilenameSuffix(final String id) {
        try {
            for (final BackupMetadata meta : client.listBackups()) {
                if (id.equals(meta.id()) && meta.createdAt() != null) {
                    return FILENAME_TIMESTAMP.format(meta.createdAt()) + "-" + id;
                }
            }
        } catch (final DbBackupException e) {
            LOG.warn("Could not resolve backup metadata for filename of {}: {}", id, e.getMessage());
        }
        return id;
    }

    private static void stream(final BackupDownload download, final OutputStream out) throws IOException {
        try (BackupDownload owned = download; InputStream in = owned.stream()) {
            in.transferTo(out);
        }
    }

    private boolean isApiTokenBlank() {
        final String token = properties.apiToken();
        return token == null || token.isBlank();
    }

    private static BackupItemDto toDto(final BackupMetadata m) {
        return new BackupItemDto(
            m.id(),
            m.createdAt(),
            m.sizeBytes(),
            m.sha256(),
            m.pgVersion(),
            m.durationMs(),
            toTriggerString(m.triggeredBy())
        );
    }

    private static String toTriggerString(final BackupTrigger trigger) {
        return trigger == null ? null : switch (trigger) {
            case API -> "api";
            case SCHEDULER -> "scheduler";
        };
    }
}
