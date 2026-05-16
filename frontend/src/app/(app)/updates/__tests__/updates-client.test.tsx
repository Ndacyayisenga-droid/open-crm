import { describe, it, expect, afterEach, beforeEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor, render, within } from "@testing-library/react";
import { LanguageProvider, TooltipProvider } from "@open-elements/ui";
import {
  UpdatesClient,
  PAGE_SIZE_OPTIONS,
  DEFAULT_PAGE_SIZE,
  PAGE_SIZE_STORAGE_KEY,
} from "../updates-client";
import { de } from "@/lib/i18n/de";
import { en } from "@/lib/i18n/en";
import { translations } from "@/lib/i18n";
import type { Page, UpdateEntryDto, UserDto } from "@/lib/types";

const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: vi.fn((key: string) => store[key] ?? null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value;
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key];
    }),
    clear: vi.fn(() => {
      store = {};
    }),
  };
})();
Object.defineProperty(globalThis, "localStorage", { value: localStorageMock });

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  usePathname: () => "/updates",
  useSearchParams: () => new URLSearchParams(),
}));

const mockGetUpdates = vi.fn();

vi.mock("@/lib/api", () => ({
  getUpdates: (...args: unknown[]) => mockGetUpdates(...args),
}));

function makeUser(overrides: Partial<UserDto> = {}): UserDto {
  return {
    id: "u-1",
    name: "Alice",
    email: "alice@example.com",
    avatarUrl: null,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

function makeEntry(overrides: Partial<UpdateEntryDto> = {}): UpdateEntryDto {
  return {
    id: "log-1",
    type: "COMPANY_CREATED",
    entityId: "550e8400-e29b-41d4-a716-446655440000",
    entityName: "Open Elements GmbH",
    user: makeUser(),
    createdAt: "2026-04-25T14:30:00Z",
    ...overrides,
  };
}

function makePage<T>(items: T[], size = 20): Page<T> {
  return {
    content: items,
    page: { size, number: 0, totalElements: items.length, totalPages: 1 },
  };
}

function renderClient(lang: "de" | "en" = "de") {
  return render(
    <LanguageProvider translations={translations} defaultLanguage={lang}>
      <TooltipProvider>
        <UpdatesClient />
      </TooltipProvider>
    </LanguageProvider>,
  );
}

beforeEach(() => {
  localStorageMock.clear();
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("UpdatesClient", () => {
  describe("constants", () => {
    it("exposes page-size options 20/50/100/200", () => {
      expect(Array.from(PAGE_SIZE_OPTIONS)).toEqual([20, 50, 100, 200]);
    });

    it("defaults to 20 items per page", () => {
      expect(DEFAULT_PAGE_SIZE).toBe(20);
    });

    it("persists user-selected page size under updates.pageSize", () => {
      expect(PAGE_SIZE_STORAGE_KEY).toBe("updates.pageSize");
    });
  });

  describe("loading / empty / error", () => {
    it("renders a skeleton while loading", () => {
      mockGetUpdates.mockReturnValue(new Promise(() => {}));
      renderClient();
      expect(screen.getByTestId("updates-loading")).toBeInTheDocument();
    });

    it("renders the empty state when content is empty", async () => {
      mockGetUpdates.mockResolvedValue(makePage<UpdateEntryDto>([]));
      renderClient("de");
      await waitFor(() => {
        expect(screen.getByTestId("updates-empty")).toBeInTheDocument();
        expect(screen.getByText(de.updates.empty)).toBeInTheDocument();
      });
    });

    it("renders the empty state with English copy when language is en", async () => {
      mockGetUpdates.mockResolvedValue(makePage<UpdateEntryDto>([]));
      renderClient("en");
      await waitFor(() => {
        expect(screen.getByText(en.updates.empty)).toBeInTheDocument();
      });
    });

    it("renders the error state when the fetch rejects", async () => {
      const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
      mockGetUpdates.mockRejectedValue(new Error("boom"));
      renderClient();
      await waitFor(() => {
        expect(screen.getByTestId("updates-error")).toBeInTheDocument();
        expect(screen.getByText(de.updates.loadError)).toBeInTheDocument();
      });
      expect(screen.queryByTestId("updates-empty")).toBeNull();
      consoleError.mockRestore();
    });
  });

  describe("rendering rows with links", () => {
    it("renders COMPANY_UPDATED as a link to /companies/{id}", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_UPDATED",
            entityId: "c-1",
            entityName: "Acme",
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const link = within(row).getByRole("link", { name: "Acme" });
      expect(link).toHaveAttribute("href", "/companies/c-1");
    });

    it("renders CONTACT_CREATED as a link to /contacts/{id}", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "CONTACT_CREATED",
            entityId: "p-1",
            entityName: "John Doe",
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const link = within(row).getByRole("link", { name: "John Doe" });
      expect(link).toHaveAttribute("href", "/contacts/p-1");
    });

    it("renders COMPANY_COMMENT_CREATED with link to parent company", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_COMMENT_CREATED",
            entityId: "c-9",
            entityName: "Open Elements GmbH",
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const link = within(row).getByRole("link", { name: "Open Elements GmbH" });
      expect(link).toHaveAttribute("href", "/companies/c-9");
    });

    it("renders CONTACT_COMMENT_CREATED with link to parent contact", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "CONTACT_COMMENT_CREATED",
            entityId: "p-9",
            entityName: "Jane Doe",
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const link = within(row).getByRole("link", { name: "Jane Doe" });
      expect(link).toHaveAttribute("href", "/contacts/p-9");
    });

    it("renders COMPANY_DELETED as plain text without a link or name", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({ type: "COMPANY_DELETED", entityId: null, entityName: null }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).queryByRole("link")).toBeNull();
      expect(within(row).getByText(/A company was deleted/)).toBeInTheDocument();
    });

    it("renders CONTACT_DELETED as plain text without a link or name", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({ type: "CONTACT_DELETED", entityId: null, entityName: null }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).queryByRole("link")).toBeNull();
      expect(within(row).getByText(/A person was deleted/)).toBeInTheDocument();
    });

    it("still renders a link with a placeholder name when entityName is null but entityId is present", async () => {
      // Per design.md: `entityId` is preserved for diagnosis even when the entity name cannot be
      // resolved (race / unexpected state). The link is kept; the visible label falls back to a dash.
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({
            type: "COMPANY_UPDATED",
            entityId: "c-1",
            entityName: null,
          }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      const link = within(row).getByRole("link");
      expect(link).toHaveAttribute("href", "/companies/c-1");
    });

    it("renders the actor name", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({ user: makeUser({ id: "u-2", name: "Bob" }) }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).getByText(/by Bob/)).toBeInTheDocument();
    });
  });

  describe("page-size selector", () => {
    it("requests size=20 on first load when localStorage is empty", async () => {
      mockGetUpdates.mockResolvedValue(makePage([makeEntry()]));
      renderClient();
      await waitFor(() => {
        expect(mockGetUpdates).toHaveBeenCalledWith({ size: 20 });
      });
    });

    it("reads page size from localStorage on mount", async () => {
      localStorageMock.setItem("updates.pageSize", "100");
      mockGetUpdates.mockResolvedValue(makePage([makeEntry()], 100));
      renderClient();
      await waitFor(() => {
        expect(mockGetUpdates).toHaveBeenCalledWith({ size: 100 });
      });
    });

    it("ignores out-of-range stored values and falls back to default", async () => {
      localStorageMock.setItem("updates.pageSize", "999");
      mockGetUpdates.mockResolvedValue(makePage([makeEntry()]));
      renderClient();
      await waitFor(() => {
        expect(mockGetUpdates).toHaveBeenCalledWith({ size: 20 });
      });
    });
  });

  describe("auto-refresh", () => {
    it("does not refetch after the initial load when no input changes", async () => {
      mockGetUpdates.mockResolvedValue(makePage([makeEntry()]));
      renderClient();
      await waitFor(() => {
        expect(mockGetUpdates).toHaveBeenCalledTimes(1);
      });
      // wait a tick — no polling
      await new Promise((r) => setTimeout(r, 30));
      expect(mockGetUpdates).toHaveBeenCalledTimes(1);
    });
  });

  describe("i18n", () => {
    it("renders German company-created text by default", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({ type: "COMPANY_CREATED", entityName: "Acme" }),
        ]),
      );
      renderClient("de");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).getByText(/Neue Firma/)).toBeInTheDocument();
      expect(within(row).getByText(/wurde angelegt/)).toBeInTheDocument();
    });

    it("renders English company-created text when language is en", async () => {
      mockGetUpdates.mockResolvedValue(
        makePage([
          makeEntry({ type: "COMPANY_CREATED", entityName: "Acme" }),
        ]),
      );
      renderClient("en");
      const row = await screen.findByTestId("updates-row");
      expect(within(row).getByText(/New company/)).toBeInTheDocument();
      expect(within(row).getByText(/was created/)).toBeInTheDocument();
    });
  });
});
