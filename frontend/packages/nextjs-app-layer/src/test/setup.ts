import "@testing-library/jest-dom/vitest";

// jsdom 29's default doesn't expose a working localStorage in every test run.
// Components in the lib read pageSize from localStorage on mount; provide a
// deterministic in-memory shim so tests don't blow up on `getItem`.
if (typeof globalThis.localStorage === "undefined") {
  let store: Record<string, string> = {};
  Object.defineProperty(globalThis, "localStorage", {
    configurable: true,
    value: {
      getItem: (k: string) => store[k] ?? null,
      setItem: (k: string, v: string) => {
        store[k] = String(v);
      },
      removeItem: (k: string) => {
        delete store[k];
      },
      clear: () => {
        store = {};
      },
      key: (i: number) => Object.keys(store)[i] ?? null,
      get length() {
        return Object.keys(store).length;
      },
    },
  });
}
