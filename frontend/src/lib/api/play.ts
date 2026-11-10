"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { gqlRequest } from "@/lib/api/graphql-client";
import type { BallStrikeIntentInput, PhysicalPuttIntentInput } from "@/lib/graphql/generated/graphql";
import {
  AdvanceSeasonDocument,
  SeasonAdvanceOperationDocument,
  AdvanceWeekDocument,
  CompleteEventDocument,
  PlayingHoleDocument,
  PlayPuttDocument,
  PlayPhysicalPuttDocument,
  PlayShotDocument,
  PlayStateDocument,
  SimEventDocument,
  SimHoleDocument,
  SimRoundDocument,
  SimShotDocument,
} from "@/lib/graphql/operations";

/** World status, current shot, and leaderboard for a session, in one request. */
export function usePlayState(id: string) {
  return useQuery({
    queryKey: ["play", id],
    queryFn: () => gqlRequest(PlayStateDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

/**
 * Geometry of a hole in the pending event, for the 2D render (spec: web-hole-visualization). Defaults to the
 * current hole; pass an explicit `hole` (1..18) to pre-fetch another. Keyed by hole so it refetches as play
 * advances to the next hole, and is invalidated alongside the play state after each shot.
 */
export function usePlayingHole(id: string, hole?: number) {
  return useQuery({
    queryKey: ["playingHole", id, hole ?? "current"],
    queryFn: () => gqlRequest(PlayingHoleDocument, { id, hole }),
    networkMode: "always",
    retry: false,
  });
}

/** Invalidate the play state and the career overview after a play mutation. */
function useInvalidatePlay(id: string) {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ["play", id] });
    queryClient.invalidateQueries({ queryKey: ["playingHole", id] });
    queryClient.invalidateQueries({ queryKey: ["career", id] });
    queryClient.invalidateQueries({ queryKey: ["inbox", id] });
  };
}

export function useAdvanceWeek(id: string) {
  const invalidate = useInvalidatePlay(id);
  return useMutation({
    mutationFn: () => gqlRequest(AdvanceWeekDocument, { id }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

/**
 * Advance to the end of the season, simming the player's remaining entered events. A whole season
 * moves ranking, earnings, standings, offers and history at once. The caller refreshes session-scoped
 * queries after confirmed completion, including when completion is learned from the status query.
 */
export function useAdvanceSeason(id: string) {
  return useMutation({
    mutationFn: ({ requestId, expectedSeason }: { requestId: string; expectedSeason: number }) =>
      gqlRequest(AdvanceSeasonDocument, { id, requestId, expectedSeason }),
    networkMode: "always",
  });
}

/** Reads the backend operation record; RUNNING attempts are polled until they settle. */
export function useSeasonAdvanceOperation(id: string) {
  return useQuery({
    queryKey: ["season-advance", id],
    queryFn: () => gqlRequest(SeasonAdvanceOperationDocument, { id }),
    networkMode: "always",
    retry: false,
    refetchInterval: (query) => seasonAdvancePollingInterval(
      query.state.status, query.state.data?.seasonAdvanceOperation?.state),
  });
}

/** A failed status read makes cached RUNNING data stale; wait for an explicit Check status action. */
export function seasonAdvancePollingInterval(status: string, state?: string | null): number | false {
  return status !== "error" && state === "RUNNING" ? 2_000 : false;
}

export function usePlayShot(id: string) {
  const invalidate = useInvalidatePlay(id);
  return useMutation({
    mutationFn: (intent: BallStrikeIntentInput) => gqlRequest(PlayShotDocument, { id, intent }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

export function usePlayPutt(id: string) {
  const invalidate = useInvalidatePlay(id);
  return useMutation({
    mutationFn: (expectedShotRevision: string) => gqlRequest(PlayPuttDocument, { id, expectedShotRevision }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

/** Sends a player-selected physical line and level-ground pace through the normal shot-revision guard. */
export function usePlayPhysicalPutt(id: string) {
  const invalidate = useInvalidatePlay(id);
  return useMutation({
    mutationFn: (intent: PhysicalPuttIntentInput) => gqlRequest(PlayPhysicalPuttDocument, { id, intent }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

export function useSimShot(id: string) {
  const invalidate = useInvalidatePlay(id);
  return useMutation({
    mutationFn: () => gqlRequest(SimShotDocument, { id }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

export function useSimHole(id: string) {
  const invalidate = useInvalidatePlay(id);
  return useMutation({
    mutationFn: () => gqlRequest(SimHoleDocument, { id }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

export function useSimRound(id: string) {
  const invalidate = useInvalidatePlay(id);
  return useMutation({
    mutationFn: () => gqlRequest(SimRoundDocument, { id }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

export function useSimEvent(id: string) {
  const invalidate = useInvalidatePlay(id);
  return useMutation({
    mutationFn: () => gqlRequest(SimEventDocument, { id }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

export function useCompleteEvent(id: string) {
  const invalidate = useInvalidatePlay(id);
  return useMutation({
    mutationFn: () => gqlRequest(CompleteEventDocument, { id }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}
