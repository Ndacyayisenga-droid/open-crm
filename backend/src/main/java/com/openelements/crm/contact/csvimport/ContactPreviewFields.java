package com.openelements.crm.contact.csvimport;

/**
 * Contact-shaped preview of the eight importable fields.
 */
public record ContactPreviewFields(
    String title,
    String firstName,
    String lastName,
    String email,
    String position,
    String phoneNumber,
    String linkedInUrl,
    String websiteUrl
) {
}
