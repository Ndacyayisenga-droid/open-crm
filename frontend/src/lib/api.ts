import type {
  AuditLogDto,
  CompanyDto,
  CompanyCreateDto,
  ContactDto,
  ContactCreateDto,
  CommentDto,
  CommentCreateDto,
  TagCreateDto,
  TaskDto,
  TaskCreateDto,
  TaskUpdateDto,
  UserDto,
  WebhookDto,
  WebhookCreateDto,
  WebhookUpdateDto,
  ApiKeyDto,
  ApiKeyCreateDto,
  ApiKeyCreatedDto,
  UpdateEntryDto,
  BrevoSettingsDto,
  BrevoSyncResultDto,
  BackupStatusDto,
  BackupTriggerDto,
  BackupItemDto,
  TranslationConfigDto,
  TranslateResponseDto,
  Page,
} from "./types";
import type { TagDto } from "@open-elements/ui";

import type { TaskStatus } from "./types";

import { auth } from "@/auth";
import { ForbiddenError } from "@open-elements/nextjs-app-layer";

export { ForbiddenError };

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

function isServer(): boolean {
  return typeof window === "undefined";
}

function baseUrl(): string {
  return isServer() ? BACKEND_URL : "";
}

async function apiFetch(url: string, init?: RequestInit): Promise<Response> {
  if (isServer()) {
    try {
      const session = await auth();
      if (session?.accessToken) {
        const headers = new Headers(init?.headers);
        headers.set("Authorization", `Bearer ${session.accessToken}`);
        return fetch(url, { ...init, headers });
      }
    } catch {
      // No session available
    }
    return fetch(url, init);
  }
  const response = await fetch(url, init);
  if (response.status === 401) {
    window.location.href = "/api/logout";
  }
  return response;
}

export interface CompanyListParams {
  readonly page?: number;
  readonly size?: number;
  readonly name?: string;
  readonly brevo?: boolean;
  readonly tagIds?: readonly string[];
}

export async function getCompanies(params: CompanyListParams = {}): Promise<Page<CompanyDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.name) searchParams.set("name", params.name);
  if (params.brevo !== undefined) searchParams.set("brevo", String(params.brevo));
  if (params.tagIds) {
    for (const id of params.tagIds) {
      searchParams.append("tagIds", id);
    }
  }

  const query = searchParams.toString();
  const url = `${baseUrl()}/api/companies${query ? `?${query}` : ""}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch companies: ${response.status}`);
  }

  return response.json();
}

export async function getCompany(id: string): Promise<CompanyDto> {
  const url = `${baseUrl()}/api/companies/${id}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error("Company not found");
    }
    throw new Error(`Failed to fetch company: ${response.status}`);
  }

  return response.json();
}

export async function createCompany(data: CompanyCreateDto): Promise<CompanyDto> {
  const url = `${baseUrl()}/api/companies`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create company: ${response.status}`);
  }

  return response.json();
}

