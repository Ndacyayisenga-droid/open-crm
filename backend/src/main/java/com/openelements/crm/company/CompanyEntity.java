package com.openelements.crm.company;

import com.openelements.spring.base.data.AbstractEntity;
import com.openelements.spring.base.data.image.EntityWithImage;
import com.openelements.spring.base.data.image.ImageData;
import com.openelements.spring.base.services.comment.CommentEntity;
import com.openelements.spring.base.services.tag.TagEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * JPA entity representing a company in the CRM system.
 */
@Entity
@Table(name = "companies")
public class CompanyEntity extends AbstractEntity implements EntityWithImage {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "website", length = 500)
    private String website;

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "house_number", length = 20)
    private String houseNumber;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "phone_number", length = 255)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "bank_name", length = 255)
    private String bankName;

    @Column(name = "bic", length = 11)
    private String bic;

    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "vat_id", length = 20)
    private String vatId;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "logo", length = ImageData.MAX_IMAGE_SIZE)
    private byte[] logo;

    @Column(name = "logo_content_type", length = 50)
    private String logoContentType;

    @Column(name = "brevo_company_id", length = 50)
    private String brevoCompanyId;

    @ManyToMany
    @JoinTable(
        name = "company_tags",
        joinColumns = @JoinColumn(name = "company_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<TagEntity> tags = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "company_comments",
        joinColumns = @JoinColumn(name = "company_id"),
        inverseJoinColumns = @JoinColumn(name = "comment_id", unique = true)
    )
    private Set<CommentEntity> comments = new HashSet<>();

    /**
     * Default constructor required by JPA.
     */
    public CompanyEntity() {
    }

    /**
     * Returns the name of this company.
     *
     * @return the company name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this company.
     *
     * @param name the company name
     */
    public void setName(final String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Returns the global email address of this company.
     *
     * @return the email address, or null
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the global email address of this company.
     *
     * @param email the email address
     */
    public void setEmail(final String email) {
        this.email = email;
    }

    /**
     * Returns the website URL of this company.
     *
     * @return the website URL, or null
     */
    public String getWebsite() {
        return website;
    }

    /**
     * Sets the website URL of this company.
     *
     * @param website the website URL
     */
    public void setWebsite(final String website) {
        this.website = website;
    }

    /**
     * Returns the street of this company's address.
     *
     * @return the street, or null
     */
    public String getStreet() {
        return street;
    }

    /**
     * Sets the street of this company's address.
     *
     * @param street the street
     */
    public void setStreet(final String street) {
        this.street = street;
    }

    /**
     * Returns the house number of this company's address.
     *
     * @return the house number, or null
     */
    public String getHouseNumber() {
        return houseNumber;
    }

    /**
     * Sets the house number of this company's address.
     *
     * @param houseNumber the house number
     */
    public void setHouseNumber(final String houseNumber) {
        this.houseNumber = houseNumber;
    }

    /**
     * Returns the zip code of this company's address.
     *
     * @return the zip code, or null
     */
    public String getZipCode() {
        return zipCode;
    }

    /**
     * Sets the zip code of this company's address.
     *
     * @param zipCode the zip code
     */
    public void setZipCode(final String zipCode) {
        this.zipCode = zipCode;
    }

    /**
     * Returns the city of this company's address.
     *
     * @return the city, or null
     */
    public String getCity() {
        return city;
    }

    /**
     * Sets the city of this company's address.
     *
     * @param city the city
     */
    public void setCity(final String city) {
        this.city = city;
    }

    /**
     * Returns the country of this company's address.
     *
     * @return the country, or null
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the country of this company's address.
     *
     * @param country the country
     */
    public void setCountry(final String country) {
        this.country = country;
    }

    /**
     * Returns the phone number.
     *
     * @return the phone number, or null
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /**
     * Sets the phone number.
     *
     * @param phoneNumber the phone number
     */
    public void setPhoneNumber(final String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /**
     * Returns the logo image data.
     *
     * @return the logo bytes, or null if no logo is set
     */
    public byte[] getLogo() {
        return logo;
    }

    /**
     * Sets the logo image data.
     *
     * @param logo the logo bytes, or null to remove
     */
    public void setLogo(final byte[] logo) {
        this.logo = logo;
    }

    /**
     * Returns the MIME content type of the logo.
     *
     * @return the content type, or null if no logo is set
     */
    public String getLogoContentType() {
        return logoContentType;
    }

    /**
     * Sets the MIME content type of the logo.
     *
     * @param logoContentType the content type
     */
    public void setLogoContentType(final String logoContentType) {
        this.logoContentType = logoContentType;
    }

    /**
     * Returns the Brevo CRM company ID used for import matching.
     *
     * @return the Brevo company ID, or null if not imported from Brevo
     */
    public String getBrevoCompanyId() {
        return brevoCompanyId;
    }

    /**
     * Sets the Brevo CRM company ID.
     *
     * @param brevoCompanyId the Brevo company ID
     */
    public void setBrevoCompanyId(final String brevoCompanyId) {
        this.brevoCompanyId = brevoCompanyId;
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

    public String getBankName() {
        return bankName;
    }

    public void setBankName(final String bankName) {
        this.bankName = bankName;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(final String bic) {
        this.bic = bic;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(final String iban) {
        this.iban = iban;
    }

    public String getVatId() {
        return vatId;
    }

    public void setVatId(final String vatId) {
        this.vatId = vatId;
    }


    @Override
    public String toString() {
        return "CompanyEntity[id=" + id() + ", name=" + name + "]";
    }

    @Override
    public byte[] getRawImageData() {
        return logo;
    }

    @Override
    public void setRawImageData(byte[] rawImageData) {
        this.logo = rawImageData;
    }

    @Override
    public String getContentType() {
        return logoContentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.logoContentType = contentType;
    }
}
