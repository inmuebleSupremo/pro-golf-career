"use client";

import { useQuery } from "@tanstack/react-query";

import { gqlRequest } from "@/lib/api/graphql-client";
import { ListSavesDocument } from "@/lib/graphql/operations";

/** The player's saved games, newest first — the vertical-slice read. */
export function useSaves() {
  return useQuery({
    queryKey: ["saves"],
    queryFn: () => gqlRequest(ListSavesDocument),
  });
}
