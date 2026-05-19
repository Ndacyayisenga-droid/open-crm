import { describe, it, expect, afterEach, vi } from "vitest";
import { cleanup, screen, waitFor } from "@testing-library/react";
import { UsersClient } from "../users-client";
import { renderWithLibProviders } from "../../../../test/render-with-providers";
import { appLayerTranslations } from "../../../../translations/provider";
import type { AppLayerApiClient } from "../../../../api/client";
import type { Page, UserDto } from "../../../../api/types";

function makePage(items: UserDto[]): Page<UserDto> {
  return {
    content: items,
    page: { size: 20, number: 0, totalElements: items.length, totalPages: 1 },
  };
}

function makeClient(overrides: Partial<AppLayerApiClient> = {}): AppLayerApiClient {
  return {
    getAuditLogs: vi.fn(),
    getAuditLogEntityTypes: vi.fn(),
    getUsers: vi.fn().mockResolvedValue(makePage([])),
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
}

afterEach(() => cleanup());

describe("UsersClient", () => {
  it("shows the skeleton while loading", () => {
    const client = makeClient({ getUsers: vi.fn(() => new Promise(() => {})) });
    renderWithLibProviders(<UsersClient />, { apiClient: client });
    expect(screen.getByTestId("users-loading")).toBeInTheDocument();
  });

  it("renders one row per user with an avatar fallback for missing avatarUrl", async () => {
    const users: UserDto[] = [
      {
        id: "u-1",
        name: "Alice",
        email: "alice@example.com",
        avatarUrl: null,
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-01-01T00:00:00Z",
      },
    ];
    const client = makeClient({ getUsers: vi.fn().mockResolvedValue(makePage(users)) });
    renderWithLibProviders(<UsersClient />, { apiClient: client, language: "en" });
    await waitFor(() => {
      expect(screen.getByText("Alice")).toBeInTheDocument();
    });
    expect(screen.getByTestId("user-avatar-fallback")).toBeInTheDocument();
  });

  it("shows the error state when fetch rejects", async () => {
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const client = makeClient({ getUsers: vi.fn().mockRejectedValue(new Error("boom")) });
    renderWithLibProviders(<UsersClient />, { apiClient: client, language: "en" });
    await waitFor(() => {
      expect(screen.getByTestId("users-error")).toBeInTheDocument();
    });
    expect(
      screen.getByText(appLayerTranslations.en.users.loadError),
    ).toBeInTheDocument();
    consoleError.mockRestore();
  });
});
