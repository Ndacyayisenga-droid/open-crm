import { describe, it, expect, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { LanguageProvider } from "@open-elements/ui";
import {
  AppLayerTranslationProvider,
  useAppLayerTranslations,
  appLayerTranslations,
} from "../provider";

afterEach(() => cleanup());

function Probe() {
  const t = useAppLayerTranslations();
  return <span data-testid="title">{t.auditLog.title}</span>;
}

describe("AppLayerTranslationProvider", () => {
  it("resolves to the de bundle when language is de", () => {
    render(
      <LanguageProvider translations={appLayerTranslations} defaultLanguage="de">
        <AppLayerTranslationProvider>
          <Probe />
        </AppLayerTranslationProvider>
      </LanguageProvider>,
    );
    expect(screen.getByTestId("title")).toHaveTextContent(
      appLayerTranslations.de.auditLog.title,
    );
  });

  it("resolves to the en bundle when language is en", () => {
    render(
      <LanguageProvider translations={appLayerTranslations} defaultLanguage="en">
        <AppLayerTranslationProvider>
          <Probe />
        </AppLayerTranslationProvider>
      </LanguageProvider>,
    );
    expect(screen.getByTestId("title")).toHaveTextContent(
      appLayerTranslations.en.auditLog.title,
    );
  });

  it("throws a recognizable error when used outside the provider", () => {
    expect(() =>
      render(
        <LanguageProvider translations={appLayerTranslations} defaultLanguage="en">
          <Probe />
        </LanguageProvider>,
      ),
    ).toThrowError(/AppLayerTranslationProvider/);
  });
});
