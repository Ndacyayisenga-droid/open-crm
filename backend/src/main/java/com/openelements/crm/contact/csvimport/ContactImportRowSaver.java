package com.openelements.crm.contact.csvimport;

import com.openelements.crm.contact.ContactDto;
import com.openelements.crm.contact.ContactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Commits a single imported contact in its own transaction for partial-success imports.
 */
@Service
public class ContactImportRowSaver {

    private final ContactService contactService;

    public ContactImportRowSaver(final ContactService contactService) {
        this.contactService = Objects.requireNonNull(contactService, "contactService must not be null");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ContactDto save(final ContactDto contact) {
        return contactService.save(contact);
    }
}
