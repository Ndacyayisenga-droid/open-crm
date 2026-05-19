"use client";

import { useEffect } from "react";
import {
  SessionProvider as NextAuthSessionProvider,
  useSession,
} from "next-auth/react";

function RefreshTokenErrorWatcher() {
  const { data: session } = useSession();
  useEffect(() => {
    if (session?.error === "RefreshTokenError") {
      window.location.href = "/api/logout";
    }
  }, [session?.error]);
  return null;
}

export function SessionProvider({
  children,
}: {
  readonly children: React.ReactNode;
}) {
  return (
    <NextAuthSessionProvider refetchInterval={120} refetchOnWindowFocus>
      <RefreshTokenErrorWatcher />
      {children}
    </NextAuthSessionProvider>
  );
}