export async function updateCompany(id: string, data: CompanyCreateDto): Promise<CompanyDto> {
  const url = `${baseUrl()}/api/companies/${id}`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to update company: ${response.status}`);
  }

  return response.json();
}

export async function deleteCompany(id: string, deleteContacts: boolean = false): Promise<void> {
  const url = `${baseUrl()}/api/companies/${id}${deleteContacts ? "?deleteContacts=true" : ""}`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (response.status === 403) {
    throw new ForbiddenError();
  }
  if (!response.ok) {
    throw new Error(`Failed to delete company: ${response.status}`);
  }
}

// Company Logo API

export async function uploadCompanyLogo(id: string, file: File): Promise<void> {
  const formData = new FormData();
  formData.append("file", file);
  const url = `${baseUrl()}/api/companies/${id}/logo`;
  const response = await apiFetch(url, { method: "POST", body: formData });

  if (!response.ok) {
    throw new Error(`Failed to upload logo: ${response.status}`);
  }
}

export function getCompanyLogoUrl(id: string): string {
  return `${baseUrl()}/api/companies/${id}/logo`;
}

export async function deleteCompanyLogo(id: string): Promise<void> {
  const url = `${baseUrl()}/api/companies/${id}/logo`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (response.status === 403) {
    throw new ForbiddenError();
  }
  if (!response.ok) {
    throw new Error(`Failed to delete logo: ${response.status}`);
  }
}

// Contact API

export interface ContactListParams {
  readonly page?: number;
  readonly size?: number;
  readonly search?: string;
  readonly companyId?: string;
  readonly noCompany?: boolean;
  readonly language?: string;
  readonly brevo?: boolean;
  readonly tagIds?: readonly string[];
}

export async function getContacts(params: ContactListParams = {}): Promise<Page<ContactDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.search) searchParams.set("search", params.search);
  if (params.companyId) searchParams.set("companyId", params.companyId);
  if (params.noCompany) searchParams.set("noCompany", "true");
  if (params.language) searchParams.set("language", params.language);
  if (params.brevo !== undefined) searchParams.set("brevo", String(params.brevo));
  if (params.tagIds) {
    for (const id of params.tagIds) {
      searchParams.append("tagIds", id);
    }
  }

  const query = searchParams.toString();
  const url = `${baseUrl()}/api/contacts${query ? `?${query}` : ""}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch contacts: ${response.status}`);
  }

  return response.json();
}

export async function getContact(id: string): Promise<ContactDto> {
  const url = `${baseUrl()}/api/contacts/${id}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error("Contact not found");
    }
    throw new Error(`Failed to fetch contact: ${response.status}`);
  }

  return response.json();
}

export async function createContact(data: ContactCreateDto): Promise<ContactDto> {
  const url = `${baseUrl()}/api/contacts`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create contact: ${response.status}`);
  }

  return response.json();
}

export async function updateContact(id: string, data: ContactCreateDto): Promise<ContactDto> {
  const url = `${baseUrl()}/api/contacts/${id}`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to update contact: ${response.status}`);
  }

  return response.json();
}

export async function deleteContact(id: string): Promise<void> {
  const url = `${baseUrl()}/api/contacts/${id}`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (response.status === 403) {
    throw new ForbiddenError();
  }
  if (!response.ok) {
    throw new Error(`Failed to delete contact: ${response.status}`);
  }
}

// Contact Photo API

export async function uploadContactPhoto(id: string, file: File): Promise<void> {
  const formData = new FormData();
  formData.append("file", file);
  const url = `${baseUrl()}/api/contacts/${id}/photo`;
  const response = await apiFetch(url, { method: "POST", body: formData });

  if (!response.ok) {
    throw new Error(`Failed to upload photo: ${response.status}`);
  }
}

export function getContactPhotoUrl(id: string): string {
  return `${baseUrl()}/api/contacts/${id}/photo`;
}

export async function deleteContactPhoto(id: string): Promise<void> {
  const url = `${baseUrl()}/api/contacts/${id}/photo`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (response.status === 403) {
    throw new ForbiddenError();
  }
  if (!response.ok) {
    throw new Error(`Failed to delete photo: ${response.status}`);
  }
}

export function getCompanyExportUrl(params: CompanyListParams, columns: string[]): string {
  const searchParams = new URLSearchParams();
  if (params.name) searchParams.set("name", params.name);
  if (params.brevo !== undefined) searchParams.set("brevo", String(params.brevo));
  if (params.tagIds) {
    for (const id of params.tagIds) {
      searchParams.append("tagIds", id);
    }
  }
  for (const col of columns) {
    searchParams.append("columns", col);
  }
  return `${baseUrl()}/api/companies/export?${searchParams.toString()}`;
}

