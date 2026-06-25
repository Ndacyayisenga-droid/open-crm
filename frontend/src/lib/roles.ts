import type { Session } from "next-auth";

/** Matches backend `@RequiresAppAdmin` / JWT claim `APP-ADMIN`. */
export const ROLE_APP_ADMIN = "APP-ADMIN";

export const ROLE_IT_ADMIN = "IT-ADMIN";

export function hasAppAdmin(session: Session | null | undefined): boolean {
  return !!session?.roles?.includes(ROLE_APP_ADMIN);
}

export function hasItAdmin(session: Session | null | undefined): boolean {
  return !!session?.roles?.includes(ROLE_IT_ADMIN);
}
