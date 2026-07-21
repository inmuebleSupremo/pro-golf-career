"use client";

import { useQuery } from "@tanstack/react-query";

import { gqlRequest } from "@/lib/api/graphql-client";
import {
  CareerOverviewDocument,
  HallOfFameDocument,
  ListSavesDocument,
  PlayerCalendarDocument,
  PlayerProfileDocument,
  WorldRankingsDocument,
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

/** The current World Ranking (name-enriched), for the rankings + rivals spokes. */
export function useWorldRankings(id: string, limit = 100) {
  return useQuery({
    queryKey: ["rankings", id, limit],
    queryFn: () => gqlRequest(WorldRankingsDocument, { id, limit }),
    networkMode: "always",
    retry: false,
  });
}

/** The player's season calendar with per-event results. */
export function usePlayerCalendar(id: string) {
  return useQuery({
    queryKey: ["calendar", id],
    queryFn: () => gqlRequest(PlayerCalendarDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}
