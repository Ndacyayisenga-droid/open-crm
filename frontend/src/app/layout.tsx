import type { Metadata } from "next";
import { Montserrat, Lato } from "next/font/google";
import { SessionProvider } from "@/components/session-provider";
import { LanguageProvider } from "@open-elements/ui";
import { AppLayerTranslationProvider, ApiClientProvider } from "@open-elements/nextjs-app-layer";
import { translations } from "@/lib/i18n";
import "./globals.css";

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

export const metadata: Metadata = {
  title: "Open CRM",
  description: "CRM system by Open Elements",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={`${montserrat.variable} ${lato.variable}`}>
      <body className="antialiased">
        <SessionProvider>
          <LanguageProvider translations={translations}>
            <AppLayerTranslationProvider>
              <ApiClientProvider>
                {children}
              </ApiClientProvider>
            </AppLayerTranslationProvider>
          </LanguageProvider>
        </SessionProvider>
      </body>
    </html>
  );
}
