"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { signIn, useSession } from "next-auth/react";
import { Button } from "@open-elements/ui";
import { useAppLayerTranslations } from "../../translations/provider";

export function LoginClient({ homeRoute = "/" }: { readonly homeRoute?: string }) {
  return (
    <Suspense>
      <LoginContent homeRoute={homeRoute} />
    </Suspense>
  );
}

function LoginContent({ homeRoute }: { readonly homeRoute: string }) {
  const t = useAppLayerTranslations();
  const S = t.login;
  const router = useRouter();
  const searchParams = useSearchParams();
  const { data: session, status } = useSession();
  const error = searchParams.get("error");

  useEffect(() => {
    if (error) {
      console.error("Auth error:", error);
    }
  }, [error]);

  useEffect(() => {
    if (status === "authenticated" && session) {
      router.replace(homeRoute);
    }
  }, [status, session, router, homeRoute]);

  if (status === "loading" || (status === "authenticated" && session)) {
    return null;
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-oe-white md:bg-oe-gray-lightest">
      <div className="flex w-full flex-col items-center gap-8 bg-oe-dark px-8 py-16 text-white md:max-w-md md:rounded-2xl">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src="/oe-logo-landscape-dark.svg"
          alt="Open Elements"
          className="h-12"
        />

        <h1 className="font-heading text-3xl font-bold">{S.title}</h1>

        {error && (
          <p className="text-center text-sm text-oe-red">{S.error}</p>
        )}

        <Button
          onClick={() => signIn("oidc")}
          className="w-full max-w-xs py-6 text-lg"
          size="lg"
        >
          {S.button}
        </Button>

        <div className="mt-4 flex items-center gap-2 text-xs text-oe-gray-light">
          <span>{S.developedBy}</span>
          <a
            href="https://open-elements.com"
            target="_blank"
            rel="noopener noreferrer"
            className="underline hover:text-white"
          >
            Open Elements
          </a>
        </div>
      </div>
    </div>
  );
}
