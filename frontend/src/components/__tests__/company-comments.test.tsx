import { describe, it, expect, afterEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { CompanyComments } from "@/components/company-comments";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { CommentDto } from "@/lib/types";

const S = de.companies.comments;

const mockGetCompanyComments = vi.fn();
const mockCreateCompanyComment = vi.fn();

vi.mock("@/lib/api", () => ({
  getCompanyComments: (...args: unknown[]) => mockGetCompanyComments(...args),
  createCompanyComment: (...args: unknown[]) => mockCreateCompanyComment(...args),
  deleteCompanyComment: vi.fn(),
  getTranslationSettings: vi.fn().mockResolvedValue({ configured: false }),
}));

vi.mock("@open-elements/ui", async () => {
  const actual = await vi.importActual<typeof import("@open-elements/ui")>("@open-elements/ui");
  return {
    ...actual,
    MarkdownEditor: ({
      value,
      onChange,
      placeholder,
    }: {
      readonly value: string;
      readonly onChange: (v: string) => void;
      readonly placeholder?: string;
    }) => (
      <textarea
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    ),
  };
});

function makeComment(overrides: Partial<CommentDto> = {}): CommentDto {
  return {
    id: "comment-1",
    text: "Test comment",
    author: {
      id: "user-1",
      name: "Tester",
      email: "tester@example.com",
      avatarUrl: null,
      createdAt: "2026-03-01T00:00:00Z",
      updatedAt: "2026-03-01T00:00:00Z",
    },
    createdAt: "2026-03-27T15:30:00Z",
    updatedAt: "2026-03-27T15:30:00Z",
    ...overrides,
  };
}

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("CompanyComments", () => {
  describe("display", () => {
    it("should render comments with author, date, and text", async () => {
      mockGetCompanyComments.mockResolvedValue([
        makeComment({ id: "1", text: "First comment" }),
        makeComment({ id: "2", text: "Second comment" }),
      ]);

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText("First comment")).toBeInTheDocument();
        expect(screen.getByText("Second comment")).toBeInTheDocument();
        expect(screen.getAllByText(/Tester/).length).toBeGreaterThanOrEqual(2);
      });
    });

    it("should show empty state when no comments", async () => {
      mockGetCompanyComments.mockResolvedValue([]);

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText(S.empty)).toBeInTheDocument();
      });
    });

    it("should show loading skeleton while fetching", () => {
      mockGetCompanyComments.mockReturnValue(new Promise(() => {}));

      const { container } = renderWithProviders(<CompanyComments companyId="company-1" />);

      const skeletons = container.querySelectorAll("[data-slot='skeleton']");
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it("should format dates in readable format", async () => {
      mockGetCompanyComments.mockResolvedValue([
        makeComment({ createdAt: "2026-03-27T15:30:00Z" }),
      ]);

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        const dateText = screen.getByText(/27/);
        expect(dateText).toBeInTheDocument();
      });
    });

    it("should render — when author is null (legacy SYSTEM-USER fallback)", async () => {
      mockGetCompanyComments.mockResolvedValue([
        makeComment({ id: "1", text: "Legacy", author: null }),
      ]);

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText("Legacy")).toBeInTheDocument();
        expect(screen.getByText(/—/)).toBeInTheDocument();
      });
    });
  });

  describe("add comment button", () => {
    it("should show Add Comment button in header", async () => {
      mockGetCompanyComments.mockResolvedValue([]);

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText(S.add)).toBeInTheDocument();
      });
    });

    it("should not show inline textarea", async () => {
      mockGetCompanyComments.mockResolvedValue([]);

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText(S.empty)).toBeInTheDocument();
      });

      expect(screen.queryByPlaceholderText(S.placeholder)).not.toBeInTheDocument();
    });

    it("should open dialog when Add Comment button is clicked", async () => {
      mockGetCompanyComments.mockResolvedValue([]);

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText(S.add)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.add));

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
        expect(screen.getAllByText(S.addTitle).length).toBeGreaterThanOrEqual(1);
      });
    });
  });

  describe("modal create flow", () => {
    it("should disable send button when text is empty", async () => {
      mockGetCompanyComments.mockResolvedValue([]);

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText(S.add)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.add));

      await waitFor(() => {
        const sendButton = screen.getByText(S.send);
        expect(sendButton.closest("button")).toBeDisabled();
      });
    });

    it("should create comment, close dialog, and add to top of list", async () => {
      mockGetCompanyComments.mockResolvedValue([
        makeComment({ id: "1", text: "Existing comment" }),
      ]);
      mockCreateCompanyComment.mockResolvedValue(
        makeComment({ id: "2", text: "New comment" }),
      );

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText("Existing comment")).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.add));

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "New comment" },
      });

      fireEvent.click(screen.getByText(S.send));

      await waitFor(() => {
        expect(mockCreateCompanyComment).toHaveBeenCalledWith("company-1", { text: "New comment" });
        expect(screen.getByText("New comment")).toBeInTheDocument();
      });
    });

    it("should show error dialog on API failure and preserve text", async () => {
      mockGetCompanyComments.mockResolvedValue([]);
      mockCreateCompanyComment.mockRejectedValue(new Error("Server error"));

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText(S.add)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.add));

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "Will fail" },
      });

      fireEvent.click(screen.getByText(S.send));

      await waitFor(() => {
        expect(screen.getByText(S.errorGeneric)).toBeInTheDocument();
      });

      const textarea = screen.getByPlaceholderText(S.placeholder) as HTMLTextAreaElement;
      expect(textarea.value).toBe("Will fail");
    });
  });

  describe("comment count live update", () => {
    it("should show totalCount in heading", async () => {
      mockGetCompanyComments.mockResolvedValue([]);

      renderWithProviders(<CompanyComments companyId="company-1" totalCount={3} />);

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (3)`)).toBeInTheDocument();
      });
    });

    it("should increment count after adding a comment", async () => {
      mockGetCompanyComments.mockResolvedValue([]);
      mockCreateCompanyComment.mockResolvedValue(
        makeComment({ id: "new", text: "New comment" }),
      );

      renderWithProviders(<CompanyComments companyId="company-1" totalCount={3} />);

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (3)`)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.add));

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "New comment" },
      });
      fireEvent.click(screen.getByText(S.send));

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (4)`)).toBeInTheDocument();
      });
    });

    it("should not increment count on API failure", async () => {
      mockGetCompanyComments.mockResolvedValue([]);
      mockCreateCompanyComment.mockRejectedValue(new Error("Server error"));

      renderWithProviders(<CompanyComments companyId="company-1" totalCount={3} />);

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (3)`)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.add));

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "Will fail" },
      });
      fireEvent.click(screen.getByText(S.send));

      await waitFor(() => {
        expect(screen.getByText(S.errorGeneric)).toBeInTheDocument();
      });

      expect(screen.getByText(`${S.title} (3)`)).toBeInTheDocument();
    });

    it("should not show count when totalCount is undefined", async () => {
      mockGetCompanyComments.mockResolvedValue([]);
      mockCreateCompanyComment.mockResolvedValue(
        makeComment({ id: "new", text: "New comment" }),
      );

      renderWithProviders(<CompanyComments companyId="company-1" />);

      await waitFor(() => {
        expect(screen.getByText(S.title)).toBeInTheDocument();
      });

      expect(screen.queryByText(/\(\d+\)/)).not.toBeInTheDocument();

      fireEvent.click(screen.getByText(S.add));

      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });

      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "New comment" },
      });
      fireEvent.click(screen.getByText(S.send));

      await waitFor(() => {
        expect(screen.getByText("New comment")).toBeInTheDocument();
      });

      expect(screen.queryByText(/\(\d+\)/)).not.toBeInTheDocument();
    });

    it("should reset count when totalCount prop changes", async () => {
      mockGetCompanyComments.mockResolvedValue([]);
      mockCreateCompanyComment.mockResolvedValue(
        makeComment({ id: "new", text: "New comment" }),
      );

      const { rerender } = renderWithProviders(
        <CompanyComments companyId="company-1" totalCount={3} />,
      );

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (3)`)).toBeInTheDocument();
      });

      fireEvent.click(screen.getByText(S.add));
      await waitFor(() => {
        expect(screen.getByPlaceholderText(S.placeholder)).toBeInTheDocument();
      });
      fireEvent.change(screen.getByPlaceholderText(S.placeholder), {
        target: { value: "New comment" },
      });
      fireEvent.click(screen.getByText(S.send));
      await waitFor(() => {
        expect(screen.getByText(`${S.title} (4)`)).toBeInTheDocument();
      });

      rerender(
        <CompanyComments companyId="company-2" totalCount={1} />,
      );

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (1)`)).toBeInTheDocument();
      });
    });
  });
});
