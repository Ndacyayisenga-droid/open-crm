import { describe, it, expect, afterEach, beforeEach, vi } from "vitest";
import { screen, cleanup, fireEvent, waitFor } from "@testing-library/react";
import { CompanyDetail } from "@/components/company-detail";
import { de } from "@/lib/i18n/de";
import { renderWithProviders } from "@/test/test-utils";
import type { CompanyDto } from "@/lib/types";

const S = de.companies;

const mockPush = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush }),
  usePathname: () => "/companies/test-id",
}));

const mockDeleteCompany = vi.fn();
const mockGetCompanyComments = vi.fn();
const mockGetCompanyLogoUrl = vi.fn().mockReturnValue("/api/companies/test-id/logo");

vi.mock("@/lib/api", () => ({
  deleteCompany: (...args: unknown[]) => mockDeleteCompany(...args),
  getCompanyComments: (...args: unknown[]) => mockGetCompanyComments(...args),
  createCompanyComment: vi.fn(),
  deleteCompanyComment: vi.fn(),
  getCompanyLogoUrl: (...args: unknown[]) => mockGetCompanyLogoUrl(...args),
  getTranslationSettings: () => Promise.resolve({ configured: false }),
  translateText: vi.fn(),
}));

const testCompany: CompanyDto = {
  id: "test-id",
  name: "Open Elements GmbH",
  email: "info@open-elements.com",
  website: "https://open-elements.com",
  street: "Musterstraße",
  houseNumber: "42",
  zipCode: "12345",
  city: "Berlin",
  country: "Germany",
  phoneNumber: "+49 30 12345678",
  description: null,
  bankName: null,
  bic: null,
  iban: null,
  vatId: null,
  brevo: false,
  hasLogo: false,
  contactCount: 3,
  commentCount: 5,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
  tagIds: [],
};

