package com.openelements.crm.contact.csvimport;

import java.util.List;
import java.util.Map;

/**
 * Response from the CSV import preview endpoint.
 */
public record ContactImportPreviewResponse(
    String delimiter,
    List<String> columns,
    int totalRows,
    List<Map<String, String>> sampleRows,
    List<ContactPreviewDto> sampleContacts
) {
}
