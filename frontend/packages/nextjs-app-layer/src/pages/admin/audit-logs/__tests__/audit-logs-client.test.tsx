import { describe, it, expect, afterEach, vi } from "vitest";
import { cleanup, screen, waitFor } from "@testing-library/react";
import { AuditLogsClient } from "../audit-logs-client";
import { renderWithLibProviders } from "../../../../test/render-with-providers";
import { appLayerTranslations } from "../../../../translations/provider";
import type { AppLayerApiClient } from "../../../../api/client";
import type { AuditLogDto, Page, UserDto } from "../../../../api/types";

function emptyPage<T>(): Page<T> {
  return {
    content: [],
    page: { size: 20, number: 0, totalElements: 0, totalPages: 0 },
  };
}

function makeClient(overrides: Partial<AppLayerApiClient> = {}): AppLayerApiClient {
  const stub = {
    getAuditLogs: vi.fn().mockResolvedValue(emptyPage<AuditLogDto>()),
    getAuditLogEntityTypes: vi.fn().mockResolvedValue([]),
    getUsers: vi.fn().mockResolvedValue(emptyPage<UserDto>()),
    getApiKeys: vi.fn(),
    createApiKey: vi.fn(),
    deleteApiKey: vi.fn(),
    getWebhooks: vi.fn(),
    createWebhook: vi.fn(),
    updateWebhook: vi.fn(),
    deleteWebhook: vi.fn(),
    pingWebhook: vi.fn(),
    getTranslationSettings: vi.fn(),
    getCurrentUser: vi.fn(),
    ...overrides,
  } as unknown as AppLayerApiClient;
  return stub;
}

afterEach(() => cleanup());

describe("AuditLogsClient", () => {
  it("shows the skeleton while loading", () => {
    const client = makeClient({ getAuditLogs: vi.fn(() => new Promise(() => {})) });
    renderWithLibProviders(<AuditLogsClient />, { apiClient: client });
    expect(screen.getByTestId("audit-logs-loading")).toBeInTheDocument();
  });

  it("shows the empty state when the API returns no entries", async () => {
    const client = makeClient();
    renderWithLibProviders(<AuditLogsClient />, { apiClient: client, language: "en" });
    await waitFor(() => {
      expect(screen.getByTestId("audit-logs-empty")).toBeInTheDocument();
    });
    expect(
      screen.getByText(appLayerTranslations.en.auditLog.empty),
    ).toBeInTheDocument();
  });

  it("shows the error state when the fetch rejects", async () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const client = makeClient({
      getAuditLogs: vi.fn().mockRejectedValue(new Error("boom")),
    });
    renderWithLibProviders(<AuditLogsClient />, { apiClient: client, language: "en" });
    await waitFor(() => {
      expect(screen.getByTestId("audit-logs-error")).toBeInTheDocument();
    });
    expect(
      screen.getByText(appLayerTranslations.en.auditLog.loadError),
    ).toBeInTheDocument();
    consoleError.mockRestore();
  });
});
