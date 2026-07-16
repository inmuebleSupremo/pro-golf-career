"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { gqlRequest } from "@/lib/api/graphql-client";
import type { ShotDecisionInput } from "@/lib/graphql/generated/graphql";
import {
  AdvanceWeekDocument,
  CompleteEventDocument,
  PlayShotDocument,
  PlayStateDocument,
  SimEventDocument,
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

/** Invalidate the play state and the career overview after a play mutation. */
function useInvalidatePlay(id: string) {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ["play", id] });
    queryClient.invalidateQueries({ queryKey: ["career", id] });
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

export function usePlayShot(id: string) {
  const invalidate = useInvalidatePlay(id);
  return useMutation({
    mutationFn: (decision: ShotDecisionInput) => gqlRequest(PlayShotDocument, { id, decision }),
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
