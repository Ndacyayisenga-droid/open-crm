import { describe, it, expect, afterEach } from "vitest";
import { cleanup, screen } from "@testing-library/react";
import { ForbiddenPage } from "../forbidden-page";
import { renderWithLibProviders } from "../../test/render-with-providers";
import { appLayerTranslations } from "../../translations/provider";

afterEach(() => cleanup());

describe("ForbiddenPage", () => {
  it("renders the English title, description, and back link by default", () => {
    renderWithLibProviders(<ForbiddenPage />, { language: "en" });
    expect(
      screen.getByText(appLayerTranslations.en.errors.forbidden.title),
    ).toBeInTheDocument();
    expect(
      screen.getByText(appLayerTranslations.en.errors.forbidden.description),
    ).toBeInTheDocument();
    const link = screen.getByRole("link", {
      name: appLayerTranslations.en.errors.forbidden.backToHome,
    });
    expect(link).toHaveAttribute("href", "/");
  });

  it("respects a custom homeRoute prop", () => {
    renderWithLibProviders(<ForbiddenPage homeRoute="/updates" />, { language: "en" });
    const link = screen.getByRole("link", {
      name: appLayerTranslations.en.errors.forbidden.backToHome,
    });
    expect(link).toHaveAttribute("href", "/updates");
  });

  it("renders German strings when language is de", () => {
    renderWithLibProviders(<ForbiddenPage />, { language: "de" });
    expect(
      screen.getByText(appLayerTranslations.de.errors.forbidden.title),
    ).toBeInTheDocument();
  });
});
