import { Activity } from "lucide-react";
import { ROLE_IT_ADMIN } from "../../../lib/roles";
import type { AppLayerTranslations } from "../../../translations/provider";

export const serverStatusPageMeta = {
  defaultRoute: "/admin/status",
  icon: Activity,
  label: (t: AppLayerTranslations) => t.nav.serverStatus,
  requiredRole: ROLE_IT_ADMIN,
} as const;
