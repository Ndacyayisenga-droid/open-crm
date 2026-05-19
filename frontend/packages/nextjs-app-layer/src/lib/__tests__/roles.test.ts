import { describe, it, expect } from "vitest";
import { ROLE_ADMIN, ROLE_IT_ADMIN, hasRole } from "../roles";

describe("roles", () => {
  it("exposes the OE-convention role names", () => {
    expect(ROLE_ADMIN).toBe("ADMIN");
    expect(ROLE_IT_ADMIN).toBe("IT-ADMIN");
  });

  it("hasRole returns true when the session has the role", () => {
    // @ts-expect-error — minimal session shape for the test
    expect(hasRole({ roles: ["ADMIN"] }, "ADMIN")).toBe(true);
  });

  it("hasRole returns false when the session lacks the role", () => {
    // @ts-expect-error — minimal session shape for the test
    expect(hasRole({ roles: ["ADMIN"] }, "IT-ADMIN")).toBe(false);
  });

  it("hasRole returns false for null/undefined session", () => {
    expect(hasRole(null, "ADMIN")).toBe(false);
    expect(hasRole(undefined, "ADMIN")).toBe(false);
  });
});
