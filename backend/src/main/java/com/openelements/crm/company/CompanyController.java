package com.openelements.crm.company;

import com.openelements.crm.contact.ContactService;
import com.openelements.spring.base.data.image.ImageData;
import com.openelements.spring.base.security.roles.RequiresAppAdmin;
import com.openelements.spring.base.services.comment.CommentCreateDto;
import com.openelements.spring.base.services.comment.CommentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * REST controller for company management.
 */
@RestController
@RequestMapping("/api/companies")
@Tag(name = "Companies", description = "Company management operations")
@SecurityRequirement(name = "oidc")
public class CompanyController {

    private final CompanyService companyService;
    private final ContactService contactService;

    public CompanyController(final CompanyService companyService, final ContactService contactService) {
        this.companyService = Objects.requireNonNull(companyService, "companyService must not be null");
        this.contactService = Objects.requireNonNull(contactService, "contactService must not be null");
    }

    /**
     * Lists companies with pagination, filtering, and sorting.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List companies", description = "Returns a paginated list of companies with optional filtering")
    public Page<CompanyDto> list(
        @Parameter(description = "Partial company name filter (case-insensitive contains)")
        @RequestParam(required = false) final String name,
        @Parameter(description = "Filter by Brevo origin: true = only Brevo, false = only non-Brevo, omit = all")
        @RequestParam(required = false) final Boolean brevo,
        @Parameter(description = "Filter by tag IDs (AND semantics)")
        @RequestParam(required = false) final List<UUID> tagIds,
        @Parameter(hidden = true)
        @PageableDefault(size = 20, sort = "name") final Pageable pageable) {
        return companyService.list(name, brevo, tagIds, pageable);
    }

    /**
     * Exports companies as CSV with selected columns.
     */
    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export companies as CSV")
    @ApiResponse(responseCode = "200", description = "CSV file downloaded")
    public void exportCsv(
        @Parameter(description = "Partial company name filter") @RequestParam(required = false) final String name,
        @Parameter(description = "Filter by Brevo origin") @RequestParam(required = false) final Boolean brevo,
        @Parameter(description = "Filter by tag IDs") @RequestParam(required = false) final List<UUID> tagIds,
        @Parameter(description = "Columns to include in the CSV") @RequestParam final List<CompanyExportColumn> columns,
        final HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"companies.csv\"");

        final List<CompanyDto> companies = companyService.listAll(name, brevo, tagIds);
        final String[] headers = columns.stream().map(CompanyExportColumn::getHeader).toArray(String[]::new);

