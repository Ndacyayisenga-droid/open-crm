package com.openelements.crm.contact.csvimport;

/**
 * Mapping targets for CSV contact import columns.
 */
public enum ImportTarget {
    TITLE("title", false, 255),
    FIRST_NAME("firstName", true, 255),
    LAST_NAME("lastName", true, 255),
    EMAIL("email", false, 255),
    POSITION("position", false, 255),
    PHONE_NUMBER("phoneNumber", false, 50),
    LINKEDIN_URL("linkedInUrl", false, 500),
    WEBSITE_URL("websiteUrl", false, 500);

    private final String fieldName;
    private final boolean requiredMapping;
    private final int maxLength;

    ImportTarget(final String fieldName, final boolean requiredMapping, final int maxLength) {
        this.fieldName = fieldName;
        this.requiredMapping = requiredMapping;
        this.maxLength = maxLength;
    }

    public String fieldName() {
        return fieldName;
    }

    public boolean requiredMapping() {
        return requiredMapping;
    }

    public int maxLength() {
        return maxLength;
    }
}
