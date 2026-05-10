import { describe, it, expect, afterEach, beforeEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { ContactDetail } from "@/components/contact-detail";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { ContactDto } from "@/lib/types";

const S = de.contacts;

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
  usePathname: () => "/contacts/test-id",
}));

const mockDeleteContact = vi.fn();
const mockGetContactComments = vi.fn();
const mockGetContactPhotoUrl = vi.fn().mockReturnValue("/api/contacts/test-id/photo");

vi.mock("@/lib/api", () => ({
  deleteContact: (...args: unknown[]) => mockDeleteContact(...args),
  getContactComments: (...args: unknown[]) => mockGetContactComments(...args),
  createContactComment: vi.fn(),
  deleteContactComment: vi.fn(),
  getContactPhotoUrl: (...args: unknown[]) => mockGetContactPhotoUrl(...args),
}));

function makeContact(overrides: Partial<ContactDto> = {}): ContactDto {
  return {
    id: "test-id",
    title: null,
    firstName: "Max",
    lastName: "Mustermann",
    email: "max@example.com",
    position: "CEO",
    gender: "MALE",
    socialLinks: [{networkType: "LINKEDIN", value: "max", url: "https://linkedin.com/in/max"}],
    phoneNumber: "+49 123 456",
    description: null,
    companyId: "company-1",
    companyName: "Open Elements",
    commentCount: 2,
    hasPhoto: false,
    birthday: "1990-03-15",
    brevo: true,
    receivesNewsletter: false,
    language: "DE",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    tagIds: [],
    ...overrides,
  };
}

