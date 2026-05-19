// Public client-safe surface of @open-elements/nextjs-app-layer.
// Server-only factories live in ./server.ts and are imported via
// `@open-elements/nextjs-app-layer/server`.

export { ROLE_ADMIN, ROLE_IT_ADMIN, hasRole } from "./lib/roles";
export { ForbiddenError } from "./lib/forbidden-error";

export {
  AppLayerTranslationProvider,
  useAppLayerTranslations,
  appLayerTranslations,
} from "./translations/provider";
export type { AppLayerTranslations } from "./translations/provider";

export { SessionProvider } from "./components/session-provider";
export { ForbiddenPage } from "./components/forbidden-page";
export { BearerTokenCard } from "./components/bearer-token-card";
export { AddCommentDialog } from "./components/add-comment-dialog";

export { ApiClientProvider, useApiClient } from "./hooks/api-client";
export { defaultApiClient } from "./api/client";
export type { AppLayerApiClient } from "./api/client";

export { createAuditLogsPage } from "./pages/admin/audit-logs/page";
export { AuditLogsClient } from "./pages/admin/audit-logs/audit-logs-client";
export { auditLogsPageMeta } from "./pages/admin/audit-logs/meta";

export { createUsersPage } from "./pages/admin/users/page";
export { UsersClient } from "./pages/admin/users/users-client";
export { usersPageMeta } from "./pages/admin/users/meta";

export { createServerStatusPage } from "./pages/admin/status/page";
export { ServerStatusClient } from "./pages/admin/status/server-status-client";
export { serverStatusPageMeta } from "./pages/admin/status/meta";

export { createBearerTokenPage } from "./pages/admin/token/page";
export { BearerTokenClient } from "./pages/admin/token/bearer-token-client";
export { bearerTokenPageMeta } from "./pages/admin/token/meta";

export type {
  Page,
  UserDto,
  AuditAction,
  AuditLogDto,
  ApiKeyDto,
  ApiKeyCreateDto,
  ApiKeyCreatedDto,
  WebhookDto,
  WebhookCreateDto,
  WebhookUpdateDto,
  TranslationConfigDto,
  PageRequest,
} from "./api/types";
