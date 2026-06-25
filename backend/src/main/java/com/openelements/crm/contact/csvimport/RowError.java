package com.openelements.crm.contact.csvimport;

/**
 * Validation error for a single contact field on one CSV row.
 */
public record RowError(String field, String reason) {
}
