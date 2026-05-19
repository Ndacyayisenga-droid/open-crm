import { Webhook } from "lucide-react";
import { ROLE_IT_ADMIN } from "../../lib/roles";
import type { AppLayerTranslations } from "../../translations/provider";

export const webhooksPageMeta = {
  defaultRoute: "/webhooks",
  icon: Webhook,
  label: (t: AppLayerTranslations) => t.nav.webhooks,
  requiredRole: ROLE_IT_ADMIN,
} as const;
