import { print } from "graphql";
import type { TypedDocumentNode } from "@graphql-typed-document-node/core";

/*
 * Browser-side GraphQL client. Targets the same-origin BFF (/api/graphql), which
 * attaches the access token server-side and forwards to the backend. The browser
 * therefore never calls the cross-origin backend and never handles tokens.
 */

/** Thrown when a GraphQL request fails; carries the HTTP status and error classification. */
export class GraphQLRequestError extends Error {
  readonly status: number;
  readonly classification?: string;
  constructor(message: string, status: number, classification?: string) {
    super(message);
    this.name = "GraphQLRequestError";
    this.status = status;
    this.classification = classification;
  }
}

export async function gqlRequest<TResult, TVariables extends object>(
  document: TypedDocumentNode<TResult, TVariables>,
  variables?: TVariables,
): Promise<TResult> {
  const res = await fetch("/api/graphql", {
    method: "POST",
    headers: { "content-type": "application/json" },
    credentials: "same-origin",
    body: JSON.stringify({ query: print(document), variables }),
  });

  const json = (await res.json().catch(() => null)) as {
    data?: TResult;
    errors?: Array<{ message: string; extensions?: { classification?: string } }>;
  } | null;

  if (!res.ok || json?.errors?.length) {
    const first = json?.errors?.[0];
    throw new GraphQLRequestError(
      first?.message ?? "Request failed.",
      res.status,
      first?.extensions?.classification,
    );
  }

  return json!.data as TResult;
}

/** True when a request failed because the session is not (or no longer) valid. */
export function isUnauthorized(error: unknown): boolean {
  return error instanceof GraphQLRequestError && error.status === 401;
}

/** True when a request failed because the target resource does not exist (e.g. an expired session). */
export function isNotFound(error: unknown): boolean {
  return error instanceof GraphQLRequestError && error.classification === "NOT_FOUND";
}
