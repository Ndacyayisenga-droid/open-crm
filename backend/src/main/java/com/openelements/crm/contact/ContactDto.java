package com.openelements.crm.contact;

import com.openelements.spring.base.data.NameSupplier;
import com.openelements.spring.base.data.WithId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Contact response")
public record ContactDto(
    @Schema(description = "Contact ID", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
    @Schema(description = "Title (e.g. Dr., Prof.)") String title,
    @Schema(description = "First name", requiredMode = Schema.RequiredMode.REQUIRED) String firstName,
    @Schema(description = "Last name", requiredMode = Schema.RequiredMode.REQUIRED) String lastName,
    @Schema(description = "Email address") String email,
    @Schema(description = "Job position") String position,
    @Schema(description = "Gender") Gender gender,
    @Schema(description = "Social network links") List<SocialLinkDto> socialLinks,
    @Schema(description = "Phone number") String phoneNumber,
    @Schema(description = "Free-text description") String description,
    @Schema(description = "Company ID") UUID companyId,
    @Schema(description = "Company name") String companyName,
    @Schema(description = "Number of comments", requiredMode = Schema.RequiredMode.REQUIRED) long commentCount,
    @Schema(description = "Whether the contact has an uploaded photo", requiredMode = Schema.RequiredMode.REQUIRED) boolean hasPhoto,
    @Schema(description = "Birthday") LocalDate birthday,
    @Schema(description = "Whether the contact was imported from Brevo", requiredMode = Schema.RequiredMode.REQUIRED) boolean brevo,
    @Schema(description = "Whether the contact receives the newsletter", requiredMode = Schema.RequiredMode.REQUIRED) boolean receivesNewsletter,
    @Schema(description = "Preferred language (null if unknown)") Language language,
    @Schema(description = "Assigned tag IDs") List<UUID> tagIds,
    @Schema(description = "Creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
    @Schema(description = "Last update timestamp", requiredMode = Schema.RequiredMode.REQUIRED) Instant updatedAt
) implements WithId {

    /**
     * Display name used by the spring-services audit log ({@code @NameSupplier}).
     * Returns {@code "firstName lastName"} (trimmed); the title is intentionally
     * not included.
     *
     * @return the audit-log display name
     */
    @NameSupplier
    public String displayName() {
        final String f = firstName == null ? "" : firstName;
        final String l = lastName == null ? "" : lastName;
        return (f + " " + l).trim();
    }
}
