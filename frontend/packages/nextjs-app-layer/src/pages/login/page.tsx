import { LoginClient } from "./login-client";

export function createLoginPage({ homeRoute }: { readonly homeRoute?: string } = {}) {
  return function LoginPage() {
    return <LoginClient homeRoute={homeRoute} />;
  };
}
