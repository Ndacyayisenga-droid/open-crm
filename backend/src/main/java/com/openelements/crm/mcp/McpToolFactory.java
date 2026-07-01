package com.openelements.crm.mcp;

import static com.openelements.spring.base.mcp.McpTools.bool;
import static com.openelements.spring.base.mcp.McpTools.integer;
import static com.openelements.spring.base.mcp.McpTools.paginationProps;
import static com.openelements.spring.base.mcp.McpTools.prop;
import static com.openelements.spring.base.mcp.McpTools.requiredString;
import static com.openelements.spring.base.mcp.McpTools.requiredUuid;
import static com.openelements.spring.base.mcp.McpTools.string;
import static com.openelements.spring.base.mcp.McpTools.tool;
import static com.openelements.spring.base.mcp.McpTools.uuid;
import static com.openelements.spring.base.mcp.McpTools.uuidArray;
import static com.openelements.spring.base.mcp.McpTools.uuidList;
import static com.openelements.spring.base.mcp.McpTools.uuidProp;

import com.openelements.crm.company.CompanyService;
import com.openelements.crm.contact.ContactService;
import com.openelements.crm.search.CrmSearchService;
import com.openelements.spring.base.data.image.ImageData;
import com.openelements.spring.base.mcp.McpPage;
import com.openelements.spring.base.mcp.McpPaging;
import com.openelements.spring.base.mcp.McpToolProvider;
import com.openelements.spring.base.mcp.McpToolSupport;
import com.openelements.spring.base.mcp.McpUnavailableException;
import com.openelements.spring.base.services.tag.TagDataService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Builds the read-only CRM MCP tool catalog (spec 108, Phase 1) over the existing
 * CRM services. Each tool validates and parses its arguments (via
 * {@link com.openelements.spring.base.mcp.McpTools}), calls a backing service,
 * wraps collections in the {@link McpPage} envelope, and is handed to
 * {@link McpToolSupport#spec} which serializes the payload, logs one access line,
 * and maps failures to JSON-RPC error results.
 *
 * <p>This class holds the CRM-specific half of the MCP feature: which tools exist
 * and which services back them. The domain-agnostic plumbing (schema/argument
 * helpers, dispatch, pagination, server and security wiring) lives in
 * {@code com.openelements.spring.base.mcp} and is reusable across applications.
 *
 * <p>MCP reads are <em>not</em> written to the {@code audit_log}: that table
 * records mutations, and read access (like viewing a record in the frontend) is
 * not audited. See {@code docs/TODO.md} for a possible future read-access audit.
 */
@Component
public class McpToolFactory implements McpToolProvider {

    private static final String PAGINATION_HINT =
        " Results are paginated; when the response field hasMore is true, fetch the next page by increasing page.";

    private final ContactService contactService;
    private final CompanyService companyService;
    private final TagDataService tagService;
    private final CrmSearchService searchService;
    private final McpPaging paging;
    private final McpToolSupport support;

    public McpToolFactory(final ContactService contactService,
                          final CompanyService companyService,
                          final TagDataService tagService,
                          final CrmSearchService searchService,
                          final McpPaging paging,
                          final McpToolSupport support) {
        this.contactService = Objects.requireNonNull(contactService);
        this.companyService = Objects.requireNonNull(companyService);
        this.tagService = Objects.requireNonNull(tagService);
        this.searchService = Objects.requireNonNull(searchService);
        this.paging = Objects.requireNonNull(paging);
        this.support = Objects.requireNonNull(support);
    }

    /**
     * @return the full Phase 1 tool catalog as MCP sync tool specifications
     */
    @Override
    public List<SyncToolSpecification> toolSpecifications() {
        return List.of(
            searchTool(),
            listCompaniesTool(),
            getCompanyTool(),
            listContactsTool(),
            getContactTool(),
            listTagsTool(),
            getTagTool(),
            listCompanyCommentsTool(),
            listContactCommentsTool(),
            getContactPhotoTool(),
            getCompanyLogoTool()
        );
    }

    // -- Tools ---------------------------------------------------------------

    private SyncToolSpecification searchTool() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("q", prop("string", "Search query (min 2 characters)."));
        props.put("limit", prop("integer", "Max hits per section (default 5, max 20)."));
        final McpSchema.Tool tool = tool("search",
            "Global typo-tolerant search across companies, contacts, tags, and comments. "
                + "Returns grouped hits with id, label, snippet, score, and (for comments) owner reference.",
            props, List.of("q"));
        return support.spec(tool, args -> {
            if (searchService.isBootstrapping()) {
                throw new McpUnavailableException("Search index is initializing; retry shortly.");
            }
            final String q = requiredString(args, "q");
            final Integer limit = integer(args, "limit");
            return searchService.search(q, limit == null ? 0 : limit);
        });
    }

    private SyncToolSpecification listCompaniesTool() {
        final Map<String, Object> props = paginationProps();
        props.put("name", prop("string", "Filter by company name (case-insensitive contains)."));
        props.put("brevo", prop("boolean", "Filter by Brevo origin: true = only Brevo, false = only non-Brevo."));
        props.put("tagIds", uuidArray("Filter by tag IDs (all must be present)."));
        final McpSchema.Tool tool = tool("list_companies",
            "List companies with optional name/brevo/tag filters." + PAGINATION_HINT, props, List.of());
        return support.spec(tool, args -> McpPage.from(companyService.list(
            string(args, "name"), bool(args, "brevo"), uuidList(args, "tagIds"),
            paging.toPageable(integer(args, "page"), integer(args, "size"), Sort.by("name")))));
    }

    private SyncToolSpecification getCompanyTool() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", uuidProp("The company ID."));
        final McpSchema.Tool tool = tool("get_company", "Get a single company by ID.", props, List.of("id"));
        return support.spec(tool, args -> {
            final UUID id = requiredUuid(args, "id");
            return companyService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Company not found: " + id));
        });
    }

    private SyncToolSpecification listContactsTool() {
        final Map<String, Object> props = paginationProps();
        props.put("search", prop("string", "Multi-word search across name, email, and company name."));
        props.put("companyId", uuidProp("Filter by company ID."));
        props.put("language", prop("string", "Filter by language code (DE, EN, or UNKNOWN for none)."));
        props.put("brevo", prop("boolean", "Filter by Brevo origin: true = only Brevo, false = only non-Brevo."));
        props.put("tagIds", uuidArray("Filter by tag IDs (all must be present)."));
        final McpSchema.Tool tool = tool("list_contacts",
            "List contacts with optional search and company/language/brevo/tag filters." + PAGINATION_HINT,
            props, List.of());
        return support.spec(tool, args -> McpPage.from(contactService.list(
            string(args, "search"), uuid(args, "companyId"), false, string(args, "language"),
            bool(args, "brevo"), uuidList(args, "tagIds"),
            paging.toPageable(integer(args, "page"), integer(args, "size"), Sort.by("lastName", "firstName")))));
    }

    private SyncToolSpecification getContactTool() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", uuidProp("The contact ID."));
        final McpSchema.Tool tool = tool("get_contact", "Get a single contact by ID.", props, List.of("id"));
        return support.spec(tool, args -> {
            final UUID id = requiredUuid(args, "id");
            return contactService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contact not found: " + id));
        });
    }

    private SyncToolSpecification listTagsTool() {
        final McpSchema.Tool tool = tool("list_tags",
            "List tags used to categorize companies and contacts." + PAGINATION_HINT,
            paginationProps(), List.of());
        return support.spec(tool, args -> McpPage.from(tagService.findAll(
            paging.toPageable(integer(args, "page"), integer(args, "size"), Sort.by("name")))));
    }

    private SyncToolSpecification getTagTool() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", uuidProp("The tag ID."));
        final McpSchema.Tool tool = tool("get_tag", "Get a single tag by ID.", props, List.of("id"));
        return support.spec(tool, args -> {
            final UUID id = requiredUuid(args, "id");
            return tagService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tag not found: " + id));
        });
    }

    private SyncToolSpecification listCompanyCommentsTool() {
        final Map<String, Object> props = paginationProps();
        props.put("companyId", uuidProp("The company ID."));
        final McpSchema.Tool tool = tool("list_company_comments",
            "List the comments (full text) attached to a company." + PAGINATION_HINT,
            props, List.of("companyId"));
        return support.spec(tool, args -> support.paginate(
            companyService.listCommentsOfCompany(requiredUuid(args, "companyId")),
            integer(args, "page"), integer(args, "size")));
    }

    private SyncToolSpecification listContactCommentsTool() {
        final Map<String, Object> props = paginationProps();
        props.put("contactId", uuidProp("The contact ID."));
        final McpSchema.Tool tool = tool("list_contact_comments",
            "List the comments (full text) attached to a contact." + PAGINATION_HINT,
            props, List.of("contactId"));
        return support.spec(tool, args -> support.paginate(
            contactService.listCommentsOfContact(requiredUuid(args, "contactId")),
            integer(args, "page"), integer(args, "size")));
    }

    private SyncToolSpecification getContactPhotoTool() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", uuidProp("The contact ID."));
        final McpSchema.Tool tool = tool("get_contact_photo",
            "Return the contact's photo as an image. Errors if the contact does not exist or has no photo.",
            props, List.of("id"));
        return support.imageSpec(tool, args -> {
            final UUID id = requiredUuid(args, "id");
            final Optional<ImageData> photo;
            try {
                photo = contactService.getPhoto(id);
            } catch (final ResponseStatusException e) {           // 404: contact missing
                throw new NoSuchElementException("Contact not found: " + id);
            }
            return photo.orElseThrow(() -> new NoSuchElementException("Contact has no photo: " + id));
        });
    }

    private SyncToolSpecification getCompanyLogoTool() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", uuidProp("The company ID."));
        final McpSchema.Tool tool = tool("get_company_logo",
            "Return the company's logo as an image. Errors if the company does not exist or has no logo.",
            props, List.of("id"));
        return support.imageSpec(tool, args -> {
            final UUID id = requiredUuid(args, "id");
            final Optional<ImageData> logo;
            try {
                logo = companyService.getLogo(id);
            } catch (final ResponseStatusException e) {           // 404: company missing
                throw new NoSuchElementException("Company not found: " + id);
            }
            return logo.orElseThrow(() -> new NoSuchElementException("Company has no logo: " + id));
        });
    }
}
