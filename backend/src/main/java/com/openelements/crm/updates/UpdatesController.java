package com.openelements.crm.updates;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller exposing the user-facing "Updates" activity feed. Available to every
 * authenticated user — no admin role required.
 */
@RestController
@RequestMapping("/api/updates")
@Tag(name = "Updates", description = "User-facing activity feed of recent changes")
@SecurityRequirement(name = "oidc")
public class UpdatesController {

    static final int DEFAULT_SIZE = 20;
    static final Set<Integer> ALLOWED_SIZES = Set.of(20, 50, 100, 200);

    private final UpdatesService updatesService;

    public UpdatesController(final UpdatesService updatesService) {
        this.updatesService = Objects.requireNonNull(updatesService, "updatesService must not be null");
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "List recent updates",
        description = "Returns up to `size` most recent activity entries across companies, contacts and their "
            + "comments. Sorted by createdAt descending. Available to every authenticated user."
    )
    @ApiResponse(responseCode = "200", description = "Update entries returned")
    @ApiResponse(responseCode = "400", description = "Invalid size value")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public Page<UpdateEntryDto> list(
        @Parameter(description = "Maximum number of entries to return. Allowed: 20, 50, 100, 200")
        @RequestParam(required = false, defaultValue = "20") final int size,
        @Parameter(description = "Page index. Only 0 returns content; > 0 returns an empty content page.")
        @RequestParam(required = false, defaultValue = "0") final int page) {
        if (!ALLOWED_SIZES.contains(size)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "size must be one of " + ALLOWED_SIZES);
        }
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
        }
        final PageRequest pageRequest = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (page > 0) {
            return new PageImpl<>(List.of(), pageRequest, 0);
        }
        final List<UpdateEntryDto> content = updatesService.load(size);
        return new PageImpl<>(content, pageRequest, content.size());
    }
}
