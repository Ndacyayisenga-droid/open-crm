package com.openelements.crm.contact.csvimport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

/**
 * REST endpoints for bulk CSV contact import.
 */
@RestController
@RequestMapping("/api/contacts/import")
@PreAuthorize("hasRole('APP-ADMIN') or hasRole('IT-ADMIN')")
@Tag(name = "Contact Import", description = "Bulk CSV import for contacts")
@SecurityRequirement(name = "oidc")
public class ContactImportController {

    private final ContactImportService importService;

    public ContactImportController(final ContactImportService importService) {
        this.importService = Objects.requireNonNull(importService, "importService must not be null");
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Preview CSV import", description = "Parses a CSV file and optionally previews mapped contact rows")
    @ApiResponse(responseCode = "200", description = "Preview generated")
    @ApiResponse(responseCode = "400", description = "Invalid file or mapping")
    @ApiResponse(responseCode = "403", description = "Missing APP-ADMIN or IT-ADMIN role")
    @ApiResponse(responseCode = "413", description = "File or row limit exceeded")
    @ApiResponse(responseCode = "415", description = "Unsupported encoding")
    public ContactImportPreviewResponse preview(
        @RequestPart("file") final MultipartFile file,
        @RequestPart("request") final ContactImportRequest request) {
        return importService.preview(file, request);
    }

    @PostMapping(value = "/commit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Commit CSV import", description = "Imports contacts from a CSV file with partial success")
    @ApiResponse(responseCode = "200", description = "Import completed")
    @ApiResponse(responseCode = "400", description = "Invalid file or mapping")
    @ApiResponse(responseCode = "403", description = "Missing APP-ADMIN or IT-ADMIN role")
    @ApiResponse(responseCode = "413", description = "File or row limit exceeded")
    @ApiResponse(responseCode = "415", description = "Unsupported encoding")
    public ImportResult commit(
        @RequestPart("file") final MultipartFile file,
        @RequestPart("request") final ContactImportRequest request) {
        return importService.commit(file, request);
    }

    @RestControllerAdvice(assignableTypes = ContactImportController.class)
    static class ContactImportExceptionHandler {

        @ExceptionHandler(ResponseStatusException.class)
        ResponseEntity<ImportErrorResponse> handleResponseStatus(final ResponseStatusException ex) {
            final String reason = ex.getReason() == null ? ex.getStatusCode().toString() : ex.getReason();
            final int separator = reason.indexOf(':');
            if (separator > 0) {
                return ResponseEntity.status(ex.getStatusCode())
                    .body(new ImportErrorResponse(reason.substring(0, separator).trim(), reason.substring(separator + 1).trim()));
            }
            return ResponseEntity.status(ex.getStatusCode())
                .body(new ImportErrorResponse(reason, reason));
        }
    }
}