        final var writer = response.getWriter();
        writer.write('\uFEFF'); // UTF-8 BOM
        try (final CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build())) {
            for (final CompanyDto company : companies) {
                final Object[] values = columns.stream().map(col -> col.extract(company)).toArray();
                printer.printRecord(values);
            }
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get company by ID")
    @ApiResponse(responseCode = "200", description = "Company found")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public CompanyDto getById(@Parameter(description = "The company ID") @PathVariable final UUID id) {
        return companyService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new company")
    @ApiResponse(responseCode = "201", description = "Company created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public CompanyDto create(@Valid @RequestBody final CompanyDataDto request) {
        final CompanyDto dto = new CompanyDto(null,
            request.name(),
            request.description(),
            request.website(),
            request.street(),
            request.houseNumber(),
            request.zipCode(),
            request.city(),
            request.country(),
            request.phoneNumber(),
            request.description(),
            request.bankName(),
            request.bic(),
            request.iban(),
            request.vatId(),
            false,
            false,
            0,
            0,
            request.tagIds(),
            null,
            null);
        return companyService.save(dto);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a company")
    @ApiResponse(responseCode = "200", description = "Company updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public CompanyDto update(@Parameter(description = "The company ID") @PathVariable final UUID id,
                             @Valid @RequestBody final CompanyDataDto request) {
        final CompanyDto current = companyService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        final CompanyDto dto = new CompanyDto(id,
            request.name(),
            request.email(),
            request.website(),
            request.street(),
            request.houseNumber(),
            request.zipCode(),
            request.city(),
            request.country(),
            request.phoneNumber(),
            request.description(),
            request.bankName(),
            request.bic(),
            request.iban(),
            request.vatId(),
            current.hasLogo(),
            current.brevo(),
            current.contactCount(),
            current.commentCount(),
            request.tagIds(),
            current.createdAt(),
            current.updatedAt());
        return companyService.save(dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresAppAdmin
    @Operation(summary = "Delete a company")
    @ApiResponse(responseCode = "204", description = "Company deleted")
    @ApiResponse(responseCode = "403", description = "Missing ADMIN role")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public void delete(@Parameter(description = "The company ID") @PathVariable final UUID id,
                       @Parameter(description = "Whether to also delete all associated contacts")
                       @RequestParam(defaultValue = "false") final boolean deleteContacts) {
        if (deleteContacts) {
            contactService.getForCompany(id).forEach(contact -> contactService.delete(contact.id()));
        }
        companyService.delete(id);
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload company logo")
    @ApiResponse(responseCode = "200", description = "Logo uploaded")
    @ApiResponse(responseCode = "400", description = "Invalid file format or size")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public void uploadLogo(@Parameter(description = "The company ID") @PathVariable final UUID id,
                           @Parameter(description = "The logo image file") @RequestParam("file") final MultipartFile file) {
        try {
            companyService.updateLogo(id, ImageData.of(file));
        } catch (final java.io.IOException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Failed to read file");
        }
    }

    @GetMapping(value = "/{id}/logo")
    @Operation(summary = "Get company logo")
    @ApiResponse(responseCode = "200", description = "Logo found")
    @ApiResponse(responseCode = "404", description = "Company or logo not found")
    public ResponseEntity<byte[]> getLogo(@Parameter(description = "The company ID") @PathVariable final UUID id) {
        return companyService.getLogo(id)
            .map(i -> i.toHttpResponse())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    @DeleteMapping("/{id}/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresAppAdmin
    @Operation(summary = "Remove company logo")
    @ApiResponse(responseCode = "204", description = "Logo removed")
    @ApiResponse(responseCode = "403", description = "Missing ADMIN role")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public void deleteLogo(@Parameter(description = "The company ID") @PathVariable final UUID id) {
        companyService.deleteLogo(id);
    }

    /**
     * Lists comments attached to a company. Returns a flat array sorted by createdAt descending.
     */
    @GetMapping(value = "/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List comments for a company")
    @ApiResponse(responseCode = "200", description = "Comments found")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public List<CommentDto> listComments(
        @Parameter(description = "The company ID") @PathVariable final UUID id) {
        return companyService.listCommentsOfCompany(id);
    }

    /**
     * Adds a comment to a company.
     */
    @PostMapping(value = "/{id}/comments", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment to a company")
    @ApiResponse(responseCode = "201", description = "Comment created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Company not found")
    public CommentDto addComment(@Parameter(description = "The company ID") @PathVariable final UUID id,
                                 @Valid @RequestBody final CommentCreateDto request) {
        return companyService.addCommentToCompany(id, request);
    }

    /**
     * Updates a comment attached to a company.
     */
    @PutMapping(value = "/{id}/comments/{commentId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a comment of a company")
    @ApiResponse(responseCode = "200", description = "Comment updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Company or comment not found, or mismatched owner")
    public CommentDto updateComment(@Parameter(description = "The company ID") @PathVariable final UUID id,
                                    @Parameter(description = "The comment ID") @PathVariable final UUID commentId,
                                    @Valid @RequestBody final CommentCreateDto request) {
        return companyService.updateCommentOfCompany(id, commentId, request);
    }

    /**
     * Deletes a comment attached to a company.
     */
    @DeleteMapping("/{id}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequiresAppAdmin
    @Operation(summary = "Delete a comment of a company")
    @ApiResponse(responseCode = "204", description = "Comment deleted")
    @ApiResponse(responseCode = "403", description = "Missing ADMIN role")
    @ApiResponse(responseCode = "404", description = "Company or comment not found, or mismatched owner")
    public void deleteComment(@Parameter(description = "The company ID") @PathVariable final UUID id,
                              @Parameter(description = "The comment ID") @PathVariable final UUID commentId) {
        companyService.deleteCommentOfCompany(id, commentId);
    }
}