export function getContactExportUrl(params: ContactListParams, columns: string[]): string {
  const searchParams = new URLSearchParams();
  if (params.search) searchParams.set("search", params.search);
  if (params.companyId) searchParams.set("companyId", params.companyId);
  if (params.noCompany) searchParams.set("noCompany", "true");
  if (params.language) searchParams.set("language", params.language);
  if (params.brevo !== undefined) searchParams.set("brevo", String(params.brevo));
  if (params.tagIds) {
    for (const id of params.tagIds) {
      searchParams.append("tagIds", id);
    }
  }
  for (const col of columns) {
    searchParams.append("columns", col);
  }
  return `${baseUrl()}/api/contacts/export?${searchParams.toString()}`;
}

export async function getCompaniesForSelect(): Promise<CompanyDto[]> {
  const data = await getCompanies({ size: 1000 });
  return data.content as CompanyDto[];
}

// Company Comment API

export async function getCompanyComments(companyId: string): Promise<CommentDto[]> {
  const url = `${baseUrl()}/api/companies/${companyId}/comments`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch comments: ${response.status}`);
  }

  return response.json();
}

export async function createCompanyComment(
  companyId: string,
  data: CommentCreateDto,
): Promise<CommentDto> {
  const url = `${baseUrl()}/api/companies/${companyId}/comments`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create comment: ${response.status}`);
  }

  return response.json();
}

export async function updateCompanyComment(
  companyId: string,
  commentId: string,
  data: CommentCreateDto,
): Promise<CommentDto> {
  const url = `${baseUrl()}/api/companies/${companyId}/comments/${commentId}`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to update comment: ${response.status}`);
  }

  return response.json();
}

export async function deleteCompanyComment(companyId: string, commentId: string): Promise<void> {
  const url = `${baseUrl()}/api/companies/${companyId}/comments/${commentId}`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (response.status === 404) {
    return;
  }
  if (response.status === 403) {
    throw new ForbiddenError();
  }
  if (!response.ok) {
    throw new Error(`Failed to delete comment: ${response.status}`);
  }
}

// Contact Comment API

export async function getContactComments(contactId: string): Promise<CommentDto[]> {
  const url = `${baseUrl()}/api/contacts/${contactId}/comments`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch contact comments: ${response.status}`);
  }

  return response.json();
}

export async function createContactComment(
  contactId: string,
  data: CommentCreateDto,
): Promise<CommentDto> {
  const url = `${baseUrl()}/api/contacts/${contactId}/comments`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create contact comment: ${response.status}`);
  }

  return response.json();
}

export async function updateContactComment(
  contactId: string,
  commentId: string,
  data: CommentCreateDto,
): Promise<CommentDto> {
  const url = `${baseUrl()}/api/contacts/${contactId}/comments/${commentId}`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to update contact comment: ${response.status}`);
  }

  return response.json();
}

export async function deleteContactComment(contactId: string, commentId: string): Promise<void> {
  const url = `${baseUrl()}/api/contacts/${contactId}/comments/${commentId}`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (response.status === 404) {
    return;
  }
  if (response.status === 403) {
    throw new ForbiddenError();
  }
  if (!response.ok) {
    throw new Error(`Failed to delete contact comment: ${response.status}`);
  }
}

// Brevo Sync API

export async function getBrevoSettings(): Promise<BrevoSettingsDto> {
  const url = `${baseUrl()}/api/brevo/settings`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch Brevo settings: ${response.status}`);
  }

  return response.json();
}

export async function updateBrevoSettings(apiKey: string): Promise<BrevoSettingsDto> {
  const url = `${baseUrl()}/api/brevo/settings`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ apiKey }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to update Brevo settings: ${response.status}`);
  }

  return response.json();
}

export async function deleteBrevoSettings(): Promise<void> {
  const url = `${baseUrl()}/api/brevo/settings`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete Brevo settings: ${response.status}`);
  }
}

export async function startBrevoSync(): Promise<BrevoSyncResultDto> {
  const url = `${baseUrl()}/api/brevo/sync`;
  const response = await apiFetch(url, { method: "POST" });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to start Brevo sync: ${response.status}`);
  }

  return response.json();
}

