package com.openelements.crm.contact.csvimport;

import java.util.List;

/**
 * Result of a CSV import commit.
 */
public record ImportResult(
    int createdCount,
    int failedCount,
    List<ImportFailureDto> failures
) {
}
