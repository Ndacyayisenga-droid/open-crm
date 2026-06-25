package com.openelements.crm.contact.csvimport;

/**
 * Structured error body for import endpoints.
 */
public record ImportErrorResponse(String error, String detail) {
}
