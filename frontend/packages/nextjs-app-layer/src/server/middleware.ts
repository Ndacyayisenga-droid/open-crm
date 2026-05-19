/**
 * Canonical OE middleware matcher string. Excludes the auth/logout API routes,
 * the public login page, Next.js internal paths, and common static assets.
 *
 * ⚠️ DO NOT re-export this object as `config` from the consuming app's
 * `middleware.ts`. Next.js' build-time analyzer extracts the matcher
 * statically from `middleware.ts` and does NOT follow re-exports across the
 * workspace-package boundary. A re-export silently falls back to the default
 * matcher (run middleware on every request), which routes `/_next/static/*`
 * and `/_next/image/*` through the auth middleware and breaks the deployment.
 *
 * The consuming app must declare `config` as a static literal inside its own
 * `middleware.ts`. This export exists only as a reference value for the
 * canonical matcher pattern — copy the string, do not re-export the object.
 */
export const middlewareConfig = {
  matcher: ["/((?!api/auth|api/logout|login|_next/static|_next/image|favicon\\.ico|.*\\.svg$|.*\\.png$|.*\\.jpg$|.*\\.ico$).*)"],
};
