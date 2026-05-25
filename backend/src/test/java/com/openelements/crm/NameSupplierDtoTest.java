package com.openelements.crm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openelements.crm.company.CompanyDto;
import com.openelements.crm.contact.ContactDto;
import com.openelements.spring.base.data.NameSupplier;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code @NameSupplier} adoption on {@link CompanyDto} and
 * {@link ContactDto}: the annotated {@code displayName()} method exists, is
 * annotated, and returns the value spring-services 0.16 writes to
 * {@code audit_log.entity_name}.
 */
class NameSupplierDtoTest {

    private static CompanyDto company(final String name) {
        return new CompanyDto(UUID.randomUUID(), name, null, null, null, null, null, null, null,
            null, null, null, null, null, null, false, false, 0L, 0L, List.of(),
            Instant.now(), Instant.now());
    }

    private static ContactDto contact(final String title, final String first, final String last) {
        return new ContactDto(UUID.randomUUID(), title, first, last, null, null, null, List.of(),
            null, null, null, null, 0L, false, null, false, false, null, List.of(),
            Instant.now(), Instant.now());
    }

    @Test
    void companyDisplayNameReturnsName() {
        assertEquals("Acme GmbH", company("Acme GmbH").displayName());
    }

    @Test
    void companyDisplayNameIsEmptyForNullName() {
        assertEquals("", company(null).displayName());
    }

    @Test
    void contactDisplayNameJoinsFirstAndLastWithoutTitle() {
        assertEquals("Jane Doe", contact("Dr.", "Jane", "Doe").displayName());
    }

    @Test
    void contactDisplayNameTrimsMissingNameParts() {
        assertEquals("Jane", contact(null, "Jane", null).displayName());
    }

    @Test
    void displayNameMethodsAreAnnotatedWithNameSupplier() throws NoSuchMethodException {
        assertTrue(CompanyDto.class.getMethod("displayName").isAnnotationPresent(NameSupplier.class),
            "CompanyDto.displayName() must carry @NameSupplier");
        assertTrue(ContactDto.class.getMethod("displayName").isAnnotationPresent(NameSupplier.class),
            "ContactDto.displayName() must carry @NameSupplier");
    }
}