// Backup admin API (spec 107)

export async function getBackupStatus(): Promise<BackupStatusDto> {
  const url = `${baseUrl()}/api/admin/backup/status`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch backup status: ${response.status}`);
  }
  return response.json();
}

export async function triggerBackup(): Promise<BackupTriggerDto> {
  const url = `${baseUrl()}/api/admin/backup/trigger`;
  const response = await apiFetch(url, { method: "POST" });

  if (!response.ok) {
    throw new Error(`Failed to trigger backup: ${response.status}`);
  }
  return response.json();
}

export async function getBackups(): Promise<BackupItemDto[]> {
  const url = `${baseUrl()}/api/admin/backup/backups`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch backups: ${response.status}`);
  }
  return response.json();
}

export function backupDownloadUrl(id: string): string {
  return `/api/admin/backup/backups/${encodeURIComponent(id)}/download`;
}

// Tags

export interface TagListParams {
  readonly page?: number;
  readonly size?: number;
  readonly name?: string;
  readonly includeCounts?: boolean;
}

export async function getTags(params: TagListParams = {}): Promise<Page<TagDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.name) searchParams.set("name", params.name);
  if (params.includeCounts) searchParams.set("includeCounts", "true");
  searchParams.set("sort", "name,asc");

  const url = `${baseUrl()}/api/tags?${searchParams.toString()}`;
  const response = await apiFetch(url);

  if (!response.ok) {
    throw new Error(`Failed to fetch tags: ${response.status}`);
  }

  return response.json();
}

export async function getTag(id: string): Promise<TagDto> {
  const url = `${baseUrl()}/api/tags/${id}`;
  const response = await apiFetch(url);

  if (!response.ok) {
    throw new Error(`Failed to fetch tag: ${response.status}`);
  }

  return response.json();
}

export async function createTag(data: TagCreateDto): Promise<TagDto> {
  const url = `${baseUrl()}/api/tags`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (response.status === 409) {
    throw new Error("CONFLICT");
  }

  if (!response.ok) {
    throw new Error(`Failed to create tag: ${response.status}`);
  }

  return response.json();
}

export async function updateTag(id: string, data: TagCreateDto): Promise<TagDto> {
  const url = `${baseUrl()}/api/tags/${id}`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (response.status === 409) {
    throw new Error("CONFLICT");
  }

  if (!response.ok) {
    throw new Error(`Failed to update tag: ${response.status}`);
  }

  return response.json();
}

export async function deleteTag(id: string): Promise<void> {
  const url = `${baseUrl()}/api/tags/${id}`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (response.status === 403) {
    throw new ForbiddenError();
  }
  if (!response.ok) {
    throw new Error(`Failed to delete tag: ${response.status}`);
  }
}

// Tasks

export interface TaskListParams {
  readonly page?: number;
  readonly size?: number;
  readonly status?: TaskStatus;
  readonly tagIds?: readonly string[];
}

export async function getTasks(params: TaskListParams = {}): Promise<Page<TaskDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.status) searchParams.set("status", params.status);
  if (params.tagIds) {
    for (const id of params.tagIds) {
      searchParams.append("tagIds", id);
    }
  }

  const query = searchParams.toString();
  const url = `${baseUrl()}/api/tasks${query ? `?${query}` : ""}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch tasks: ${response.status}`);
  }

  return response.json();
}

export async function getTask(id: string): Promise<TaskDto> {
  const url = `${baseUrl()}/api/tasks/${id}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    if (response.status === 404) {
      throw new Error("Task not found");
    }
    throw new Error(`Failed to fetch task: ${response.status}`);
  }

  return response.json();
}

export async function createTask(data: TaskCreateDto): Promise<TaskDto> {
  const url = `${baseUrl()}/api/tasks`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create task: ${response.status}`);
  }

  return response.json();
}

