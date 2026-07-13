import { graphql } from "./generated";

/*
 * Typed GraphQL operations. Authored against the generated `graphql()` function so
 * every field is checked against the backend schema at build time (schema drift
 * fails type-check rather than surfacing at runtime).
 */

/** The player's stored saved games, newest first. Proves the data-access layer. */
export const ListSavesDocument = graphql(`
  query ListSaves {
    listSaves {
      saveId
      savedAt
      season
      week
      playerGolferId
    }
  }
`);
