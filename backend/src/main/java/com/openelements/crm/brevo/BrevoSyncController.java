package com.openelements.crm.brevo;

import com.openelements.spring.base.security.NeedsItAdminRole;
import com.openelements.spring.base.services.settings.SettingsDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * REST controller for Brevo import settings and synchronization.
 */
@RestController
@RequestMapping("/api/brevo")
@Tag(name = "Brevo Sync", description = "Brevo import and settings management")
@SecurityRequirement(name = "oidc")
@NeedsItAdminRole
public class BrevoSyncController {

    private static final String BREVO_API_KEY = "brevo.api-key";

    private final SettingsDataService settingsService;
    private final BrevoApiClient brevoApiClient;
    private final BrevoSyncService brevoSyncService;

    /**
     * Creates a new BrevoSyncController.
     *
     * @param settingsService  the settings service
     * @param brevoApiClient   the Brevo API client
     * @param brevoSyncService the Brevo sync service
     */
    public BrevoSyncController(final SettingsDataService settingsService,
                               final BrevoApiClient brevoApiClient,
                               final BrevoSyncService brevoSyncService) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService must not be null");
        this.brevoApiClient = Objects.requireNonNull(brevoApiClient, "brevoApiClient must not be null");
        this.brevoSyncService = Objects.requireNonNull(brevoSyncService, "brevoSyncService must not be null");
    }

    /**
     * Returns whether a Brevo API key is currently configured.
     *
     * @return the settings status
     */
    @GetMapping(value = "/settings", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Brevo settings status")
    @ApiResponse(responseCode = "200", description = "Settings status returned")
    public BrevoSettingsDto getSettings() {
        final boolean configured = settingsService.get(BREVO_API_KEY).isPresent();
        return new BrevoSettingsDto(configured);
    }

    /**
     * Updates the Brevo API key after validating it against the Brevo API.
     *
     * @param request the update request containing the new API key
     * @return the updated settings status
     */
    @PutMapping(value = "/settings", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update Brevo API key")
    @ApiResponse(responseCode = "200", description = "API key updated")
    @ApiResponse(responseCode = "400", description = "Invalid API key")
    public BrevoSettingsDto updateSettings(@Valid @RequestBody final BrevoSettingsUpdateDto request) {
        brevoApiClient.validateApiKey(request.apiKey());
        settingsService.set(BREVO_API_KEY, request.apiKey());
        return new BrevoSettingsDto(true);
    }

    /**
     * Removes the stored Brevo API key.
     */
    @DeleteMapping("/settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove Brevo API key")
    @ApiResponse(responseCode = "204", description = "API key removed")
    public void deleteSettings() {
        settingsService.delete(BREVO_API_KEY);
    }

    /**
     * Triggers a full synchronization of companies and contacts from Brevo.
     *
     * @return the sync result summary
     */
    @PostMapping(value = "/sync", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Trigger Brevo import sync")
    @ApiResponse(responseCode = "200", description = "Sync completed")
    @ApiResponse(responseCode = "400", description = "API key not configured")
    @ApiResponse(responseCode = "409", description = "Sync already in progress")
    public BrevoSyncResultDto sync() {
        if (settingsService.get(BREVO_API_KEY).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Brevo API key is not configured");
        }
        return brevoSyncService.syncAll();
    }
}
