package com.openelements.crm.backup;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.openelements.crm.AbstractDbTest;
import com.openelements.spring.base.services.dbbackup.BackupDownload;
import com.openelements.spring.base.services.dbbackup.BackupJob;
import com.openelements.spring.base.services.dbbackup.BackupJobStatus;
import com.openelements.spring.base.services.dbbackup.BackupMetadata;
import com.openelements.spring.base.services.dbbackup.BackupServiceInfo;
import com.openelements.spring.base.services.dbbackup.BackupTrigger;
import com.openelements.spring.base.services.dbbackup.BackupTriggerResult;
import com.openelements.spring.base.services.dbbackup.DbBackupClient;
import com.openelements.spring.base.services.dbbackup.DbBackupException;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Behaviour tests for the spec-107 BackupAdminController.
 *
 * <p>Role-based 401/403/200 coverage lives in
 * {@link com.openelements.crm.security.SecurityRoleIntegrationTest}; this class focuses on the
 * controller contract for the four endpoints.
 *
 * <p>Two nested test scenarios — one with a configured API token and one without — are needed
 * because {@code DbBackupProperties} is bound at context start; the not-configured case lives in
 * {@link BackupAdminControllerNotConfiguredTest}.
 */
@TestPropertySource(properties = {
    "openelements.db-backup.api-token=test-token",
    "openelements.db-backup.base-url=http://localhost:65535"
})
class BackupAdminControllerTest extends AbstractDbTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DbBackupClient client;

    private static MockHttpServletRequestBuilder asItAdmin(MockHttpServletRequestBuilder builder) {
        final List<String> roles = List.of("IT-ADMIN");
        final Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test-user")
            .claim("preferred_username", "test-user")
            .claim("email", "test@example.com")
            .claim("roles", roles)
            .build();
        final Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (final String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return builder.with(jwt().jwt(jwt).authorities(authorities));
    }

    private static BackupDownload newDownload(final String id, final long size, final byte[] body) {
        try {
            final Constructor<BackupDownload> ctor = BackupDownload.class.getDeclaredConstructor(
                String.class, long.class, java.io.InputStream.class, java.io.Closeable.class);
            ctor.setAccessible(true);
            return ctor.newInstance(id, size, new ByteArrayInputStream(body), (java.io.Closeable) () -> {});
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate BackupDownload via reflection", e);
        }
    }

    // -- /status --

    @Test
    void statusReturnsHealthyAndInfo() throws Exception {
        when(client.isHealthy()).thenReturn(true);
        when(client.getInfo()).thenReturn(new BackupServiceInfo(
            "1.2.3",
            "17.2",
            new BackupServiceInfo.Retention(30),
            new BackupServiceInfo.BackupInterval("P1D", 86400),
            new BackupServiceInfo.Backup(3600)));

        mockMvc.perform(asItAdmin(get("/api/admin/backup/status")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.healthy").value(true))
            .andExpect(jsonPath("$.info.version").value("1.2.3"))
            .andExpect(jsonPath("$.info.pgDumpVersion").value("17.2"))
            .andExpect(jsonPath("$.info.retention.days").value(30))
            .andExpect(jsonPath("$.info.backupInterval.iso8601").value("P1D"))
            .andExpect(jsonPath("$.info.backup.lastSuccessfulBackupAgeSeconds").value(3600));
    }

    @Test
    void statusReturnsHealthyButNullInfoWhenInfoThrows() throws Exception {
        when(client.isHealthy()).thenReturn(true);
        when(client.getInfo()).thenThrow(new DbBackupException("info boom"));

        mockMvc.perform(asItAdmin(get("/api/admin/backup/status")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.healthy").value(true))
            .andExpect(jsonPath("$.info").doesNotExist());
    }

    @Test
    void statusReturnsUnhealthyWhenHealthFalseAndDoesNotCallGetInfo() throws Exception {
        when(client.isHealthy()).thenReturn(false);

        mockMvc.perform(asItAdmin(get("/api/admin/backup/status")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.healthy").value(false))
            .andExpect(jsonPath("$.info").doesNotExist());

        verify(client, never()).getInfo();
    }

    // -- /trigger --

    @Test
    void triggerSuccessReturnsJobIdAndAlreadyRunningFalse() throws Exception {
        when(client.triggerBackup()).thenReturn(new BackupTriggerResult(
            new BackupJob("j1", BackupJobStatus.QUEUED, BackupTrigger.API, null, null, null, null, null),
            false));

        mockMvc.perform(asItAdmin(post("/api/admin/backup/trigger")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value("j1"))
            .andExpect(jsonPath("$.alreadyRunning").value(false));
    }

    @Test
    void triggerAlreadyRunningReturnsExistingJobIdAndTrue() throws Exception {
        when(client.triggerBackup()).thenReturn(new BackupTriggerResult(
            new BackupJob("j1", BackupJobStatus.RUNNING, BackupTrigger.API,
                Instant.parse("2026-06-01T10:00:00Z"), null, null, null, null),
            true));

        mockMvc.perform(asItAdmin(post("/api/admin/backup/trigger")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobId").value("j1"))
            .andExpect(jsonPath("$.alreadyRunning").value(true));
    }

    @Test
    void triggerReturns503OnDbBackupException() throws Exception {
        when(client.triggerBackup()).thenThrow(new DbBackupException("upstream down"));

        mockMvc.perform(asItAdmin(post("/api/admin/backup/trigger")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("Backup-Service nicht verfügbar"));
    }

    // -- /backups --

    @Test
    void listReturnsMappedBackupsInOrder() throws Exception {
        when(client.listBackups()).thenReturn(List.of(
            new BackupMetadata("b1", Instant.parse("2026-05-30T08:15:00Z"),
                12345L, "sha-b1", "17.2", 9000L, BackupTrigger.API),
            new BackupMetadata("b2", Instant.parse("2026-05-29T08:15:00Z"),
                11000L, "sha-b2", "17.2", 8500L, BackupTrigger.SCHEDULER)));

        mockMvc.perform(asItAdmin(get("/api/admin/backup/backups")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("b1"))
            .andExpect(jsonPath("$[0].sizeBytes").value(12345))
            .andExpect(jsonPath("$[0].sha256").value("sha-b1"))
            .andExpect(jsonPath("$[0].pgVersion").value("17.2"))
            .andExpect(jsonPath("$[0].durationMs").value(9000))
            .andExpect(jsonPath("$[0].triggeredBy").value("api"))
            .andExpect(jsonPath("$[1].id").value("b2"))
            .andExpect(jsonPath("$[1].triggeredBy").value("scheduler"));
    }

    @Test
    void listReturnsEmptyArrayWhenNoBackups() throws Exception {
        when(client.listBackups()).thenReturn(List.of());

        mockMvc.perform(asItAdmin(get("/api/admin/backup/backups")))
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));
    }

    @Test
    void listReturns503OnDbBackupException() throws Exception {
        when(client.listBackups()).thenThrow(new DbBackupException("upstream down"));

        mockMvc.perform(asItAdmin(get("/api/admin/backup/backups")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("Backup-Service nicht verfügbar"));
    }

    // -- /backups/{id}/download --

    @Test
    void downloadHappyPathStreamsBytesAndSetsHeaders() throws Exception {
        final byte[] payload = new byte[]{1, 2, 3, 4, 5};
        when(client.listBackups()).thenReturn(List.of(
            new BackupMetadata("abc", Instant.parse("2026-05-30T08:15:00Z"),
                payload.length, "sha-abc", "17.2", 9000L, BackupTrigger.API)));
        when(client.downloadBackup("abc")).thenReturn(
            newDownload("abc", payload.length, payload));

        mockMvc.perform(asItAdmin(get("/api/admin/backup/backups/abc/download")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/gzip"))
            .andExpect(header().longValue("Content-Length", payload.length))
            .andExpect(header().string("Content-Disposition",
                "form-data; name=\"attachment\"; filename=\"backup-2026-05-30T08-15-00Z-abc.sql.gz\""))
            .andExpect(content().bytes(payload));
    }

    @Test
    void downloadFallsBackToIdWhenMetadataLookupFails() throws Exception {
        final byte[] payload = new byte[]{9};
        when(client.listBackups()).thenThrow(new DbBackupException("list down"));
        when(client.downloadBackup("abc")).thenReturn(
            newDownload("abc", payload.length, payload));

        mockMvc.perform(asItAdmin(get("/api/admin/backup/backups/abc/download")))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                "form-data; name=\"attachment\"; filename=\"backup-abc.sql.gz\""))
            .andExpect(content().bytes(payload));
    }

    @Test
    void downloadReturns503WhenUpstreamThrowsBeforeFirstByte() throws Exception {
        when(client.listBackups()).thenReturn(List.of());
        when(client.downloadBackup(eq("missing"))).thenThrow(new DbBackupException("not found"));

        mockMvc.perform(asItAdmin(get("/api/admin/backup/backups/missing/download")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("Backup-Service nicht verfügbar"));
    }

    @Test
    void downloadCallsListAndDownloadExactlyOnceEach() throws Exception {
        final byte[] payload = new byte[]{42};
        when(client.listBackups()).thenReturn(List.of(
            new BackupMetadata("abc", Instant.parse("2026-05-30T08:15:00Z"),
                payload.length, "sha-abc", "17.2", 9000L, BackupTrigger.API)));
        when(client.downloadBackup("abc")).thenReturn(
            newDownload("abc", payload.length, payload));

        mockMvc.perform(asItAdmin(get("/api/admin/backup/backups/abc/download")))
            .andExpect(status().isOk());

        verify(client, times(1)).listBackups();
        verify(client, times(1)).downloadBackup("abc");
    }
}
