package com.openelements.crm.updates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openelements.crm.AbstractDbTest;
import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.company.CompanyRepository;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.contact.ContactRepository;
import com.openelements.spring.base.services.audit.AuditAction;
import com.openelements.spring.base.services.audit.AuditLogDataService;
import com.openelements.spring.base.services.user.SystemUser;
import com.openelements.spring.base.services.user.UserEntity;
import com.openelements.spring.base.services.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class UpdatesServiceTest extends AbstractDbTest {

    @Autowired
    private UpdatesService updatesService;

    @Autowired
    private AuditLogDataService auditLogDataService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserEntity alice;
    private UserEntity bob;

    @BeforeEach
    void resetAuditLog() {
        jdbcTemplate.update("DELETE FROM audit_log");
        seedSystemUser();
        alice = ensureUser("alice", "Alice");
        bob = ensureUser("bob", "Bob");
    }

    private void seedSystemUser() {
        if (userRepository.findBySub(SystemUser.SUB).isEmpty()) {
            jdbcTemplate.update(
                "INSERT INTO users (id, sub, name, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                SystemUser.ID, SystemUser.SUB, SystemUser.NAME);
        }
    }

    private UserEntity ensureUser(final String sub, final String name) {
        return userRepository.findBySub(sub).orElseGet(() -> {
            final UserEntity entity = new UserEntity();
            entity.setSub(sub);
            entity.setName(name);
            return userRepository.saveAndFlush(entity);
        });
    }

    private CompanyEntity newCompany(final String name) {
        final CompanyEntity company = new CompanyEntity();
        company.setName(name);
        return companyRepository.saveAndFlush(company);
    }

    private CompanyEntity newCompanyWithLogo(final String name) {
        final CompanyEntity company = new CompanyEntity();
        company.setName(name);
        company.setLogo(new byte[]{1, 2, 3});
        company.setLogoContentType("image/png");
        return companyRepository.saveAndFlush(company);
    }

    private ContactEntity newContact(final String firstName, final String lastName) {
        final ContactEntity contact = new ContactEntity();
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        return contactRepository.saveAndFlush(contact);
    }

    private ContactEntity newContactWithPhoto(final String firstName, final String lastName) {
        final ContactEntity contact = new ContactEntity();
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        contact.setPhoto(new byte[]{4, 5, 6});
        contact.setPhotoContentType("image/png");
        return contactRepository.saveAndFlush(contact);
    }

    private void tinyPause() throws InterruptedException {
        Thread.sleep(5L);
    }

    @Test
    void emptyAuditLogReturnsEmptyContent() {
        final List<UpdateEntryDto> result = updatesService.load(20);
        assertTrue(result.isEmpty(), "empty audit log → empty list");
    }

    @Test
    void companyCreatedReturnsCorrectEntry() {
        final CompanyEntity company = newCompany("Open Elements GmbH");
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.INSERT, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        final UpdateEntryDto entry = result.get(0);
        assertEquals(UpdateType.COMPANY_CREATED, entry.type());
        assertEquals(company.getId(), entry.entityId());
        assertEquals("Open Elements GmbH", entry.entityName());
        assertEquals(alice.id(), entry.user().id());
    }

    @Test
    void companyUpdatedReturnsCurrentName() {
        final CompanyEntity company = newCompany("Acme");
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size());
        assertEquals(UpdateType.COMPANY_UPDATED, result.get(0).type());
        assertEquals("Acme", result.get(0).entityName());
    }

    @Test
    void companyDeletedHasNullEntityIdAndName() {
        auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.DELETE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size());
        assertEquals(UpdateType.COMPANY_DELETED, result.get(0).type());
        assertNull(result.get(0).entityId());
        assertNull(result.get(0).entityName());
    }

    @Test
    void renamedCompanyShowsCurrentNameForOlderEntries() throws InterruptedException {
        final CompanyEntity company = newCompany("ACME");
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.INSERT, alice);
        tinyPause();
        company.setName("ACME Corp");
        companyRepository.saveAndFlush(company);
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(2, result.size());
        for (final UpdateEntryDto e : result) {
            assertEquals("ACME Corp", e.entityName());
        }
    }

    @Test
    void contactCreatedUsesDisplayName() {
        final ContactEntity contact = newContact("John", "Doe");
        auditLogDataService.createEntry("ContactDto", contact.getId(), AuditAction.INSERT, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size());
        assertEquals(UpdateType.CONTACT_CREATED, result.get(0).type());
        assertEquals(contact.getId(), result.get(0).entityId());
        assertEquals("John Doe", result.get(0).entityName());
    }

    @Test
    void contactDeletedHasNullIdAndName() {
        auditLogDataService.createEntry("ContactDto", UUID.randomUUID(), AuditAction.DELETE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size());
        assertEquals(UpdateType.CONTACT_DELETED, result.get(0).type());
        assertNull(result.get(0).entityId());
        assertNull(result.get(0).entityName());
    }

    @Test
    void companyCommentCreatedUsesParentName() {
        final CompanyEntity company = newCompany("Open Elements GmbH");
        auditLogDataService.createEntry("CompanyComment", company.getId(), AuditAction.INSERT, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size());
        assertEquals(UpdateType.COMPANY_COMMENT_CREATED, result.get(0).type());
        assertEquals(company.getId(), result.get(0).entityId());
        assertEquals("Open Elements GmbH", result.get(0).entityName());
    }

    @Test
    void companyCommentDeletedKeepsParentName() {
        final CompanyEntity company = newCompany("Open Elements GmbH");
        auditLogDataService.createEntry("CompanyComment", company.getId(), AuditAction.DELETE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size());
        assertEquals(UpdateType.COMPANY_COMMENT_DELETED, result.get(0).type());
        assertEquals(company.getId(), result.get(0).entityId());
        assertEquals("Open Elements GmbH", result.get(0).entityName());
    }

    @Test
    void contactCommentCreatedUsesContactName() {
        final ContactEntity contact = newContact("Jane", "Doe");
        auditLogDataService.createEntry("ContactComment", contact.getId(), AuditAction.INSERT, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size());
        assertEquals(UpdateType.CONTACT_COMMENT_CREATED, result.get(0).type());
        assertEquals(contact.getId(), result.get(0).entityId());
        assertEquals("Jane Doe", result.get(0).entityName());
    }

    @Test
    void dedupeMergesTwoConsecutiveUpdatesOnSameCompanyBySameUser() throws InterruptedException {
        final CompanyEntity company = newCompany("Acme");
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);
        tinyPause();
        final var secondId = auditLogDataService
            .createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice).id();

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        assertEquals(UpdateType.COMPANY_UPDATED, result.get(0).type());
        assertEquals(secondId, result.get(0).id(), "newest id wins");
    }

    @Test
    void dedupeDoesNotMergeUpdatesByDifferentUsers() throws InterruptedException {
        final CompanyEntity company = newCompany("Acme");
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);
        tinyPause();
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, bob);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(2, result.size());
    }

    @Test
    void dedupeDoesNotMergeUpdatesOnDifferentCompanies() throws InterruptedException {
        final CompanyEntity c1 = newCompany("Acme");
        final CompanyEntity c2 = newCompany("Globex");
        auditLogDataService.createEntry("CompanyDto", c1.getId(), AuditAction.UPDATE, alice);
        tinyPause();
        auditLogDataService.createEntry("CompanyDto", c2.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(2, result.size());
    }

    @Test
    void dedupeDoesNotMergeCreateFollowedByUpdate() throws InterruptedException {
        final CompanyEntity company = newCompany("Acme");
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.INSERT, alice);
        tinyPause();
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(2, result.size());
        assertEquals(UpdateType.COMPANY_UPDATED, result.get(0).type());
        assertEquals(UpdateType.COMPANY_CREATED, result.get(1).type());
    }

    @Test
    void dedupeDoesNotMergeUpdateFollowedByDelete() throws InterruptedException {
        final CompanyEntity company = newCompany("Acme");
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);
        tinyPause();
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.DELETE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(2, result.size());
        assertEquals(UpdateType.COMPANY_DELETED, result.get(0).type());
        assertEquals(UpdateType.COMPANY_UPDATED, result.get(1).type());
    }

    @Test
    void dedupeAppliesToContactUpdates() throws InterruptedException {
        final ContactEntity contact = newContact("Jane", "Doe");
        auditLogDataService.createEntry("ContactDto", contact.getId(), AuditAction.UPDATE, alice);
        tinyPause();
        auditLogDataService.createEntry("ContactDto", contact.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size());
        assertEquals(UpdateType.CONTACT_UPDATED, result.get(0).type());
    }

    @Test
    void dedupeAppliesToCommentUpdatesOnSameParent() throws InterruptedException {
        final CompanyEntity company = newCompany("Acme");
        auditLogDataService.createEntry("CompanyComment", company.getId(), AuditAction.UPDATE, alice);
        tinyPause();
        auditLogDataService.createEntry("CompanyComment", company.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size());
        assertEquals(UpdateType.COMPANY_COMMENT_UPDATED, result.get(0).type());
    }

    @Test
    void dedupeSkipsFilteredEntriesBetweenCandidates() throws InterruptedException {
        final CompanyEntity company = newCompany("Acme");
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);
        tinyPause();
        auditLogDataService.createEntry("Webhook", UUID.randomUUID(), AuditAction.UPDATE, alice);
        tinyPause();
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size(), "filtered entry between two UPDATEs should be dropped, the two UPDATEs collapse");
    }

    @Test
    void outOfScopeEntityTypesAreNotExposed() {
        auditLogDataService.createEntry("Webhook", UUID.randomUUID(), AuditAction.INSERT, alice);
        auditLogDataService.createEntry("ApiKey", UUID.randomUUID(), AuditAction.INSERT, alice);
        auditLogDataService.createEntry("Tag", UUID.randomUUID(), AuditAction.INSERT, alice);
        auditLogDataService.createEntry("UserDto", UUID.randomUUID(), AuditAction.INSERT, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertTrue(result.isEmpty(), "non-relevant entity types must be filtered out");
    }

    @Test
    void entriesAreSortedNewestFirst() throws InterruptedException {
        final CompanyEntity company = newCompany("Acme");
        final var firstId = auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.INSERT, alice).id();
        tinyPause();
        final var secondId = auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice).id();
        tinyPause();
        final var thirdId = auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.DELETE, alice).id();

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(3, result.size());
        assertEquals(thirdId, result.get(0).id());
        assertEquals(secondId, result.get(1).id());
        assertEquals(firstId, result.get(2).id());
    }

    @Test
    void fewerEntriesThanRequestedSizeReturnsAll() {
        final CompanyEntity company = newCompany("Acme");
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.INSERT, alice);
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.DELETE, alice);
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(3, result.size());
    }

    @Test
    void iterativeFetchReturnsUpToSizeAfterHeavyDedupe() throws InterruptedException {
        final CompanyEntity company = newCompany("Acme");
        // Write 50 UPDATEs by alice — should collapse to one
        for (int i = 0; i < 50; i++) {
            auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);
        }
        // Write 20 distinct INSERTs (different ids)
        for (int i = 0; i < 20; i++) {
            tinyPause();
            auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.INSERT, alice);
        }

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(20, result.size());
    }

    @Test
    void entityVanishesBetweenAuditEmissionAndRead() {
        final UUID unknownId = UUID.randomUUID();
        auditLogDataService.createEntry("CompanyDto", unknownId, AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size());
        assertEquals(unknownId, result.get(0).entityId());
        assertNull(result.get(0).entityName());
    }

    @Test
    void commentParentHasBeenDeleted() {
        final UUID unknownParent = UUID.randomUUID();
        auditLogDataService.createEntry("CompanyComment", unknownParent, AuditAction.DELETE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(1, result.size());
        assertEquals(UpdateType.COMPANY_COMMENT_DELETED, result.get(0).type());
        assertNull(result.get(0).entityName());
        assertEquals(unknownParent, result.get(0).entityId());
    }

    @Test
    void taskCommentsAreNotEmittedAsUpdates() {
        // No "TaskComment" entityType is in scope; even if such an entry were present, it must be filtered out.
        auditLogDataService.createEntry("TaskComment", UUID.randomUUID(), AuditAction.INSERT, alice);
        auditLogDataService.createEntry("Task", UUID.randomUUID(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertTrue(result.isEmpty());
    }

    @Test
    void companyEventWithLogoSetsEntityHasLogoTrue() {
        final CompanyEntity company = newCompanyWithLogo("Open Elements GmbH");
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        assertTrue(result.get(0).entityHasLogo());
        assertFalse(result.get(0).entityHasPhoto());
    }

    @Test
    void companyEventWithoutLogoSetsEntityHasLogoFalse() {
        final CompanyEntity company = newCompany("Acme");
        auditLogDataService.createEntry("CompanyDto", company.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        assertFalse(result.get(0).entityHasLogo());
        assertFalse(result.get(0).entityHasPhoto());
    }

    @Test
    void contactEventWithPhotoSetsEntityHasPhotoTrue() {
        final ContactEntity contact = newContactWithPhoto("John", "Doe");
        auditLogDataService.createEntry("ContactDto", contact.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        assertTrue(result.get(0).entityHasPhoto());
        assertFalse(result.get(0).entityHasLogo());
    }

    @Test
    void contactEventWithoutPhotoSetsEntityHasPhotoFalse() {
        final ContactEntity contact = newContact("Jane", "Doe");
        auditLogDataService.createEntry("ContactDto", contact.getId(), AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        assertFalse(result.get(0).entityHasPhoto());
        assertFalse(result.get(0).entityHasLogo());
    }

    @Test
    void companyDeletedHasBothFlagsFalse() {
        auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.DELETE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        assertFalse(result.get(0).entityHasLogo());
        assertFalse(result.get(0).entityHasPhoto());
    }

    @Test
    void contactDeletedHasBothFlagsFalse() {
        auditLogDataService.createEntry("ContactDto", UUID.randomUUID(), AuditAction.DELETE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        assertFalse(result.get(0).entityHasLogo());
        assertFalse(result.get(0).entityHasPhoto());
    }

    @Test
    void companyCommentEventInheritsParentLogoFlag() {
        final CompanyEntity company = newCompanyWithLogo("Open Elements GmbH");
        auditLogDataService.createEntry("CompanyComment", company.getId(), AuditAction.INSERT, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        assertTrue(result.get(0).entityHasLogo());
        assertFalse(result.get(0).entityHasPhoto());
    }

    @Test
    void companyCommentDeletedKeepsParentLogoFlag() {
        final CompanyEntity company = newCompanyWithLogo("Open Elements GmbH");
        auditLogDataService.createEntry("CompanyComment", company.getId(), AuditAction.DELETE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        assertEquals(UpdateType.COMPANY_COMMENT_DELETED, result.get(0).type());
        assertTrue(result.get(0).entityHasLogo());
    }

    @Test
    void contactCommentEventInheritsParentPhotoFlag() {
        final ContactEntity contact = newContactWithPhoto("Jane", "Doe");
        auditLogDataService.createEntry("ContactComment", contact.getId(), AuditAction.INSERT, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        assertTrue(result.get(0).entityHasPhoto());
        assertFalse(result.get(0).entityHasLogo());
    }

    @Test
    void unresolvedEntityHasBothFlagsFalse() {
        final UUID unknownId = UUID.randomUUID();
        auditLogDataService.createEntry("CompanyDto", unknownId, AuditAction.UPDATE, alice);

        final List<UpdateEntryDto> result = updatesService.load(20);

        assertEquals(1, result.size());
        assertEquals(unknownId, result.get(0).entityId());
        assertNull(result.get(0).entityName());
        assertFalse(result.get(0).entityHasLogo());
        assertFalse(result.get(0).entityHasPhoto());
    }

    @Test
    void loadReturnsAtMostRequestedSize() throws InterruptedException {
        final CompanyEntity company = newCompany("Acme");
        for (int i = 0; i < 30; i++) {
            tinyPause();
            auditLogDataService.createEntry("CompanyDto", UUID.randomUUID(), AuditAction.INSERT, alice);
        }
        assertNotNull(company);

        final List<UpdateEntryDto> result = updatesService.load(20);
        assertEquals(20, result.size());
    }
}
