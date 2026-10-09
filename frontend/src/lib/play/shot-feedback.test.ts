import { describe, expect, it } from "vitest";

import { GraphQLRequestError } from "../api/graphql-client";
import { shotSubmissionFeedback } from "./shot-feedback";

describe("shotSubmissionFeedback", () => {
  it("keeps expected server validation feedback actionable", () => {
    expect(shotSubmissionFeedback(new GraphQLRequestError(
      "aim point is outside the planning envelope", 400, "BAD_REQUEST",
    ))).toBe("aim point is outside the planning envelope");
  });

  it("does not expose unexpected GraphQL failures", () => {
    expect(shotSubmissionFeedback(new GraphQLRequestError("database detail", 500, "INTERNAL_ERROR")))
      .toBe("Unable to play that shot. Check your connection and try again.");
  });
});
