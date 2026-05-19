import { describe, it, expect, afterEach, vi } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { ApiClientProvider, useApiClient } from "../api-client";
import type { AppLayerApiClient } from "../../api/client";

afterEach(() => cleanup());

function Probe() {
  const client = useApiClient();
  return <span data-testid="probe">{typeof client.getUsers}</span>;
}

describe("ApiClientProvider", () => {
  it("provides the default client when no client prop is passed", () => {
    render(
      <ApiClientProvider>
        <Probe />
      </ApiClientProvider>,
    );
    expect(screen.getByTestId("probe")).toHaveTextContent("function");
  });

  it("uses the provided custom client", () => {
    const custom: AppLayerApiClient = {
      getUsers: vi.fn(),
    } as unknown as AppLayerApiClient;
    function CustomProbe() {
      const c = useApiClient();
      return <span data-testid="same">{String(c === custom)}</span>;
    }
    render(
      <ApiClientProvider client={custom}>
        <CustomProbe />
      </ApiClientProvider>,
    );
    expect(screen.getByTestId("same")).toHaveTextContent("true");
  });

  it("throws a recognizable error when used outside the provider", () => {
    expect(() => render(<Probe />)).toThrowError(/ApiClientProvider/);
  });
});
