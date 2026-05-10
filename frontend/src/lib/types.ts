export interface CompanyDto {
  readonly id: string;
  readonly name: string;
  readonly email: string | null;
  readonly website: string | null;
  readonly street: string | null;
  readonly houseNumber: string | null;
  readonly zipCode: string | null;
  readonly city: string | null;
  readonly country: string | null;
  readonly phoneNumber: string | null;
  readonly description: string | null;
  readonly bankName: string | null;
  readonly bic: string | null;
  readonly iban: string | null;
  readonly vatId: string | null;
  readonly brevo: boolean;
  readonly hasLogo: boolean;
  readonly contactCount: number;
  readonly commentCount: number;
  readonly tagIds: readonly string[];
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CompanyCreateDto {
  readonly name: string;
  readonly email?: string | null;
  readonly website?: string | null;
  readonly street?: string | null;
  readonly houseNumber?: string | null;
  readonly zipCode?: string | null;
  readonly city?: string | null;
  readonly country?: string | null;
  readonly phoneNumber?: string | null;
  readonly description?: string | null;
  readonly bankName?: string | null;
  readonly bic?: string | null;
  readonly iban?: string | null;
  readonly vatId?: string | null;
  readonly tagIds?: readonly string[] | null;
}

export interface CommentDto {
  readonly id: string;
  readonly text: string;
  readonly author: UserDto | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CommentCreateDto {
  readonly text: string;
}

export interface SocialLinkDto {
  readonly networkType: string;
  readonly value: string;
  readonly url: string;
}

export interface SocialLinkCreateDto {
  readonly networkType: string;
  readonly value: string;
}

export interface ContactDto {
  readonly id: string;
  readonly title: string | null;
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string | null;
  readonly position: string | null;
  readonly gender: "MALE" | "FEMALE" | "DIVERSE" | null;
  readonly socialLinks: readonly SocialLinkDto[];
  readonly phoneNumber: string | null;
  readonly description: string | null;
  readonly companyId: string | null;
  readonly companyName: string | null;
  readonly commentCount: number;
  readonly hasPhoto: boolean;
  readonly birthday: string | null;
  readonly brevo: boolean;
  readonly receivesNewsletter: boolean;
  readonly language: "DE" | "EN" | null;
  readonly tagIds: readonly string[];
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ContactCreateDto {
  readonly title?: string | null;
  readonly firstName: string;
  readonly lastName: string;
  readonly email?: string | null;
  readonly position?: string | null;
  readonly gender?: "MALE" | "FEMALE" | "DIVERSE" | null;
  readonly socialLinks?: readonly SocialLinkCreateDto[] | null;
  readonly phoneNumber?: string | null;
  readonly companyId?: string | null;
  readonly language?: "DE" | "EN" | null;
  readonly birthday?: string | null;
  readonly description?: string | null;
  readonly tagIds?: readonly string[] | null;
}

export interface Page<T> {
  readonly content: readonly T[];
  readonly page: {
    readonly size: number;
    readonly number: number;
    readonly totalElements: number;
    readonly totalPages: number;
  };
}

export interface TagCreateDto {
  readonly name: string;
  readonly description?: string | null;
  readonly color: string;
}

export type TaskStatus = "OPEN" | "IN_PROGRESS" | "DONE";

export interface TaskDto {
  readonly id: string;
  readonly action: string;
  readonly dueDate: string;
  readonly status: TaskStatus;
  readonly companyId: string | null;
  readonly companyName: string | null;
  readonly contactId: string | null;
  readonly contactName: string | null;
  readonly tagIds: readonly string[];
  readonly commentCount: number;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface TaskCreateDto {
  readonly action: string;
  readonly dueDate: string;
  readonly status?: TaskStatus;
  readonly companyId?: string | null;
  readonly contactId?: string | null;
  readonly tagIds?: readonly string[] | null;
}

export interface TaskUpdateDto {
  readonly action: string;
  readonly dueDate: string;
  readonly status: TaskStatus;
  readonly tagIds?: readonly string[] | null;
}

export interface UserDto {
  readonly id: string;
  readonly name: string;
  readonly email: string;
  readonly avatarUrl: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export type AuditAction = "INSERT" | "UPDATE" | "DELETE";

export interface AuditLogDto {
  readonly id: string;
  readonly entityType: string;
  readonly entityId: string;
  readonly action: AuditAction;
  readonly user: string;
  readonly createdAt: string;
}

export interface BrevoSettingsDto {
  readonly apiKeyConfigured: boolean;
}

export interface TranslationConfigDto {
  readonly configured: boolean;
}

export interface TranslateResponseDto {
  readonly translatedText: string;
}

export interface WebhookDto {
  readonly id: string;
  readonly url: string;
  readonly active: boolean;
  readonly lastStatus: number | null;
  readonly lastCalledAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface WebhookCreateDto {
  readonly url: string;
}

export interface WebhookUpdateDto {
  readonly url: string;
  readonly active: boolean;
}

export interface ApiKeyDto {
  readonly id: string;
  readonly name: string;
  readonly keyPrefix: string;
  readonly createdBy: string;
  readonly createdAt: string;
}

export interface ApiKeyCreateDto {
  readonly name: string;
}

export interface ApiKeyCreatedDto {
  readonly id: string;
  readonly name: string;
  readonly keyPrefix: string;
  readonly key: string;
  readonly createdBy: string;
  readonly createdAt: string;
}

export interface BrevoSyncResultDto {
  readonly companiesImported: number;
  readonly companiesUpdated: number;
  readonly companiesFailed: number;
  readonly companiesUnlinked: number;
  readonly contactsImported: number;
  readonly contactsUpdated: number;
  readonly contactsFailed: number;
  readonly contactsUnlinked: number;
  readonly errors: readonly string[];
}
