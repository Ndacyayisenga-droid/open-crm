import { describe, it, expect, afterEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { ContactComments } from "@/components/contact-comments";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { CommentDto } from "@/lib/types";

const S = de.companies.comments;

const mockGetContactComments = vi.fn();
const mockCreateContactComment = vi.fn();

vi.mock("@/lib/api", () => ({
  getContactComments: (...args: unknown[]) => mockGetContactComments(...args),
  createContactComment: (...args: unknown[]) => mockCreateContactComment(...args),
  deleteContactComment: vi.fn(),
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

describe("ContactComments", () => {
  describe("comment count live update", () => {
    it("should show totalCount in heading", async () => {
      mockGetContactComments.mockResolvedValue([]);

      renderWithProviders(<ContactComments contactId="contact-1" totalCount={2} />);

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (2)`)).toBeInTheDocument();
      });
    });

    it("should increment count after adding a comment", async () => {
      mockGetContactComments.mockResolvedValue([]);
      mockCreateContactComment.mockResolvedValue(
        makeComment({ id: "new", text: "New comment" }),
      );

      renderWithProviders(<ContactComments contactId="contact-1" totalCount={2} />);

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (2)`)).toBeInTheDocument();
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
        expect(screen.getByText(`${S.title} (3)`)).toBeInTheDocument();
      });
    });

    it("should not increment count on API failure", async () => {
      mockGetContactComments.mockResolvedValue([]);
      mockCreateContactComment.mockRejectedValue(new Error("Server error"));

      renderWithProviders(<ContactComments contactId="contact-1" totalCount={2} />);

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (2)`)).toBeInTheDocument();
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

      expect(screen.getByText(`${S.title} (2)`)).toBeInTheDocument();
    });

    it("should not show count when totalCount is undefined", async () => {
      mockGetContactComments.mockResolvedValue([]);
      mockCreateContactComment.mockResolvedValue(
        makeComment({ id: "new", text: "New comment" }),
      );

      renderWithProviders(<ContactComments contactId="contact-1" />);

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
      mockGetContactComments.mockResolvedValue([]);
      mockCreateContactComment.mockResolvedValue(
        makeComment({ id: "new", text: "New comment" }),
      );

      const { rerender } = renderWithProviders(
        <ContactComments contactId="contact-1" totalCount={2} />,
      );

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (2)`)).toBeInTheDocument();
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
        expect(screen.getByText(`${S.title} (3)`)).toBeInTheDocument();
      });

      rerender(
        <ContactComments contactId="contact-2" totalCount={0} />,
      );

      await waitFor(() => {
        expect(screen.getByText(`${S.title} (0)`)).toBeInTheDocument();
      });
    });
  });
});
