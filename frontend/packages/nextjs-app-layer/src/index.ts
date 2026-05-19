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
