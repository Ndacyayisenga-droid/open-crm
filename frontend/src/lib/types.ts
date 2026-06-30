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

import type { UserDto } from "@open-elements/nextjs-app-layer";

export type {
  Page,
  UserDto,
  AuditAction,
  AuditLogDto,
  ApiKeyDto,
  ApiKeyCreateDto,
  ApiKeyCreatedDto,
  WebhookDto,
  WebhookCreateDto,
  WebhookUpdateDto,
  TranslationConfigDto,
} from "@open-elements/nextjs-app-layer";

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

export type UpdateType =
  | "COMPANY_CREATED"
  | "COMPANY_UPDATED"
  | "COMPANY_DELETED"
  | "CONTACT_CREATED"
  | "CONTACT_UPDATED"
  | "CONTACT_DELETED"
  | "COMPANY_COMMENT_CREATED"
  | "COMPANY_COMMENT_UPDATED"
  | "COMPANY_COMMENT_DELETED"
  | "CONTACT_COMMENT_CREATED"
  | "CONTACT_COMMENT_UPDATED"
  | "CONTACT_COMMENT_DELETED";

export interface UpdateEntryDto {
  readonly id: string;
  readonly type: UpdateType;
  readonly entityId: string | null;
  readonly entityName: string | null;
  readonly entityHasLogo: boolean;
  readonly entityHasPhoto: boolean;
  readonly user: UserDto;
  readonly createdAt: string;
}

export interface BrevoSettingsDto {
  readonly apiKeyConfigured: boolean;
}

export interface TranslateResponseDto {
  readonly translatedText: string;
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

// Backup admin (spec 107)

export interface BackupRetentionDto {
  readonly days: number;
}

export interface BackupIntervalDto {
  readonly iso8601: string;
  readonly seconds: number;
}

export interface BackupLastDto {
  readonly lastSuccessfulBackupAgeSeconds: number;
}

export interface BackupServiceInfoDto {
  // All fields can be null at runtime: when the db-backup-service /info contract
  // does not match the backend's DbBackupClient, Jackson leaves unmapped fields
  // null rather than failing. Callers must guard before dereferencing.
  readonly version: string | null;
  readonly pgDumpVersion: string | null;
  readonly retention: BackupRetentionDto | null;
  readonly backupInterval: BackupIntervalDto | null;
  readonly backup: BackupLastDto | null;
}

export interface BackupStatusDto {
  readonly configured: boolean;
  readonly healthy: boolean;
  readonly info: BackupServiceInfoDto | null;
}

export interface BackupTriggerDto {
  readonly jobId: string;
  readonly alreadyRunning: boolean;
}

export interface BackupItemDto {
  readonly id: string;
  readonly createdAt: string;
  readonly sizeBytes: number;
  readonly sha256: string;
  readonly pgVersion: string;
  readonly durationMs: number;
  readonly triggeredBy: string | null;
}
