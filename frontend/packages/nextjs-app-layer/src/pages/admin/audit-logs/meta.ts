import { FileText } from "lucide-react";
import { ROLE_IT_ADMIN } from "../../../lib/roles";
import type { AppLayerTranslations } from "../../../translations/provider";

export const auditLogsPageMeta = {
  defaultRoute: "/admin/audit-logs",
  icon: FileText,
  label: (t: AppLayerTranslations) => t.nav.auditLogs,
  requiredRole: ROLE_IT_ADMIN,
} as const;
