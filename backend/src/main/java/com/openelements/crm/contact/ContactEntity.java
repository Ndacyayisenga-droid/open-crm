package com.openelements.crm.contact;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.spring.base.data.AbstractEntity;
import com.openelements.spring.base.data.image.EntityWithImage;
import com.openelements.spring.base.data.image.ImageData;
import com.openelements.spring.base.services.comment.CommentEntity;
import com.openelements.spring.base.services.tag.TagEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * JPA entity representing a contact person in the CRM system.
 */
@Entity
@Table(name = "contacts")
public class ContactEntity extends AbstractEntity implements EntityWithImage {

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "first_name", nullable = false, length = 255)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "position", length = 255)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "contact_id", nullable = false)
    private List<SocialLinkEntity> socialLinks = new ArrayList<>();

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyEntity company;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "photo", length = ImageData.MAX_IMAGE_SIZE)
    private byte[] photo;

    @Column(name = "photo_content_type", length = 50)
    private String photoContentType;

    @Column(name = "brevo_id", length = 50)
    private String brevoId;

    @Column(name = "receives_newsletter", nullable = false)
    private boolean receivesNewsletter = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", length = 5)
    private Language language;

    @ManyToMany
    @JoinTable(
        name = "contact_tags",
        joinColumns = @JoinColumn(name = "contact_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<TagEntity> tags = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "contact_comments",
        joinColumns = @JoinColumn(name = "contact_id"),
        inverseJoinColumns = @JoinColumn(name = "comment_id", unique = true)
    )
    private Set<CommentEntity> comments = new HashSet<>();

    /**
     * Default constructor required by JPA.
     */
    public ContactEntity() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Returns the first name of this contact.
     *
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name of this contact.
     *
     * @param firstName the first name
     */
    public void setFirstName(final String firstName) {
        this.firstName = Objects.requireNonNull(firstName, "firstName must not be null");
    }

    /**
     * Returns the last name of this contact.
     *
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the last name of this contact.
     *
     * @param lastName the last name
     */
    public void setLastName(final String lastName) {
        this.lastName = Objects.requireNonNull(lastName, "lastName must not be null");
    }

    /**
     * Returns the email address of this contact.
     *
     * @return the email address, or null
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of this contact.
     *
     * @param email the email address
     */
    public void setEmail(final String email) {
        this.email = email;
    }

    /**
     * Returns the job position of this contact.
     *
     * @return the position, or null
     */
    public String getPosition() {
        return position;
    }

    /**
     * Sets the job position of this contact.
     *
     * @param position the position
     */
    public void setPosition(final String position) {
        this.position = position;
    }

    /**
     * Returns the gender of this contact.
     *
     * @return the gender, or null if unknown
     */
    public Gender getGender() {
        return gender;
    }

    /**
     * Sets the gender of this contact.
     *
     * @param gender the gender, or null if unknown
     */
    public void setGender(final Gender gender) {
        this.gender = gender;
    }

    public List<SocialLinkEntity> getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(final List<SocialLinkEntity> socialLinks) {
        this.socialLinks = socialLinks;
    }

    /**
     * Returns the phone number of this contact.
     *
     * @return the phone number, or null
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the phone number of this contact.
     *
     * @param phoneNumber the phone number
     */
    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns the company this contact belongs to.
     *
     * @return the company entity, or null if the contact is independent
     */
    public CompanyEntity getCompany() {
        return company;
    }

    /**
     * Sets the company this contact belongs to.
     *
     * @param company the company entity, or null
     */
    public void setCompany(final CompanyEntity company) {
        this.company = company;
    }

    /**
     * Returns the birthday of this contact.
     *
     * @return the birthday, or null
     */
    public LocalDate getBirthday() {
        return birthday;
    }

    /**
     * Sets the birthday of this contact.
     *
     * @param birthday the birthday, or null
     */
    public void setBirthday(final LocalDate birthday) {
        this.birthday = birthday;
    }

    /**
     * Returns the photo image data.
     *
     * @return the photo bytes, or null if no photo is set
     */
    public byte[] getPhoto() {
        return photo;
    }

    /**
     * Sets the photo image data.
     *
     * @param photo the photo bytes, or null to remove
     */
    public void setPhoto(final byte[] photo) {
        this.photo = photo;
    }

    /**
     * Returns the MIME content type of the photo.
     *
     * @return the content type, or null if no photo is set
     */
    public String getPhotoContentType() {
        return photoContentType;
    }

    /**
     * Sets the MIME content type of the photo.
     *
     * @param photoContentType the content type
     */
    public void setPhotoContentType(final String photoContentType) {
        this.photoContentType = photoContentType;
    }

    /**
     * Returns the Brevo contact ID used for import matching.
     *
     * @return the Brevo contact ID, or null if not imported from Brevo
     */
    public String getBrevoId() {
        return brevoId;
    }

    /**
     * Sets the Brevo contact ID.
     *
     * @param brevoId the Brevo contact ID
     */
    public void setBrevoId(final String brevoId) {
        this.brevoId = brevoId;
    }

    public boolean isReceivesNewsletter() {
        return receivesNewsletter;
    }

    public void setReceivesNewsletter(final boolean receivesNewsletter) {
        this.receivesNewsletter = receivesNewsletter;
    }

    /**
     * Returns the preferred language of this contact.
     *
     * @return the language
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * Sets the preferred language of this contact.
     *
     * @param language the language, or null if unknown
     */
    public void setLanguage(final Language language) {
        this.language = language;
    }

    public Set<TagEntity> getTags() {
        return tags;
    }

    public void setTags(final Set<TagEntity> tags) {
        this.tags = tags;
    }

    public Set<CommentEntity> getComments() {
        return comments;
    }

    public void setComments(final Set<CommentEntity> comments) {
        this.comments = comments;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }


    @Override
    public String toString() {
        return "ContactEntity[id=" + id() + ", lastName=" + lastName + "]";
    }

    @Override
    public byte[] getRawImageData() {
        return photo;
    }

    @Override
    public void setRawImageData(byte[] rawImageData) {
        this.photo = rawImageData;
    }


    @Override
    public String getContentType() {
        return photoContentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.photoContentType = contentType;
    }
}
