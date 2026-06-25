package com.openelements.crm.contact.csvimport;

import java.util.List;
import java.util.Map;

/**
 * Request body for CSV import preview and commit endpoints.
 */
public record ContactImportRequest(
    String encoding,
    boolean hasHeader,
    Map<String, String> mapping
) {
}
