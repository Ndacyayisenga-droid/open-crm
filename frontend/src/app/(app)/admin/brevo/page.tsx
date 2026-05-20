import { auth } from "@/auth";
import { ForbiddenPage } from "@open-elements/nextjs-app-layer";
import { ROLE_IT_ADMIN } from "@open-elements/nextjs-app-layer";
import { BrevoPageClient } from "./brevo-page-client";

export default async function BrevoPage() {
  const session = await auth();
  if (!session?.roles?.includes(ROLE_IT_ADMIN)) {
    return <ForbiddenPage />;
  }
  return <BrevoPageClient />;
}
