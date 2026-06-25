import { render, type RenderOptions } from "@testing-library/react";
import { SessionProvider } from "next-auth/react";
import { TooltipProvider, LanguageProvider } from "@open-elements/ui";
import type { Language } from "@open-elements/ui";
import { AppLayerTranslationProvider } from "@open-elements/nextjs-app-layer";
import { translations } from "@/lib/i18n";
import type { Session } from "next-auth";

/**
 * Default test session. Includes APP-ADMIN and IT-ADMIN so that existing
 * component tests (which verify UI states that require these roles, e.g.
 * clickable delete buttons and visible admin menu) keep working.
 * Pass a custom `session` to override for role-specific tests.
 */
const defaultSession: Session = {
  user: { name: "Test User", email: "test@example.com", image: null },
  expires: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
  roles: ["APP-ADMIN", "IT-ADMIN"],
};

interface TestRenderOptions extends Omit<RenderOptions, "wrapper"> {
  readonly language?: Language;
  readonly session?: Session | null;
}

function renderWithProviders(ui: React.ReactElement, options?: TestRenderOptions) {
  const { language = "de", ...renderOptions } = options ?? {};
  const session = options && "session" in options ? options.session : defaultSession;

  function TestWrapper({ children }: { readonly children: React.ReactNode }) {
    return (
      <SessionProvider session={session}>
        <TooltipProvider>
          <LanguageProvider translations={translations} defaultLanguage={language}>
            <AppLayerTranslationProvider>{children}</AppLayerTranslationProvider>
          </LanguageProvider>
        </TooltipProvider>
      </SessionProvider>
    );
  }

  return render(ui, { wrapper: TestWrapper, ...renderOptions });
}

export { renderWithProviders };
