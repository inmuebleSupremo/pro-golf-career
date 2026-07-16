"use client";

import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

/*
 * App-wide client providers. The QueryClient is created once per browser session
 * (via useState) so it survives re-renders. Authenticated data is fetched through
 * the same-origin /api/graphql BFF, so no auth wiring is needed here.
 */
export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          // networkMode "always": the app talks only to the same-origin BFF and has no
          // offline-first behaviour, so never pause fetches/retries on perceived
          // connectivity (some embedded browsers report navigator.onLine === false,
          // which would otherwise pause a retrying query in a stuck pending state).
          queries: {
            staleTime: 30_000,
            retry: 1,
            refetchOnWindowFocus: false,
            networkMode: "always",
          },
          mutations: {
            networkMode: "always",
          },
        },
      }),
  );

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
