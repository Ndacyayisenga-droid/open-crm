# Behaviors: Protect User-Editable Fields During Brevo Re-Import

## First Import — New Contact

### All fields are populated from Brevo on first import

- **Given** Brevo has a contact with VORNAME="Anna", NACHNAME="Schmidt", E-MAIL="anna@test.com", SPRACHE=1, JOB_TITLE="CEO", SMS="+49123", LINKEDIN="https://linkedin.com/in/anna"
- **And** this contact does not exist in the CRM
- **When** a Brevo sync is triggered
- **Then** a new contact is created with firstName="Anna", lastName="Schmidt", email="anna@test.com", language=DE, position="CEO", phoneNumber="+49123", linkedInUrl="https://linkedin.com/in/anna"

### Company is assigned on first import via linkedContactsIds

- **Given** Brevo has company "Acme" linked to contact ID 200
- **And** contact 200 does not exist in the CRM
- **When** a Brevo sync is triggered
- **Then** the new contact is associated with company "Acme"

### Company is assigned on first import via FIRMA_MANUELL

- **Given** Brevo has a contact with FIRMA_MANUELL="NewCorp"
- **And** the contact does not exist in the CRM
- **When** a Brevo sync is triggered
- **Then** the new contact is associated with company "NewCorp" (created if needed)

## Re-Import — Existing Contact

### Brevo-managed fields are overwritten on re-import

- **Given** a contact exists with brevoId="200", firstName="Anna", lastName="Schmidt"
- **And** Brevo now has VORNAME="Anne", NACHNAME="Müller" for this contact
- **When** a Brevo sync is triggered
- **Then** the contact is updated to firstName="Anne", lastName="Müller"

### Email is overwritten on re-import

- **Given** a contact exists with brevoId="200", email="old@test.com"
- **And** Brevo now has E-MAIL="new@test.com" for this contact
- **When** a Brevo sync is triggered
- **Then** the contact's email is updated to "new@test.com"

### Language is overwritten on re-import

- **Given** a contact exists with brevoId="200", language=DE
- **And** Brevo now has SPRACHE=2 (EN) for this contact
- **When** a Brevo sync is triggered
- **Then** the contact's language is updated to EN

### Position is NOT overwritten on re-import

- **Given** a contact exists with brevoId="200", position="CTO" (manually edited)
- **And** Brevo has JOB_TITLE="CEO" for this contact
- **When** a Brevo sync is triggered
- **Then** the contact's position remains "CTO"

### Phone number is NOT overwritten on re-import

- **Given** a contact exists with brevoId="200", phoneNumber="+49999" (manually edited)
- **And** Brevo has SMS="+49123" for this contact
- **When** a Brevo sync is triggered
- **Then** the contact's phoneNumber remains "+49999"

### LinkedIn URL is NOT overwritten on re-import

- **Given** a contact exists with brevoId="200", linkedInUrl="https://linkedin.com/in/anna-custom"
- **And** Brevo has LINKEDIN="https://linkedin.com/in/anna" for this contact
- **When** a Brevo sync is triggered
- **Then** the contact's linkedInUrl remains "https://linkedin.com/in/anna-custom"

### Company assignment is NOT overwritten on re-import

- **Given** a contact exists with brevoId="200", assigned to company "MyCompany" (manually set)
- **And** Brevo has this contact linked to company "OtherCompany"
- **When** a Brevo sync is triggered
- **Then** the contact remains assigned to "MyCompany"

### FIRMA_MANUELL is ignored on re-import

- **Given** a contact exists with brevoId="200", assigned to company "MyCompany"
- **And** Brevo has FIRMA_MANUELL="DifferentCompany" for this contact
- **When** a Brevo sync is triggered
- **Then** the contact remains assigned to "MyCompany"

## Edge Cases

### Re-import with null user-editable fields preserves existing values

- **Given** a contact exists with brevoId="200", position="CEO", phoneNumber="+49123"
- **And** Brevo has JOB_TITLE=null and SMS=null for this contact
- **When** a Brevo sync is triggered
- **Then** the contact's position remains "CEO" and phoneNumber remains "+49123"

### Contact matched by email (not brevoId) is treated as existing

- **Given** a contact exists with email="anna@test.com", brevoId=NULL, position="CEO"
- **And** Brevo has a contact with email "anna@test.com" and JOB_TITLE="Developer"
- **When** a Brevo sync is triggered
- **Then** the contact is matched by email
- **And** firstName, lastName, email, language are overwritten from Brevo
- **And** position remains "CEO" (not overwritten to "Developer")
- **And** brevoId is set on the contact

### Import counters still report correctly

- **Given** 5 existing contacts with brevoIds and 3 new contacts in Brevo
- **When** a Brevo sync is triggered
- **Then** the result shows companiesImported=3, companiesUpdated=5
- **And** all 5 existing contacts have their user-editable fields preserved
