import { afterEach, beforeEach, expect, it, vi } from "vitest";

import { POST } from "./route";

const backend = vi.hoisted(() => ({
  graphql: vi.fn(),
  refresh: vi.fn(),
  readCookies: vi.fn(),
  setAccess: vi.fn(),
  clearCookies: vi.fn(),
}));

vi.mock("@/lib/auth/backend", () => ({
  backendGraphql: backend.graphql,
  backendRefresh: backend.refresh,
}));
vi.mock("@/lib/auth/cookies", () => ({
  readAuthCookies: backend.readCookies,
  setAccessCookie: backend.setAccess,
  clearAuthCookies: backend.clearCookies,
}));

function request(query = "mutation AdvanceSeason($id: ID!) { advanceSeason(id: $id) { state } }") {
  return new Request("http://localhost/api/graphql", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ query, variables: { id: "private-session-id" } }),
  });
}

let warning: ReturnType<typeof vi.spyOn>;

beforeEach(() => {
  backend.graphql.mockReset();
  backend.refresh.mockReset();
  backend.readCookies.mockReset().mockResolvedValue({ accessToken: "secret-access", refreshToken: "secret-refresh" });
  backend.setAccess.mockReset();
  backend.clearCookies.mockReset();
  warning = vi.spyOn(console, "warn").mockImplementation(() => {});
});

afterEach(() => { warning.mockRestore(); });

it("returns a controlled 504 for a confirmed nested response-header timeout", async () => {
  const cause = Object.assign(new Error("header wait"), { code: "UND_ERR_HEADERS_TIMEOUT" });
  backend.graphql.mockRejectedValue(new TypeError("fetch failed", { cause }));

  const response = await POST(request());
  expect(response.status).toBe(504);
  expect(await response.json()).toEqual({ errors: [{
    message: "The backend response timed out. The request may still be running.",
    extensions: { classification: "UPSTREAM_TIMEOUT" },
  }] });
  expect(warning).toHaveBeenCalledExactlyOnceWith("GraphQL backend transport failure", {
    operation: "AdvanceSeason", code: "UND_ERR_HEADERS_TIMEOUT", status: 504,
  });
  expect(JSON.stringify(warning.mock.calls)).not.toMatch(/secret-|private-session-id|fetch failed/);
  expect(backend.clearCookies).not.toHaveBeenCalled();
});

it("returns a controlled 502 for other backend transport failures", async () => {
  backend.graphql.mockRejectedValue(new TypeError("fetch failed", {
    cause: Object.assign(new Error("connection reset"), { code: "ECONNRESET" }),
  }));
  const response = await POST(request());
  expect(response.status).toBe(502);
  expect((await response.json()).errors[0].extensions.classification).toBe("UPSTREAM_UNAVAILABLE");
  expect(warning).toHaveBeenCalledExactlyOnceWith("GraphQL backend transport failure", {
    operation: "AdvanceSeason", code: "ECONNRESET", status: 502,
  });
});

it("passes successful GraphQL data and application errors through unchanged", async () => {
  const success = JSON.stringify({ data: { world: { season: 3 } } });
  const applicationError = JSON.stringify({ errors: [{ message: "Event pending", extensions: { classification: "BAD_REQUEST" } }] });
  backend.graphql.mockResolvedValueOnce(new Response(success, { status: 200,
    headers: { "content-type": "application/json" } }));
  backend.graphql.mockResolvedValueOnce(new Response(applicationError, { status: 200,
    headers: { "content-type": "application/json" } }));

  const first = await POST(request("query World { world(id: \"one\") { season } }"));
  const second = await POST(request());
  expect(first.status).toBe(200);
  expect(await first.text()).toBe(success);
  expect(second.status).toBe(200);
  expect(await second.text()).toBe(applicationError);
  expect(warning).not.toHaveBeenCalled();
});

it("retains the existing 401 refresh and retry flow", async () => {
  backend.graphql.mockResolvedValueOnce(new Response("", { status: 401 }));
  backend.graphql.mockResolvedValueOnce(new Response(JSON.stringify({ data: { ok: true } }), { status: 200 }));
  backend.refresh.mockResolvedValue("new-access");

  const response = await POST(request());
  expect(response.status).toBe(200);
  expect(backend.refresh).toHaveBeenCalledExactlyOnceWith("secret-refresh");
  expect(backend.graphql.mock.calls[1][0]).toBe("new-access");
  expect(backend.setAccess).toHaveBeenCalledWith(response, "new-access");
  expect(warning).not.toHaveBeenCalled();
});
