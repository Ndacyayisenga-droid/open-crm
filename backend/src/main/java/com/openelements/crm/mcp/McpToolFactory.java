package com.openelements.crm.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openelements.crm.company.CompanyService;
import com.openelements.crm.contact.ContactService;
import com.openelements.crm.search.CrmSearchService;
import com.openelements.spring.base.services.tag.TagDataService;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Builds the read-only MCP tool catalog (spec 108, Phase 1) over the existing
 * CRM services. Each tool validates and parses its arguments, calls a backing
 * service, wraps collections in the {@link McpPage} envelope, serializes the
 * payload to JSON, records one audit entry, and maps failures to JSON-RPC error
 * results.
 */
@Component
public class McpToolFactory {

    private static final String PAGINATION_HINT =
        " Results are paginated; when the response field hasMore is true, fetch the next page by increasing page.";

    private final ContactService contactService;
    private final CompanyService companyService;
    private final TagDataService tagService;
    private final CrmSearchService searchService;
    private final McpPaging paging;
    private final McpAuditService audit;
    private final McpActorResolver actorResolver;
    private final ObjectMapper objectMapper;

    public McpToolFactory(final ContactService contactService,
                          final CompanyService companyService,
                          final TagDataService tagService,
                          final CrmSearchService searchService,
                          final McpPaging paging,
                          final McpAuditService audit,
                          final McpActorResolver actorResolver,
                          final ObjectMapper objectMapper) {
        this.contactService = Objects.requireNonNull(contactService);
        this.companyService = Objects.requireNonNull(companyService);
        this.tagService = Objects.requireNonNull(tagService);
        this.searchService = Objects.requireNonNull(searchService);
        this.paging = Objects.requireNonNull(paging);
        this.audit = Objects.requireNonNull(audit);
        this.actorResolver = Objects.requireNonNull(actorResolver);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    /**
     * @return the full Phase 1 tool catalog as MCP sync tool specifications
     */
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
            listContactCommentsTool()
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
        return spec(tool, args -> {
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
        return spec(tool, args -> McpPage.from(companyService.list(
            string(args, "name"), bool(args, "brevo"), uuidList(args, "tagIds"),
            paging.toPageable(integer(args, "page"), integer(args, "size"), Sort.by("name")))));
    }

    private SyncToolSpecification getCompanyTool() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", uuidProp("The company ID."));
        final McpSchema.Tool tool = tool("get_company", "Get a single company by ID.", props, List.of("id"));
        return spec(tool, args -> {
            final UUID id = requiredUuid(args, "id");
            return companyService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Company not found: " + id));
        }, args -> uuid(args, "id"));
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
        return spec(tool, args -> McpPage.from(contactService.list(
            string(args, "search"), uuid(args, "companyId"), false, string(args, "language"),
            bool(args, "brevo"), uuidList(args, "tagIds"),
            paging.toPageable(integer(args, "page"), integer(args, "size"), Sort.by("lastName", "firstName")))));
    }

    private SyncToolSpecification getContactTool() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", uuidProp("The contact ID."));
        final McpSchema.Tool tool = tool("get_contact", "Get a single contact by ID.", props, List.of("id"));
        return spec(tool, args -> {
            final UUID id = requiredUuid(args, "id");
            return contactService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Contact not found: " + id));
        }, args -> uuid(args, "id"));
    }

    private SyncToolSpecification listTagsTool() {
        final McpSchema.Tool tool = tool("list_tags",
            "List tags used to categorize companies and contacts." + PAGINATION_HINT,
            paginationProps(), List.of());
        return spec(tool, args -> McpPage.from(tagService.findAll(
            paging.toPageable(integer(args, "page"), integer(args, "size"), Sort.by("name")))));
    }

