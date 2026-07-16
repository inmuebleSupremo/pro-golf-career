"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";

import { gqlRequest } from "@/lib/api/graphql-client";
import {
  CreatePlayerDocument,
  CreateWorldDocument,
  LoadCareerDocument,
  SaveGameDocument,
} from "@/lib/graphql/operations";

export interface CreateCareerInput {
  firstName: string;
  lastName: string;
  nationality: string;
  archetype: string;
  startAge: number;
}

export interface CreatedGolfer extends CreateCareerInput {
  golferId: string;
  saveId: string;
}

/** A positive integer world seed rendered as a string (the Long scalar maps to string). */
function randomSeed(): string {
  return String(Math.floor(Math.random() * Number.MAX_SAFE_INTEGER));
}

/*
 * Orchestrates career creation: createWorld → createPlayer → save, in sequence,
 * through the same-origin BFF (design D1). Each step is typed against the schema.
 * Partial failure is safe: nothing persists until `save`, so an aborted run leaves
 * no career (a fresh world is created per submission, so no "already assigned").
 */
export function useCreateCareer() {
  const queryClient = useQueryClient();

  return useMutation<CreatedGolfer, Error, CreateCareerInput>({
    mutationFn: async (input) => {
      const { createWorld } = await gqlRequest(CreateWorldDocument, { seed: randomSeed() });
      const worldId = createWorld.id;

      const { createPlayer } = await gqlRequest(CreatePlayerDocument, {
        id: worldId,
        firstName: input.firstName,
        lastName: input.lastName,
        nationality: input.nationality,
        startAge: input.startAge,
        archetype: input.archetype,
      });

      const saveId = `career-${crypto.randomUUID()}`;
      await gqlRequest(SaveGameDocument, { id: worldId, saveId });

      return { ...input, golferId: createPlayer, saveId };
    },
    onSuccess: () => {
      // The new career should show when the player returns to their saves.
      queryClient.invalidateQueries({ queryKey: ["saves"] });
    },
  });
}

/**
 * Loads a save into a new session and returns that session's id. The caller
 * navigates to the career hub for the returned id (each load makes a fresh
 * ephemeral session; the save is the durable handle — design D1).
 */
export function useLoadCareer() {
  return useMutation<string, Error, string>({
    mutationFn: async (saveId) => {
      const { load } = await gqlRequest(LoadCareerDocument, { saveId });
      return load.id;
    },
  });
}
