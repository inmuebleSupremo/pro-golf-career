import { GraphQLRequestError } from "../api/graphql-client";

/** Keeps expected player-correctable validation feedback distinct from transport/internal failures. */
export function shotSubmissionFeedback(error: unknown): string {
  if (error instanceof GraphQLRequestError && error.classification === "BAD_REQUEST") {
    return error.message;
  }
  return "Unable to play that shot. Check your connection and try again.";
}