export async function updateTask(id: string, data: TaskUpdateDto): Promise<TaskDto> {
  const url = `${baseUrl()}/api/tasks/${id}`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to update task: ${response.status}`);
  }

  return response.json();
}

// Task Comment API

export async function getTaskComments(taskId: string): Promise<CommentDto[]> {
  const url = `${baseUrl()}/api/tasks/${taskId}/comments`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch task comments: ${response.status}`);
  }

  return response.json();
}

export async function createTaskComment(
  taskId: string,
  data: CommentCreateDto,
): Promise<CommentDto> {
  const url = `${baseUrl()}/api/tasks/${taskId}/comments`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create task comment: ${response.status}`);
  }

  return response.json();
}

export async function updateTaskComment(
  taskId: string,
  commentId: string,
  data: CommentCreateDto,
): Promise<CommentDto> {
  const url = `${baseUrl()}/api/tasks/${taskId}/comments/${commentId}`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to update task comment: ${response.status}`);
  }

  return response.json();
}

export async function deleteTaskComment(taskId: string, commentId: string): Promise<void> {
  const url = `${baseUrl()}/api/tasks/${taskId}/comments/${commentId}`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (response.status === 404) {
    return;
  }
  if (response.status === 403) {
    throw new ForbiddenError();
  }
  if (!response.ok) {
    throw new Error(`Failed to delete task comment: ${response.status}`);
  }
}

export async function deleteTask(id: string): Promise<void> {
  const url = `${baseUrl()}/api/tasks/${id}`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (response.status === 403) {
    throw new ForbiddenError();
  }
  if (!response.ok) {
    throw new Error(`Failed to delete task: ${response.status}`);
  }
}

export async function getContactsForSelect(): Promise<ContactDto[]> {
  const data = await getContacts({ size: 1000 });
  return data.content as ContactDto[];
}

// User API

export async function getCurrentUser(): Promise<UserDto> {
  const url = `${baseUrl()}/api/users/me`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch current user: ${response.status}`);
  }

  return response.json();
}

export interface UserListParams {
  readonly page?: number;
  readonly size?: number;
}

export async function getUsers(
  params: UserListParams = {},
): Promise<Page<UserDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));

  const query = searchParams.toString();
  const url = `${baseUrl()}/api/users${query ? `?${query}` : ""}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch users: ${response.status}`);
  }

  return response.json();
}

// --- Audit Log ---

export interface AuditLogListParams {
  readonly page?: number;
  readonly size?: number;
  readonly entityType?: string;
  readonly user?: string;
}

export async function getAuditLogs(
  params: AuditLogListParams = {},
): Promise<Page<AuditLogDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  if (params.entityType) searchParams.set("entityType", params.entityType);
  if (params.user) searchParams.set("user", params.user);

  const query = searchParams.toString();
  const url = `${baseUrl()}/api/audit-logs${query ? `?${query}` : ""}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch audit logs: ${response.status}`);
  }

  return response.json();
}

export interface UpdatesListParams {
  readonly size?: number;
}

export async function getUpdates(
  params: UpdatesListParams = {},
): Promise<Page<UpdateEntryDto>> {
  const searchParams = new URLSearchParams();
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  const query = searchParams.toString();
  const url = `${baseUrl()}/api/updates${query ? `?${query}` : ""}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch updates: ${response.status}`);
  }

  return response.json();
}

export async function getAuditLogEntityTypes(): Promise<readonly string[]> {
  const url = `${baseUrl()}/api/audit-logs/entity-types`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch audit log entity types: ${response.status}`);
  }

  return response.json();
}


// --- Webhooks ---

export interface WebhookListParams {
  readonly page?: number;
  readonly size?: number;
}

export async function getWebhooks(
  params: WebhookListParams = {},
): Promise<Page<WebhookDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  searchParams.set("sort", "createdAt,desc");

  const url = `${baseUrl()}/api/webhooks?${searchParams.toString()}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch webhooks: ${response.status}`);
  }

  return response.json();
}

