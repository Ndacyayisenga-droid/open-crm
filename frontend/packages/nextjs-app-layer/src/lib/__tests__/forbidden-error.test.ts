import { describe, it, expect } from "vitest";
import { ForbiddenError } from "../forbidden-error";

describe("ForbiddenError", () => {
  it("is recognizable via instanceof", () => {
    const err = new ForbiddenError();
    expect(err).toBeInstanceOf(ForbiddenError);
    expect(err).toBeInstanceOf(Error);
  });

  it("defaults message to 'Forbidden'", () => {
    expect(new ForbiddenError().message).toBe("Forbidden");
  });

  it("accepts a custom message", () => {
    expect(new ForbiddenError("Access denied").message).toBe("Access denied");
  });

  it("sets the name to 'ForbiddenError'", () => {
    expect(new ForbiddenError().name).toBe("ForbiddenError");
  });
});
