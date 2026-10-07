"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { gqlRequest } from "@/lib/api/graphql-client";
import type { BallStrikeIntentInput } from "@/lib/graphql/generated/graphql";
import {
  AdvanceSeasonDocument,
  AdvanceWeekDocument,
  CompleteEventDocument,
  PlayingHoleDocument,
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
 * moves ranking, earnings, standings, offers and history at once, so every session-scoped query is
 * refetched rather than trying to enumerate what changed.
 */
export function useAdvanceSeason(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => gqlRequest(AdvanceSeasonDocument, { id }),
    networkMode: "always",
    onSuccess: () => queryClient.invalidateQueries(),
  });
}

export function usePlayShot(id: string) {
  const invalidate = useInvalidatePlay(id);
  return useMutation({
    mutationFn: (intent: BallStrikeIntentInput) => gqlRequest(PlayShotDocument, { id, intent }),
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