    private SyncToolSpecification getTagTool() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", uuidProp("The tag ID."));
        final McpSchema.Tool tool = tool("get_tag", "Get a single tag by ID.", props, List.of("id"));
        return spec(tool, args -> {
            final UUID id = requiredUuid(args, "id");
            return tagService.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tag not found: " + id));
        }, args -> uuid(args, "id"));
    }

    private SyncToolSpecification listCompanyCommentsTool() {
        final Map<String, Object> props = paginationProps();
        props.put("companyId", uuidProp("The company ID."));
        final McpSchema.Tool tool = tool("list_company_comments",
            "List the comments (full text) attached to a company." + PAGINATION_HINT,
            props, List.of("companyId"));
        return spec(tool, args -> paginate(
            companyService.listCommentsOfCompany(requiredUuid(args, "companyId")),
            integer(args, "page"), integer(args, "size")));
    }

    private SyncToolSpecification listContactCommentsTool() {
        final Map<String, Object> props = paginationProps();
        props.put("contactId", uuidProp("The contact ID."));
        final McpSchema.Tool tool = tool("list_contact_comments",
            "List the comments (full text) attached to a contact." + PAGINATION_HINT,
            props, List.of("contactId"));
        return spec(tool, args -> paginate(
            contactService.listCommentsOfContact(requiredUuid(args, "contactId")),
            integer(args, "page"), integer(args, "size")));
    }

    // -- Dispatch + error mapping -------------------------------------------

    @FunctionalInterface
    private interface ToolLogic {
        Object run(Map<String, Object> args) throws Exception;
    }

    private SyncToolSpecification spec(final McpSchema.Tool tool, final ToolLogic logic) {
        return spec(tool, logic, null);
    }

    /**
     * @param entityIdExtractor extracts the targeted entity id from the (already validated)
     *                          arguments for single-entity {@code get_*} tools, so the audit
     *                          entry records which record was read; {@code null} for
     *                          collection/search tools (which audit the nil sentinel)
     */
    private SyncToolSpecification spec(final McpSchema.Tool tool, final ToolLogic logic,
                                       final Function<Map<String, Object>, UUID> entityIdExtractor) {
        return new SyncToolSpecification(tool, (exchange, args) ->
            invoke(tool.name(), logic, entityIdExtractor, args, actorResolver.resolve(exchange.transportContext())));
    }

    private McpSchema.CallToolResult invoke(final String name, final ToolLogic logic,
                                            final Function<Map<String, Object>, UUID> entityIdExtractor,
                                            final Map<String, Object> rawArgs, final McpActor actor) {
        final Map<String, Object> args = rawArgs == null ? Map.of() : rawArgs;
        try {
            final Object payload = logic.run(args);
            final UUID entityId = entityIdExtractor == null ? null : entityIdExtractor.apply(args);
            audit.recordSuccess(name, entityId, actor);
            return new McpSchema.CallToolResult(json(payload), false);
        } catch (final IllegalArgumentException e) {
            audit.recordFailure(name, "invalid argument", actor);
            return new McpSchema.CallToolResult("Invalid argument: " + e.getMessage(), true);
        } catch (final NoSuchElementException e) {
            audit.recordFailure(name, "not found", actor);
            return new McpSchema.CallToolResult(e.getMessage(), true);
        } catch (final McpUnavailableException e) {
            audit.recordFailure(name, "unavailable", actor);
            return new McpSchema.CallToolResult(e.getMessage(), true);
        } catch (final Exception e) {
            audit.recordFailure(name, e.getClass().getSimpleName(), actor);
            return new McpSchema.CallToolResult("Internal error executing tool " + name, true);
        }
    }

    private String json(final Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize MCP tool payload", e);
        }
    }

    private <T> McpPage<T> paginate(final List<T> all, final Integer pageReq, final Integer sizeReq) {
        final int size = paging.resolveSize(sizeReq);
        final int page = paging.resolvePage(pageReq);
        final int from = Math.min(page * size, all.size());
        final int to = Math.min(from + size, all.size());
        return new McpPage<>(new ArrayList<>(all.subList(from, to)), page, size, all.size(), to < all.size());
    }

    // -- Schema + argument helpers ------------------------------------------

    private static McpSchema.Tool tool(final String name, final String description,
                                       final Map<String, Object> properties, final List<String> required) {
        return McpSchema.Tool.builder()
            .name(name)
            .description(description)
            .inputSchema(new McpSchema.JsonSchema("object", properties, required, Boolean.FALSE, null, null))
            .build();
    }

    private static Map<String, Object> paginationProps() {
        final Map<String, Object> props = new LinkedHashMap<>();
        props.put("page", prop("integer", "Zero-based page index (default 0)."));
        props.put("size", prop("integer", "Page size (default 20, max 50)."));
        return props;
    }

    private static Map<String, Object> prop(final String type, final String description) {
        return Map.of("type", type, "description", description);
    }

    private static Map<String, Object> uuidProp(final String description) {
        return Map.of("type", "string", "format", "uuid", "description", description);
    }

    private static Map<String, Object> uuidArray(final String description) {
        return Map.of("type", "array", "description", description,
            "items", Map.of("type", "string", "format", "uuid"));
    }

    private static String string(final Map<String, Object> args, final String key) {
        final Object value = args.get(key);
        return value == null ? null : value.toString();
    }

    private static String requiredString(final Map<String, Object> args, final String key) {
        final String value = string(args, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private static Integer integer(final Map<String, Object> args, final String key) {
        final Object value = args.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
    }

    private static Boolean bool(final Map<String, Object> args, final String key) {
        final Object value = args.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.valueOf(value.toString().trim());
    }

    private static UUID uuid(final Map<String, Object> args, final String key) {
        final String value = string(args, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(key + " must be a valid UUID");
        }
    }

    private static UUID requiredUuid(final Map<String, Object> args, final String key) {
        final UUID value = uuid(args, key);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private static List<UUID> uuidList(final Map<String, Object> args, final String key) {
        final Object value = args.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(key + " must be an array of UUIDs");
        }
        final List<UUID> result = new ArrayList<>(list.size());
        for (final Object element : list) {
            try {
                result.add(UUID.fromString(String.valueOf(element).trim()));
            } catch (final IllegalArgumentException e) {
                throw new IllegalArgumentException(key + " must contain valid UUIDs");
            }
        }
        return result;
    }
}
