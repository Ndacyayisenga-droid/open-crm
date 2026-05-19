"use client";

import { createContext, useContext, type ReactNode } from "react";
import { defaultApiClient, type AppLayerApiClient } from "../api/client";

const ApiClientContext = createContext<AppLayerApiClient | null>(null);

export function ApiClientProvider({
  client = defaultApiClient,
  children,
}: {
  readonly client?: AppLayerApiClient;
  readonly children: ReactNode;
}) {
  return (
    <ApiClientContext.Provider value={client}>
      {children}
    </ApiClientContext.Provider>
  );
}

export function useApiClient(): AppLayerApiClient {
  const value = useContext(ApiClientContext);
  if (value === null) {
    throw new Error(
      "useApiClient must be used within <ApiClientProvider>",
    );
  }
  return value;
}
