package com.openelements.crm.contact;

import com.openelements.spring.base.security.NeedsAppAdminRole;
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
 * REST controller for contact management.
 */
@RestController
@RequestMapping("/api/contacts")
@Tag(name = "Contacts", description = "Contact management operations")
@SecurityRequirement(name = "oidc")
public class ContactController {

    private final ContactService contactService;

    public ContactController(final ContactService contactService) {
        this.contactService = Objects.requireNonNull(contactService, "contactService must not be null");
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List contacts", description = "Returns a paginated list of contacts with optional filtering")
    public Page<ContactDto> list(
        @Parameter(description = "Multi-word search across name, email, and company (case-insensitive contains)")
        @RequestParam(required = false) final String search,
        @Parameter(description = "Filter by company ID (exact match)")
        @RequestParam(required = false) final UUID companyId,
        @Parameter(description = "Filter by language code (DE, EN, or UNKNOWN for null)")
        @RequestParam(required = false) final String language,
        @Parameter(description = "Filter for contacts without a company association")
        @RequestParam(defaultValue = "false") final boolean noCompany,
        @Parameter(description = "Filter by Brevo origin: true = only Brevo, false = only non-Brevo, omit = all")
        @RequestParam(required = false) final Boolean brevo,
        @Parameter(description = "Filter by tag IDs (AND semantics)")
        @RequestParam(required = false) final List<UUID> tagIds,
        @Parameter(hidden = true)
        @PageableDefault(size = 20, sort = "lastName") final Pageable pageable) {
        return contactService.list(search, companyId, noCompany, language, brevo, tagIds, pageable);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export contacts as CSV")
    @ApiResponse(responseCode = "200", description = "CSV file downloaded")
    public void exportCsv(
        @Parameter(description = "Multi-word search filter") @RequestParam(required = false) final String search,
        @Parameter(description = "Filter by company ID") @RequestParam(required = false) final UUID companyId,
        @Parameter(description = "Filter by language code") @RequestParam(required = false) final String language,
        @Parameter(description = "Filter for contacts without a company") @RequestParam(defaultValue = "false") final boolean noCompany,
        @Parameter(description = "Filter by Brevo origin") @RequestParam(required = false) final Boolean brevo,
        @Parameter(description = "Filter by tag IDs") @RequestParam(required = false) final List<UUID> tagIds,
        @Parameter(description = "Columns to include in the CSV") @RequestParam final List<ContactExportColumn> columns,
        final HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"contacts.csv\"");

        final List<ContactDto> contacts = contactService.listAll(search, companyId, noCompany, language, brevo, tagIds);
        final String[] headers = columns.stream().map(ContactExportColumn::getHeader).toArray(String[]::new);

        final var writer = response.getWriter();
        writer.write('\uFEFF'); // UTF-8 BOM
        try (final CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(headers).build())) {
            for (final ContactDto contact : contacts) {
                final Object[] values = columns.stream().map(col -> col.extract(contact)).toArray();
                printer.printRecord(values);
            }
        }
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get contact by ID")
    @ApiResponse(responseCode = "200", description = "Contact found")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public ContactDto getById(@Parameter(description = "The contact ID") @PathVariable final UUID id) {
        return contactService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new contact")
    @ApiResponse(responseCode = "201", description = "Contact created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ContactDto create(@Valid @RequestBody final ContactCreateDto request) {
        final ContactDto dto = new ContactDto(
            null,
            request.title(),
            request.firstName(),
            request.lastName(),
            request.email(),
            request.position(),
            request.gender(),
            request.socialLinks().stream().map(SocialLinkDto::fromCreateDto).toList(),
            request.phoneNumber(),
            request.description(),
            request.companyId(),
            null,
            0,
            false,
            request.birthday(),
            false,
            false,
            request.language(),
            request.tagIds(),
            null,
            null
        );
        return contactService.save(dto);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a contact")
    @ApiResponse(responseCode = "200", description = "Contact updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public ContactDto update(@Parameter(description = "The contact ID") @PathVariable final UUID id,
                             @Valid @RequestBody final ContactUpdateDto request) {
        final ContactDto dto = new ContactDto(
            id,
            request.title(),
            request.firstName(),
            request.lastName(),
            request.email(),
            request.position(),
            request.gender(),
            request.socialLinks().stream().map(SocialLinkDto::fromCreateDto).toList(),
            request.phoneNumber(),
            request.description(),
            request.companyId(),
            null,
            0,
            false,
            request.birthday(),
            false,
            false,
            request.language(),
            request.tagIds(),
            null,
            null
        );
        return contactService.save(dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @NeedsAppAdminRole
    @Operation(summary = "Delete a contact", description = "Permanently deletes the contact and all associated comments")
    @ApiResponse(responseCode = "204", description = "Contact deleted")
    @ApiResponse(responseCode = "403", description = "Missing ADMIN role")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public void delete(@Parameter(description = "The contact ID") @PathVariable final UUID id) {
        contactService.delete(id);
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload contact photo")
    @ApiResponse(responseCode = "200", description = "Photo uploaded")
    @ApiResponse(responseCode = "400", description = "Invalid file format or size")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public void uploadPhoto(@Parameter(description = "The contact ID") @PathVariable final UUID id,
                            @Parameter(description = "The photo image file (JPEG only; max 2 MB)") @RequestParam("file") final MultipartFile file) {
        try {
            contactService.uploadPhoto(id, file.getBytes(), file.getContentType());
        } catch (final java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read file");
        }
    }

    @GetMapping(value = "/{id}/photo")
    @Operation(summary = "Get contact photo")
    @ApiResponse(responseCode = "200", description = "Photo found")
    @ApiResponse(responseCode = "404", description = "Contact or photo not found")
    public ResponseEntity<byte[]> getPhoto(@Parameter(description = "The contact ID") @PathVariable final UUID id) {
        return contactService.getPhoto(id)
            .map(i -> i.toHttpResponse())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @NeedsAppAdminRole
    @Operation(summary = "Remove contact photo")
    @ApiResponse(responseCode = "204", description = "Photo removed")
    @ApiResponse(responseCode = "403", description = "Missing ADMIN role")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public void deletePhoto(@Parameter(description = "The contact ID") @PathVariable final UUID id) {
        contactService.deletePhoto(id);
    }

    /**
     * Lists comments attached to a contact. Returns a flat array sorted by createdAt descending.
     */
    @GetMapping(value = "/{id}/comments", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List comments for a contact")
    @ApiResponse(responseCode = "200", description = "Comments found")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public List<CommentDto> listComments(
        @Parameter(description = "The contact ID") @PathVariable final UUID id) {
        return contactService.listCommentsOfContact(id);
    }

    @PostMapping(value = "/{id}/comments", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment to a contact")
    @ApiResponse(responseCode = "201", description = "Comment created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Contact not found")
    public CommentDto addComment(@Parameter(description = "The contact ID") @PathVariable final UUID id,
                                 @Valid @RequestBody final CommentCreateDto request) {
        return contactService.addCommentToContact(id, request);
    }

    @PutMapping(value = "/{id}/comments/{commentId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a comment of a contact")
    @ApiResponse(responseCode = "200", description = "Comment updated")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Contact or comment not found, or mismatched owner")
    public CommentDto updateComment(@Parameter(description = "The contact ID") @PathVariable final UUID id,
                                    @Parameter(description = "The comment ID") @PathVariable final UUID commentId,
                                    @Valid @RequestBody final CommentCreateDto request) {
        return contactService.updateCommentOfContact(id, commentId, request);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @NeedsAppAdminRole
    @Operation(summary = "Delete a comment of a contact")
    @ApiResponse(responseCode = "204", description = "Comment deleted")
    @ApiResponse(responseCode = "403", description = "Missing ADMIN role")
    @ApiResponse(responseCode = "404", description = "Contact or comment not found, or mismatched owner")
    public void deleteComment(@Parameter(description = "The contact ID") @PathVariable final UUID id,
                              @Parameter(description = "The comment ID") @PathVariable final UUID commentId) {
        contactService.deleteCommentOfContact(id, commentId);
    }
}
