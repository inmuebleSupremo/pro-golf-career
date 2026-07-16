"use client";

import { useQuery } from "@tanstack/react-query";

import { gqlRequest } from "@/lib/api/graphql-client";
import {
  CareerOverviewDocument,
  HallOfFameDocument,
  ListSavesDocument,
  PlayerProfileDocument,
} from "@/lib/graphql/operations";

/** The player's saved games, newest first — the vertical-slice read. */
export function useSaves() {
  return useQuery({
    queryKey: ["saves"],
    queryFn: () => gqlRequest(ListSavesDocument),
  });
}

/** The read-only overview of a loaded session: world status, goals, and schedule. */
export function useCareerOverview(id: string) {
  return useQuery({
    queryKey: ["career", id],
    queryFn: () => gqlRequest(CareerOverviewDocument, { id }),
    // Never pause on perceived-offline (some embedded browsers report offline), and
    // don't retry a not-found session — surface the error immediately.
    networkMode: "always",
    retry: false,
  });
}

/** The player's golfer profile — identity, attributes, ranking, earnings. */
export function usePlayerProfile(id: string) {
  return useQuery({
    queryKey: ["profile", id],
    queryFn: () => gqlRequest(PlayerProfileDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

/** The world's Hall-of-Fame inductions (name-enriched). */
export function useHallOfFame(id: string) {
  return useQuery({
    queryKey: ["hallOfFame", id],
    queryFn: () => gqlRequest(HallOfFameDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}
