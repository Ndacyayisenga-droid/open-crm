import type { Session } from "next-auth";
import { ForbiddenPage } from "../../../components/forbidden-page";
import { ROLE_IT_ADMIN } from "../../../lib/roles";
import { ServerStatusClient } from "./server-status-client";

type AuthFn = () => Promise<Session | null>;

export function createServerStatusPage({ auth, homeRoute }: { readonly auth: AuthFn; readonly homeRoute?: string }) {
  return async function ServerStatusPage() {
    const session = await auth();
    if (!session?.roles?.includes(ROLE_IT_ADMIN)) {
      return <ForbiddenPage homeRoute={homeRoute} />;
    }
    return <ServerStatusClient />;
  };
}