beforeEach(() => {
  mockGetContactComments.mockResolvedValue([]);
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("ContactDetail", () => {
  it("should render all contact fields", () => {
    renderWithProviders(<ContactDetail contact={makeContact()} />);

    expect(screen.getByText("Max Mustermann")).toBeInTheDocument(); // heading
    expect(screen.getByText("max@example.com")).toBeInTheDocument();
    expect(screen.getByText("CEO")).toBeInTheDocument();
    expect(screen.getByText(S.form.male)).toBeInTheDocument();
    expect(screen.getByText("+49 123 456")).toBeInTheDocument();
    expect(screen.getByText("max")).toBeInTheDocument(); // LinkedIn social link value
    expect(screen.getByText(S.detail.languageDE)).toBeInTheDocument();
    expect(screen.getByText("Open Elements")).toBeInTheDocument();
  });

  it("should not show firstName/lastName as detail fields", () => {
    renderWithProviders(<ContactDetail contact={makeContact()} />);

    expect(screen.queryByText(S.detail.firstName)).not.toBeInTheDocument();
    expect(screen.queryByText(S.detail.lastName)).not.toBeInTheDocument();
  });

  it("should show language DE as Deutsch", () => {
    renderWithProviders(<ContactDetail contact={makeContact({ language: "DE" })} />);
    expect(screen.getByText(S.detail.languageDE)).toBeInTheDocument();
  });

  it("should show language EN as Englisch", () => {
    renderWithProviders(<ContactDetail contact={makeContact({ language: "EN" })} />);
    expect(screen.getByText(S.detail.languageEN)).toBeInTheDocument();
  });

  it("should show Brevo tag when contact is from Brevo", () => {
    renderWithProviders(
      <ContactDetail contact={makeContact({ brevo: true })} />,
    );

    expect(screen.getByText("Brevo")).toBeInTheDocument();
  });

  it("should not show Brevo tag when contact is not from Brevo", () => {
    renderWithProviders(
      <ContactDetail contact={makeContact({ brevo: false })} />,
    );

    expect(screen.queryByText("Brevo")).not.toBeInTheDocument();
  });

  it("should display Unbekannt when language is null", () => {
    renderWithProviders(<ContactDetail contact={makeContact({ language: null })} />);

    expect(screen.getByText(S.detail.languageUnknown)).toBeInTheDocument();
  });

  it("should handle missing optional fields with dash", () => {
    renderWithProviders(
      <ContactDetail
        contact={makeContact({
          email: null,
          position: null,
          gender: null,
          phoneNumber: null,
          socialLinks: [],
          companyId: null,
          companyName: null,
        })}
      />,
    );

    // Optional fields should show "—"
    const dashes = screen.getAllByText("—");
    expect(dashes.length).toBeGreaterThanOrEqual(5);
  });

  it("should show comments section with count and Add Comment button", async () => {
    renderWithProviders(<ContactDetail contact={makeContact({ commentCount: 2 })} />);

    await waitFor(() => {
      expect(screen.getByText(`${de.companies.comments.title} (2)`)).toBeInTheDocument();
      expect(screen.getByText(de.companies.comments.add)).toBeInTheDocument();
    });
  });

  it("should show edit button linking to edit page", () => {
    const { container } = renderWithProviders(<ContactDetail contact={makeContact()} />);

    const editLink = Array.from(container.querySelectorAll("a")).find(
      (a) => a.getAttribute("href") === "/contacts/test-id/edit",
    );

    expect(editLink).toBeInTheDocument();
    expect(editLink?.textContent).toContain(S.detail.edit);
  });

  it("should open delete dialog with permanent warning and comment loss", async () => {
    renderWithProviders(<ContactDetail contact={makeContact()} />);

    const deleteButtons = screen.getAllByText(S.detail.delete);
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      const expectedText = S.deleteDialog.description.replace("{name}", "Max Mustermann");
      expect(screen.getByText(expectedText)).toBeInTheDocument();
    });
  });

  it("should delete contact and navigate to list on confirm", async () => {
    mockDeleteContact.mockResolvedValue(undefined);

    renderWithProviders(<ContactDetail contact={makeContact()} />);

    const deleteButtons = screen.getAllByText(S.detail.delete);
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(screen.getByText(S.deleteDialog.confirm)).toBeInTheDocument();
    });

    const allConfirmButtons = screen.getAllByText(S.deleteDialog.confirm);
    fireEvent.click(allConfirmButtons[allConfirmButtons.length - 1]);

    await waitFor(() => {
      expect(mockDeleteContact).toHaveBeenCalledWith("test-id");
      expect(mockPush).toHaveBeenCalledWith("/contacts");
    });
  });

  it("should close dialog on cancel without deleting", async () => {
    renderWithProviders(<ContactDetail contact={makeContact()} />);

    const deleteButtons = screen.getAllByText(S.detail.delete);
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(screen.getByText(S.deleteDialog.cancel)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(S.deleteDialog.cancel));

    expect(mockDeleteContact).not.toHaveBeenCalled();
  });

  it("should show photo image when contact has photo", () => {
    const { container } = renderWithProviders(
      <ContactDetail contact={makeContact({ hasPhoto: true })} />,
    );

    const img = container.querySelector("img");
    expect(img).toBeInTheDocument();
    expect(img?.getAttribute("alt")).toBe("Max Mustermann");
  });

  it("should show placeholder when contact has no photo", () => {
    const { container } = renderWithProviders(
      <ContactDetail contact={makeContact({ hasPhoto: false })} />,
    );

    const img = container.querySelector("img");
    expect(img).toBeNull();
    const svg = container.querySelector("svg");
    expect(svg).toBeInTheDocument();
  });
});

describe("ContactDetail — Social Links", () => {
  it("should show social links grouped by network", () => {
    renderWithProviders(
      <ContactDetail
        contact={makeContact({
          socialLinks: [
            { networkType: "GITHUB", value: "hendrikebbers", url: "https://github.com/hendrikebbers" },
            { networkType: "LINKEDIN", value: "hendrik", url: "https://linkedin.com/in/hendrik" },
          ],
        })}
      />,
    );

    expect(screen.getByText("hendrikebbers")).toBeInTheDocument();
    expect(screen.getByText("hendrik")).toBeInTheDocument();
  });

  it("should not show social links section when empty", () => {
    renderWithProviders(
      <ContactDetail contact={makeContact({ socialLinks: [] })} />,
    );

    // No network labels should appear
    expect(screen.queryByText(S.form.networkGithub)).not.toBeInTheDocument();
    expect(screen.queryByText(S.form.networkLinkedin)).not.toBeInTheDocument();
  });

  it("should show only networks with links", () => {
    renderWithProviders(
      <ContactDetail
        contact={makeContact({
          socialLinks: [
            { networkType: "GITHUB", value: "octocat", url: "https://github.com/octocat" },
          ],
        })}
      />,
    );

    expect(screen.getByText("octocat")).toBeInTheDocument();
    // LinkedIn label should not appear (no linkedin links)
    expect(screen.queryByText(S.form.networkLinkedin)).not.toBeInTheDocument();
  });
});
