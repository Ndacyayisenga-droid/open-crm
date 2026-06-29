import { auth } from "@/auth";
import { ForbiddenPage, ROLE_IT_ADMIN } from "@open-elements/nextjs-app-layer";
import { BackupPageClient } from "./backup-page-client";

export default async function BackupPage() {
  const session = await auth();
  if (!session?.roles?.includes(ROLE_IT_ADMIN)) {
    return <ForbiddenPage />;
  }
  return <BackupPageClient />;
}