beforeEach(() => {
  mockGetCompanyComments.mockResolvedValue([]);
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("CompanyDetail", () => {
  it("should render all company fields with merged address", () => {
    renderWithProviders(<CompanyDetail company={testCompany} />);

    expect(screen.getAllByText("Open Elements GmbH").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("info@open-elements.com")).toBeInTheDocument();
    expect(screen.getByText("https://open-elements.com")).toBeInTheDocument();
    expect(screen.getByText(S.detail.address)).toBeInTheDocument();
    expect(screen.getByText(/Musterstraße 42[\s\S]*12345 Berlin[\s\S]*Germany/)).toBeInTheDocument();
  });

  it("should show address without house number", () => {
    renderWithProviders(
      <CompanyDetail company={{ ...testCompany, houseNumber: null }} />,
    );
    expect(screen.getByText(/Musterstraße[\s\S]*12345 Berlin/)).toBeInTheDocument();
  });

  it("should skip street line when street is null", () => {
    renderWithProviders(
      <CompanyDetail company={{ ...testCompany, street: null, houseNumber: "7", phoneNumber: null }} />,
    );
    // House number alone should not appear in the address
    const addressLabel = screen.getByText(S.detail.address);
    const addressContainer = addressLabel.closest("div");
    const addressText = addressContainer?.querySelector("dd span")?.textContent ?? "";
    expect(addressText).not.toContain("7\n");
    expect(screen.getByText(/12345 Berlin/)).toBeInTheDocument();
  });

  it("should show address with only city and country", () => {
    renderWithProviders(
      <CompanyDetail
        company={{
          ...testCompany,
          street: null,
          houseNumber: null,
          zipCode: null,
          city: "Hamburg",
          country: "Deutschland",
        }}
      />,
    );
    expect(screen.getByText(/Hamburg[\s\S]*Deutschland/)).toBeInTheDocument();
  });

  it("should show address with only zip code", () => {
    renderWithProviders(
      <CompanyDetail
        company={{
          ...testCompany,
          street: null,
          houseNumber: null,
          zipCode: "50667",
          city: null,
          country: null,
        }}
      />,
    );
    expect(screen.getByText("50667")).toBeInTheDocument();
  });

  it("should show address with only country", () => {
    renderWithProviders(
      <CompanyDetail
        company={{
          ...testCompany,
          street: null,
          houseNumber: null,
          zipCode: null,
          city: null,
          country: "Deutschland",
        }}
      />,
    );
    expect(screen.getByText("Deutschland")).toBeInTheDocument();
  });

  it("should show dash when all address fields are null", () => {
    renderWithProviders(
      <CompanyDetail
        company={{
          ...testCompany,
          street: null,
          houseNumber: null,
          zipCode: null,
          city: null,
          country: null,
        }}
      />,
    );
    const addressLabel = screen.getByText(S.detail.address);
    const addressContainer = addressLabel.closest("div");
    expect(addressContainer?.querySelector("dd")?.textContent).toBe("—");
  });

  it("should show edit button linking to edit page", () => {
    const { container } = renderWithProviders(<CompanyDetail company={testCompany} />);

    const editLink = Array.from(container.querySelectorAll("a")).find(
      (a) => a.getAttribute("href") === "/companies/test-id/edit",
    );

    expect(editLink).toBeInTheDocument();
    expect(editLink?.textContent).toContain(S.detail.edit);
  });

  it("should show delete button", () => {
    renderWithProviders(<CompanyDetail company={testCompany} />);

    const deleteButton = screen.getByText(S.detail.delete);
    expect(deleteButton).toBeInTheDocument();
  });

  it("should show comment section with title, count, and Add Comment button", async () => {
    renderWithProviders(<CompanyDetail company={testCompany} />);

    await waitFor(() => {
      expect(screen.getByText(`${S.comments.title} (5)`)).toBeInTheDocument();
      expect(screen.getByText(S.comments.empty)).toBeInTheDocument();
      expect(screen.getByText(S.comments.add)).toBeInTheDocument();
    });
  });

  it("should open delete dialog and delete all on confirm", async () => {
    mockDeleteCompany.mockResolvedValue(undefined);

    renderWithProviders(<CompanyDetail company={testCompany} />);

    // Click the page-level delete button
    const deleteButtons = screen.getAllByText(S.detail.delete);
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(screen.getByText(S.deleteDialog.deleteAll)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(S.deleteDialog.deleteAll));

    await waitFor(() => {
      expect(mockDeleteCompany).toHaveBeenCalledWith("test-id", true);
      expect(mockPush).toHaveBeenCalledWith("/companies");
    });
  });

  it("should delete company only when delete only button clicked", async () => {
    mockDeleteCompany.mockResolvedValue(undefined);

    renderWithProviders(<CompanyDetail company={testCompany} />);

    // Click the page-level delete button
    const deleteButtons = screen.getAllByText(S.detail.delete);
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(screen.getByText(S.deleteDialog.deleteOnly)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(S.deleteDialog.deleteOnly));

    await waitFor(() => {
      expect(mockDeleteCompany).toHaveBeenCalledWith("test-id", false);
      expect(mockPush).toHaveBeenCalledWith("/companies");
    });
  });

  it("should show Brevo tag when company is from Brevo", () => {
    renderWithProviders(<CompanyDetail company={{ ...testCompany, brevo: true }} />);

    expect(screen.getByText("Brevo")).toBeInTheDocument();
  });

  it("should not show Brevo tag when company is not from Brevo", () => {
    renderWithProviders(<CompanyDetail company={{ ...testCompany, brevo: false }} />);

    expect(screen.queryByText("Brevo")).not.toBeInTheDocument();
  });

  it("should show logo image when company has logo", () => {
    const { container } = renderWithProviders(
      <CompanyDetail company={{ ...testCompany, hasLogo: true }} />,
    );

    const img = container.querySelector("img");
    expect(img).toBeInTheDocument();
    expect(img?.getAttribute("alt")).toBe("Open Elements GmbH");
  });

  it("should show placeholder when company has no logo", () => {
    const { container } = renderWithProviders(
      <CompanyDetail company={{ ...testCompany, hasLogo: false }} />,
    );

    const img = container.querySelector("img");
    expect(img).toBeNull();
    const svg = container.querySelector("svg");
    expect(svg).toBeInTheDocument();
  });
});

describe("CompanyDetail — Finance Section", () => {
  it("should show Finanzen section when financial fields are present", () => {
    renderWithProviders(
      <CompanyDetail
        company={{
          ...testCompany,
          bankName: "Deutsche Bank",
          iban: "DE89370400440532013000",
        }}
      />,
    );

    expect(screen.getByText(S.detail.finance.title)).toBeInTheDocument();
    expect(screen.getByText("Deutsche Bank")).toBeInTheDocument();
    expect(screen.getByText("DE89370400440532013000")).toBeInTheDocument();
  });

  it("should hide Finanzen section when all financial fields are null", () => {
    renderWithProviders(<CompanyDetail company={testCompany} />);

    expect(screen.queryByText(S.detail.finance.title)).not.toBeInTheDocument();
  });

  it("should show only non-null financial fields", () => {
    renderWithProviders(
      <CompanyDetail
        company={{
          ...testCompany,
          iban: "DE89370400440532013000",
        }}
      />,
    );

    expect(screen.getByText(S.detail.finance.title)).toBeInTheDocument();
    expect(screen.getByText("DE89370400440532013000")).toBeInTheDocument();
    // Bank label is shown but value is dash since bankName is null
    expect(screen.getByText(S.detail.finance.bankName)).toBeInTheDocument();
  });

  it("should show all four financial fields when all are set", () => {
    renderWithProviders(
      <CompanyDetail
        company={{
          ...testCompany,
          bankName: "Deutsche Bank",
          bic: "DEUTDEFF",
          iban: "DE89370400440532013000",
          vatId: "DE123456789",
        }}
      />,
    );

    expect(screen.getByText("Deutsche Bank")).toBeInTheDocument();
    expect(screen.getByText("DEUTDEFF")).toBeInTheDocument();
    expect(screen.getByText("DE89370400440532013000")).toBeInTheDocument();
    expect(screen.getByText("DE123456789")).toBeInTheDocument();
  });
});
