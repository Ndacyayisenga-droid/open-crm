import { Montserrat, Lato } from "next/font/google";
import { LanguageProvider } from "@open-elements/ui";
import type { Language } from "@open-elements/ui";
import { SessionProvider } from "../components/session-provider";
import { AppLayerTranslationProvider } from "../translations/provider";
import { ApiClientProvider } from "../hooks/api-client";
import type { AppLayerApiClient } from "../api/client";

const montserrat = Montserrat({
  subsets: ["latin"],
  variable: "--font-heading",
  display: "swap",
});

const lato = Lato({
  subsets: ["latin"],
  weight: ["400", "700"],
  variable: "--font-body",
  display: "swap",
});

/**
 * OE-standard root layout. Renders `<html>` with Montserrat + Lato font
 * variables, `<body>`, and the full provider stack (`SessionProvider`,
 * `LanguageProvider`, `AppLayerTranslationProvider`, `ApiClientProvider`).
 *
 * The consuming app passes its translation tree to drive `LanguageProvider`,
 * and optionally a custom `apiClient` to override the default proxy-based
 * implementation.
 */
export function OERootLayout({
  children,
  translations,
  apiClient,
  htmlLang = "en",
}: {
  readonly children: React.ReactNode;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  readonly translations: Record<Language, any>;
  readonly apiClient?: AppLayerApiClient;
  readonly htmlLang?: string;
}) {
  return (
    <html lang={htmlLang} className={`${montserrat.variable} ${lato.variable}`}>
      <body className="antialiased">
        <SessionProvider>
          <LanguageProvider translations={translations}>
            <AppLayerTranslationProvider>
              <ApiClientProvider client={apiClient}>{children}</ApiClientProvider>
            </AppLayerTranslationProvider>
          </LanguageProvider>
        </SessionProvider>
      </body>
    </html>
  );
}
