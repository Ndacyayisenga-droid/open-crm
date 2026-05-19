import { KeyRound } from "lucide-react";
import { ROLE_IT_ADMIN } from "../../lib/roles";
import type { AppLayerTranslations } from "../../translations/provider";

export const apiKeysPageMeta = {
  defaultRoute: "/api-keys",
  icon: KeyRound,
  label: (t: AppLayerTranslations) => t.nav.apiKeys,
  requiredRole: ROLE_IT_ADMIN,
} as const;
