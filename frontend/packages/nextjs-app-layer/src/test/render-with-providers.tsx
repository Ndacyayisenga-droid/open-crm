import { render, type RenderOptions } from "@testing-library/react";
import { SessionProvider } from "next-auth/react";
import { LanguageProvider, TooltipProvider } from "@open-elements/ui";
import type { Language } from "@open-elements/ui";
import type { Session } from "next-auth";
import {
  AppLayerTranslationProvider,
  appLayerTranslations,
} from "../translations/provider";
import { ApiClientProvider } from "../hooks/api-client";
import type { AppLayerApiClient } from "../api/client";

const defaultSession: Session = {
  user: { name: "Test User", email: "test@example.com", image: null },
  expires: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
  roles: ["ADMIN", "IT-ADMIN"],
};

interface ProviderRenderOptions extends Omit<RenderOptions, "wrapper"> {
  readonly language?: Language;
  readonly session?: Session | null;
  readonly apiClient?: AppLayerApiClient;
}

export function renderWithLibProviders(
  ui: React.ReactElement,
  options?: ProviderRenderOptions,
) {
  const { language = "en", apiClient, ...rest } = options ?? {};
  const session = options && "session" in options ? options.session : defaultSession;

  function Wrapper({ children }: { readonly children: React.ReactNode }) {
    return (
      <SessionProvider session={session}>
        <TooltipProvider>
          <LanguageProvider translations={appLayerTranslations} defaultLanguage={language}>
            <AppLayerTranslationProvider>
              <ApiClientProvider client={apiClient}>{children}</ApiClientProvider>
            </AppLayerTranslationProvider>
          </LanguageProvider>
        </TooltipProvider>
      </SessionProvider>
    );
  }

  return render(ui, { wrapper: Wrapper, ...rest });
}