export async function createWebhook(
  data: WebhookCreateDto,
): Promise<WebhookDto> {
  const url = `${baseUrl()}/api/webhooks`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create webhook: ${response.status}`);
  }

  return response.json();
}

export async function updateWebhook(
  id: string,
  data: WebhookUpdateDto,
): Promise<WebhookDto> {
  const url = `${baseUrl()}/api/webhooks/${id}`;
  const response = await apiFetch(url, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to update webhook: ${response.status}`);
  }

  return response.json();
}

export async function deleteWebhook(id: string): Promise<void> {
  const url = `${baseUrl()}/api/webhooks/${id}`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete webhook: ${response.status}`);
  }
}

export async function pingWebhook(id: string): Promise<void> {
  const url = `${baseUrl()}/api/webhooks/${id}/ping`;
  const response = await apiFetch(url, { method: "POST" });

  if (!response.ok) {
    throw new Error(`Failed to ping webhook: ${response.status}`);
  }
}

// --- API Keys ---

export interface ApiKeyListParams {
  readonly page?: number;
  readonly size?: number;
}

export async function getApiKeys(
  params: ApiKeyListParams = {},
): Promise<Page<ApiKeyDto>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));
  searchParams.set("sort", "createdAt,desc");

  const url = `${baseUrl()}/api/api-keys?${searchParams.toString()}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch API keys: ${response.status}`);
  }

  return response.json();
}

export async function createApiKey(
  data: ApiKeyCreateDto,
): Promise<ApiKeyCreatedDto> {
  const url = `${baseUrl()}/api/api-keys`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create API key: ${response.status}`);
  }

  return response.json();
}

export async function deleteApiKey(id: string): Promise<void> {
  const url = `${baseUrl()}/api/api-keys/${id}`;
  const response = await apiFetch(url, { method: "DELETE" });

  if (!response.ok) {
    throw new Error(`Failed to delete API key: ${response.status}`);
  }
}

// Translation API

export async function getTranslationSettings(): Promise<TranslationConfigDto> {
  const url = `${baseUrl()}/api/translate/settings`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`Failed to fetch translation settings: ${response.status}`);
  }

  return response.json();
}

export async function translateText(
  text: string,
  targetLanguage: "de" | "en",
): Promise<TranslateResponseDto> {
  const url = `${baseUrl()}/api/translate`;
  const response = await apiFetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text, targetLanguage }),
  });

  if (!response.ok) {
    throw new Error(`Failed to translate: ${response.status}`);
  }

  return response.json();
}

// Global search API

export interface SearchHit {
  id: string;
  label: string;
  snippet: string | null;
  highlight: string | null;
  score: number;
  ownerType?: "company" | "contact" | "task" | null;
  ownerId?: string | null;
}

export interface GlobalSearchResult {
  query: string;
  companies: SearchHit[];
  contacts: SearchHit[];
  tags: SearchHit[];
  comments: SearchHit[];
}

export class SearchUnavailableError extends Error {
  readonly retryAfterSeconds: number;
  constructor(retryAfterSeconds: number) {
    super("Search is initializing");
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

export async function globalSearch(query: string, limit?: number): Promise<GlobalSearchResult> {
  const params = new URLSearchParams({ q: query });
  if (limit !== undefined) {
    params.set("limit", String(limit));
  }
  const url = `${baseUrl()}/api/search?${params.toString()}`;
  const response = await apiFetch(url, { cache: "no-store" });

  if (response.status === 503) {
    const header = response.headers.get("Retry-After");
    const retryAfter = header ? parseInt(header, 10) : 5;
    throw new SearchUnavailableError(Number.isFinite(retryAfter) ? retryAfter : 5);
  }
  if (!response.ok) {
    throw new Error(`Search failed: ${response.status}`);
  }
  return response.json();
}
