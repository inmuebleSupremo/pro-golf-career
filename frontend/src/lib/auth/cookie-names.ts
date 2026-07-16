/*
 * Cookie name + TTL constants only — no next/headers import, so this module is
 * safe to import from the edge-runtime proxy as well as route handlers.
 */
export const ACCESS_COOKIE = "pg_access";
export const REFRESH_COOKIE = "pg_refresh";

export const ACCESS_MAX_AGE = 900; // 15 min — matches backend access TTL
export const REFRESH_MAX_AGE = 1_209_600; // 14 days — matches backend refresh TTL
