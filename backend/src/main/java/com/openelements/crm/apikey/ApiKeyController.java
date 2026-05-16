package com.openelements.crm.apikey;

import com.openelements.spring.base.security.NeedsItAdminRole;
import com.openelements.spring.base.services.apikey.ApiKeyCreateDto;
import com.openelements.spring.base.services.apikey.ApiKeyCreatedDto;
import com.openelements.spring.base.services.apikey.ApiKeyDataService;
import com.openelements.spring.base.services.apikey.ApiKeyDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * REST controller for API key management.
 */
@RestController
@RequestMapping("/api/api-keys")
@Tag(name = "API Keys", description = "API key management operations")
@SecurityRequirement(name = "oidc")
@NeedsItAdminRole
public class ApiKeyController {

    private final ApiKeyDataService apiKeyService;

    public ApiKeyController(final ApiKeyDataService apiKeyService) {
        this.apiKeyService = Objects.requireNonNull(apiKeyService, "apiKeyService must not be null");
    }

    /**
     * Creates a new API key. The raw key is returned only in this response.
     *
     * @param request the create request with a name
     * @return the created key including the raw key (shown once)
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new API key", description = "The raw key is returned only in this response and cannot be retrieved later")
    @ApiResponse(responseCode = "201", description = "API key created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ApiKeyCreatedDto create(@Valid @RequestBody final ApiKeyCreateDto request) {
        return apiKeyService.create(request);
    }

    /**
     * Lists API keys with pagination.
     *
     * @param pageable pagination parameters
     * @return a page of API key DTOs (without raw keys)
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List API keys", description = "Returns a paginated list of API keys. Raw keys are never returned.")
    public Page<ApiKeyDto> list(
        @Parameter(hidden = true)
        @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) final Pageable pageable) {
        return apiKeyService.list(pageable);
    }

    /**
     * Deletes an API key.
     *
     * @param id the API key ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an API key")
    @ApiResponse(responseCode = "204", description = "API key deleted")
    @ApiResponse(responseCode = "404", description = "API key not found")
    public void delete(@Parameter(description = "The API key ID") @PathVariable final UUID id) {
        apiKeyService.delete(id);
    }
}
