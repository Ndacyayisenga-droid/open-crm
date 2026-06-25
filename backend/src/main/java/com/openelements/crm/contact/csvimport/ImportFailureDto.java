package com.openelements.crm.contact.csvimport;

import java.util.Map;

/**
 * One failed row from a CSV import commit.
 */
public record ImportFailureDto(
    int row,
    String field,
    String reason,
    Map<String, String> cells
) {
}
