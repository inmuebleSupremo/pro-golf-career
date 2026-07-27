"use client";

import { useQuery } from "@tanstack/react-query";

import { gqlRequest } from "@/lib/api/graphql-client";
import {
  CareerOverviewDocument,
  DevelopmentReportDocument,
  HallOfFameDocument,
  ListSavesDocument,
  PlayerCalendarDocument,
  PlayerDevelopmentDocument,
  PlayerDevelopmentFocusDocument,
  PlayerFitnessDocument,
  PlayerProfileDocument,
  RecordsDocument,
  SeasonReviewDocument,
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

/**
 * A completed season's off-season review (the end-of-season moment). Omit `season` for the most recently
 * completed one; the query resolves to null when no season has completed yet.
 */
export function useSeasonReview(id: string, season?: number) {
  return useQuery({
    queryKey: ["seasonReview", id, season ?? null],
    queryFn: () => gqlRequest(SeasonReviewDocument, { id, season }),
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

/** The world Record Book — the current holder of each record (name-enriched). */
export function useRecords(id: string) {
  return useQuery({
    queryKey: ["records", id],
    queryFn: () => gqlRequest(RecordsDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

/** The player's current development focus (attribute enum names), for read-back on the Development page. */
export function usePlayerDevelopmentFocus(id: string) {
  return useQuery({
    queryKey: ["focus", id],
    queryFn: () => gqlRequest(PlayerDevelopmentFocusDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

/** The player's banked Development Points and cost-curve constants (for the spend screen). */
export function usePlayerDevelopment(id: string) {
  return useQuery({
    queryKey: ["development", id],
    queryFn: () => gqlRequest(PlayerDevelopmentDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

/** The player's attribute gains from their most recently developed season (end-of-season report). */
export function useDevelopmentReport(id: string) {
  return useQuery({
    queryKey: ["development-report", id],
    queryFn: () => gqlRequest(DevelopmentReportDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

/** The player's fitness — condition, fatigue, availability, and any active injury. */
export function usePlayerFitness(id: string) {
  return useQuery({
    queryKey: ["fitness", id],
    queryFn: () => gqlRequest(PlayerFitnessDocument, { id }),
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
