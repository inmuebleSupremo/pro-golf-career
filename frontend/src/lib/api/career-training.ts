"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { gqlRequest } from "@/lib/api/graphql-client";
import type { CareerTrainingQuery } from "@/lib/graphql/generated/graphql";
import {
  AbandonPlayerTrainingDocument,
  CareerTrainingDocument,
  CommitCareerPreparationWithResultDocument,
  SetStandingPreparationFocusDocument,
  StartPlayerTrainingDocument,
} from "@/lib/graphql/operations";

const key = (id: string) => ["career-training", id] as const;
export type PlayableSession = NonNullable<CareerTrainingQuery["playerTrainingSession"]>;
export type TrainingChallenge = PlayableSession["challenges"][number];
export type TrainingScene = {
  session: PlayableSession;
  challenge: TrainingChallenge;
  briefingActive?: boolean;
  challengePlayable?: boolean;
  onReconcile: () => Promise<PlayableSession | null>;
  onAdvance: (number: number) => void;
  onReview: () => void;
  onAbandon: () => void;
};

export function useCareerTraining(id: string) {
  return useQuery({
    queryKey: key(id),
    queryFn: () => gqlRequest(CareerTrainingDocument, { id }),
    networkMode: "always",
    retry: false,
    refetchOnMount: "always",
  });
}

export function useSetStandingPreparationFocus(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (focus: string) =>
      (await gqlRequest(SetStandingPreparationFocusDocument, { id, focus })).setStandingPreparationFocus,
    networkMode: "always",
    onSuccess: () => queryClient.invalidateQueries({ queryKey: key(id) }),
  });
}

export function useCommitCareerPreparation(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (focus: string) =>
      (await gqlRequest(CommitCareerPreparationWithResultDocument, { id, focus })).commitCareerPreparationWithResult,
    networkMode: "always",
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: key(id) });
      queryClient.invalidateQueries({ queryKey: ["profile", id] });
      queryClient.invalidateQueries({ queryKey: ["fitness", id] });
      queryClient.invalidateQueries({ queryKey: ["development", id] });
      queryClient.invalidateQueries({ queryKey: ["development-report", id] });
      queryClient.invalidateQueries({ queryKey: ["career", id] });
    },
  });
}

export function useStartPlayerTraining(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (focus: string) =>
      (await gqlRequest(StartPlayerTrainingDocument, { id, focus })).startPlayerTraining,
    networkMode: "always",
    onSettled: () => queryClient.invalidateQueries({ queryKey: key(id) }),
  });
}

export function useAbandonPlayerTraining(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (trainingId: string) =>
      (await gqlRequest(AbandonPlayerTrainingDocument, { id, trainingId })).abandonPlayerTraining,
    networkMode: "always",
    onSettled: () => queryClient.invalidateQueries({ queryKey: key(id) }),
  });
}
