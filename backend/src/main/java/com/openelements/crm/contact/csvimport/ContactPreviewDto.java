package com.openelements.crm.contact.csvimport;

import java.util.List;

/**
 * Preview of one CSV row after mapping and validation.
 */
public record ContactPreviewDto(
    int row,
    ContactPreviewFields contact,
    List<RowError> errors
) {
}
