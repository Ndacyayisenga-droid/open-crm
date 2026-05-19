import type {
  AuditLogDto,
  ApiKeyDto,
  ApiKeyCreatedDto,
  Page,
  PageRequest,
  TranslationConfigDto,
  UserDto,
  WebhookDto,
} from "./types";

export interface AppLayerApiClient {
  readonly getAuditLogs: (params: PageRequest & {
    readonly entityType?: string;
    readonly user?: string;
  }) => Promise<Page<AuditLogDto>>;
  readonly getAuditLogEntityTypes: () => Promise<readonly string[]>;
  readonly getUsers: (params: PageRequest) => Promise<Page<UserDto>>;
  readonly getApiKeys: (params: PageRequest) => Promise<Page<ApiKeyDto>>;
  readonly createApiKey: (input: { readonly name: string }) => Promise<ApiKeyCreatedDto>;
  readonly deleteApiKey: (id: string) => Promise<void>;
  readonly getWebhooks: (params: PageRequest) => Promise<Page<WebhookDto>>;
  readonly createWebhook: (input: { readonly url: string }) => Promise<void>;
  readonly updateWebhook: (id: string, input: {
    readonly url: string;
    readonly active: boolean;
  }) => Promise<void>;
  readonly deleteWebhook: (id: string) => Promise<void>;
  readonly pingWebhook: (id: string) => Promise<void>;
  readonly getTranslationSettings: () => Promise<TranslationConfigDto>;
  readonly getCurrentUser: () => Promise<UserDto>;
}

async function jsonFetch<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, { cache: "no-store", ...init });
  if (response.status === 401 && typeof window !== "undefined") {
    window.location.href = "/api/logout";
  }
  if (!response.ok) {
    const text = await response.text().catch(() => "");
    throw new Error(text || `Request failed: ${response.status}`);
  }
  if (response.status === 204) return undefined as T;
  return response.json();
}

function appendPagination(p: URLSearchParams, params: PageRequest) {
  p.set("page", String(params.page));
  p.set("size", String(params.size));
}

/**
 * Default API client that calls the OE proxy paths (`/api/...`).
 * Apps that don't use the proxy pattern can provide their own implementation
 * of the full `AppLayerApiClient` interface to `<ApiClientProvider client={...}>`.
 */
export const defaultApiClient: AppLayerApiClient = {
  async getAuditLogs(params) {
    const p = new URLSearchParams();
    appendPagination(p, params);
    if (params.entityType) p.set("entityType", params.entityType);
    if (params.user) p.set("user", params.user);
    return jsonFetch(`/api/audit-logs?${p.toString()}`);
  },
  async getAuditLogEntityTypes() {
    return jsonFetch(`/api/audit-logs/entity-types`);
  },
  async getUsers(params) {
    const p = new URLSearchParams();
    appendPagination(p, params);
    return jsonFetch(`/api/users?${p.toString()}`);
  },
  async getApiKeys(params) {
    const p = new URLSearchParams();
    appendPagination(p, params);
    p.set("sort", "createdAt,desc");
    return jsonFetch(`/api/api-keys?${p.toString()}`);
  },
  async createApiKey(input) {
    return jsonFetch(`/api/api-keys`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    });
  },
  async deleteApiKey(id) {
    await jsonFetch<void>(`/api/api-keys/${id}`, { method: "DELETE" });
  },
  async getWebhooks(params) {
    const p = new URLSearchParams();
    appendPagination(p, params);
    p.set("sort", "createdAt,desc");
    return jsonFetch(`/api/webhooks?${p.toString()}`);
  },
  async createWebhook(input) {
    await jsonFetch<void>(`/api/webhooks`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    });
  },
  async updateWebhook(id, input) {
    await jsonFetch<void>(`/api/webhooks/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    });
  },
  async deleteWebhook(id) {
    await jsonFetch<void>(`/api/webhooks/${id}`, { method: "DELETE" });
  },
  async pingWebhook(id) {
    await jsonFetch<void>(`/api/webhooks/${id}/ping`, { method: "POST" });
  },
  async getTranslationSettings() {
    return jsonFetch(`/api/translate/settings`);
  },
  async getCurrentUser() {
    return jsonFetch(`/api/users/me`);
  },
};
