import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, screen, waitFor } from "@testing-library/react";

// vi.hoisted runs before vi.mock so the factory can refer to the declared
// values. We re-declare SearchUnavailableError locally to avoid loading
// @/lib/api (and transitively next-auth) into the test bundle.
const hoisted = vi.hoisted(() => {
  class SearchUnavailableError extends Error {
    readonly retryAfterSeconds: number;
    constructor(retryAfterSeconds: number) {
      super("Search is initializing");
      this.retryAfterSeconds = retryAfterSeconds;
    }
  }
  return {
    SearchUnavailableError,
    mockGlobalSearch: vi.fn(),
  };
});

vi.mock("@/lib/api", () => ({
  globalSearch: (...args: unknown[]) => hoisted.mockGlobalSearch(...args),
  SearchUnavailableError: hoisted.SearchUnavailableError,
}));

const mockGlobalSearch = hoisted.mockGlobalSearch;
const SearchUnavailableError = hoisted.SearchUnavailableError;

import { SearchClient } from "../search-client";
import { de } from "@/lib/i18n/de";
import { en } from "@/lib/i18n/en";
import { renderWithProviders } from "@/test/test-utils";

afterEach(() => {
  cleanup();
  mockGlobalSearch.mockReset();
});

describe("SearchClient", () => {
  beforeEach(() => {
    mockGlobalSearch.mockResolvedValue({
      query: "",
      companies: [],
      contacts: [],
      tags: [],
      comments: [],
    });
  });

  it("auto-focuses the search input on mount", () => {
    renderWithProviders(<SearchClient />);
    const input = screen.getByPlaceholderText(de.search.inputPlaceholder);
    expect(document.activeElement).toBe(input);
  });

  it("shows the min-chars hint while query is shorter than 2 characters", () => {
    renderWithProviders(<SearchClient />);
    expect(screen.getByText(de.search.minChars)).toBeInTheDocument();
  });

  it("does not call the API for a single-character query", async () => {
    renderWithProviders(<SearchClient />);
    const input = screen.getByPlaceholderText(de.search.inputPlaceholder);
    fireEvent.change(input, { target: { value: "a" } });

    // Wait past the debounce window.
    await new Promise((r) => setTimeout(r, 400));
    expect(mockGlobalSearch).not.toHaveBeenCalled();
  });

  it("debounces by ~300 ms and fires exactly one request for a multi-keystroke query", async () => {
    renderWithProviders(<SearchClient />);
    const input = screen.getByPlaceholderText(de.search.inputPlaceholder);

    fireEvent.change(input, { target: { value: "h" } });
    fireEvent.change(input, { target: { value: "he" } });
    fireEvent.change(input, { target: { value: "hen" } });
    fireEvent.change(input, { target: { value: "hendrik" } });

    await waitFor(
      () => {
        expect(mockGlobalSearch).toHaveBeenCalledTimes(1);
      },
      { timeout: 1000 },
    );
    expect(mockGlobalSearch).toHaveBeenCalledWith("hendrik");
  });

  it("renders sectioned hits returned by the API", async () => {
    mockGlobalSearch.mockResolvedValue({
      query: "hendrik",
      companies: [
        { id: "c1", label: "Open Elements GmbH", snippet: "info@example.com", highlight: "<em>Open</em> Elements GmbH", score: 0.5 },
      ],
      contacts: [
        { id: "ct1", label: "Hendrik Ebbers", snippet: "hendrik@example.com", highlight: "<em>Hendrik</em> Ebbers", score: 0.9 },
      ],
      tags: [],
      comments: [],
    });

    renderWithProviders(<SearchClient />);
    fireEvent.change(screen.getByPlaceholderText(de.search.inputPlaceholder), { target: { value: "hendrik" } });

    await waitFor(
      () => {
        expect(screen.getByText("info@example.com")).toBeInTheDocument();
        expect(screen.getByText("hendrik@example.com")).toBeInTheDocument();
      },
      { timeout: 2000 },
    );
    // The "(1)" count indicator confirms the section header rendered.
    expect(screen.getAllByText("(1)").length).toBeGreaterThanOrEqual(2);
  });

  it("shows the empty-results message when every section is empty", async () => {
    renderWithProviders(<SearchClient />);
    fireEvent.change(screen.getByPlaceholderText(de.search.inputPlaceholder), { target: { value: "nothing" } });

    await waitFor(() => {
      expect(screen.getByText(de.search.emptyResults)).toBeInTheDocument();
    });
    // Section headers should not render for empty sections.
    expect(screen.queryByText(de.search.sectionCompanies)).not.toBeInTheDocument();
  });

  it("shows the bootstrap banner on 503 and auto-retries", async () => {
    mockGlobalSearch
      .mockRejectedValueOnce(new SearchUnavailableError(1))
      .mockResolvedValueOnce({
        query: "ready",
        companies: [],
        contacts: [],
        tags: [],
        comments: [],
      });

    renderWithProviders(<SearchClient />);
    fireEvent.change(screen.getByPlaceholderText(de.search.inputPlaceholder), { target: { value: "ready" } });

    await waitFor(() => {
      expect(screen.getByText(de.search.bootstrapBanner)).toBeInTheDocument();
    });
    await waitFor(
      () => {
        expect(screen.getByText(de.search.emptyResults)).toBeInTheDocument();
      },
      { timeout: 3000 },
    );
  });

  it("shows the error banner on non-503 5xx", async () => {
    mockGlobalSearch.mockRejectedValue(new Error("Search failed: 500"));

    renderWithProviders(<SearchClient />);
    fireEvent.change(screen.getByPlaceholderText(de.search.inputPlaceholder), { target: { value: "boom" } });

    await waitFor(() => {
      expect(screen.getByText(de.search.errorBanner)).toBeInTheDocument();
    });
  });

  it("German nav and search copy are in German", () => {
    expect(de.nav.search).toBe("Suche");
    expect(de.search.title).toBe("Suche");
    expect(de.search.emptyResults).toMatch(/Keine/);
  });

  it("English nav and search copy are in English", () => {
    expect(en.nav.search).toBe("Search");
    expect(en.search.title).toBe("Search");
    expect(en.search.emptyResults).toMatch(/No results/);
  });
});
