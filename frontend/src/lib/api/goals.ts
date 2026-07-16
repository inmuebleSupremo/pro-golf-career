"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";

import { gqlRequest } from "@/lib/api/graphql-client";
import type { CareerGoalInput } from "@/lib/graphql/generated/graphql";
import { SetCareerGoalsDocument } from "@/lib/graphql/operations";

/** Replace the player's self-chosen career goals, then refresh the career overview that shows them. */
export function useSetCareerGoals(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (goals: CareerGoalInput[]) => gqlRequest(SetCareerGoalsDocument, { id, goals }),
    networkMode: "always",
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["career", id] }),
  });
}
