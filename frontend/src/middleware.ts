export { auth as middleware } from "@/auth";

// IMPORTANT: `config` must be defined here as a static literal. Next.js'
// build-time analyzer extracts the matcher from this file's source and does
// not follow re-exports across the workspace-package boundary. Re-exporting
// from `@open-elements/nextjs-app-layer/server` silently falls back to the
// default matcher (run on every request), which routes `/_next/static/*` and
// `/_next/image/*` through the auth middleware and breaks the deployed app.
export const config = {
  matcher: [
    "/((?!api/auth|api/logout|login|_next/static|_next/image|favicon\\.ico|.*\\.svg$|.*\\.png$|.*\\.jpg$|.*\\.ico$).*)",
  ],
};
