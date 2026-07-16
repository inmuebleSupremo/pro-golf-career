# Frontend — Next.js (pnpm). Build context: the REPOSITORY ROOT (not ./frontend),
# because graphql-codegen (run in `prebuild`) reads ../backend/.../schema.graphqls.
# From the repo root:  docker build -f frontend/Dockerfile -t pgc-frontend .
# (docker-compose sets the root context automatically.)

FROM node:22-slim AS build
RUN corepack enable
WORKDIR /app
# The codegen source, at the path the frontend expects (../backend/...).
COPY backend/src/main/resources/graphql/schema.graphqls ./backend/src/main/resources/graphql/schema.graphqls
# Install deps first (layer cache).
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./frontend/
WORKDIR /app/frontend
RUN pnpm install --frozen-lockfile
# Build: `prebuild` runs codegen, then `next build`.
WORKDIR /app
COPY frontend/ ./frontend/
WORKDIR /app/frontend
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm build

FROM node:22-slim AS run
RUN corepack enable
WORKDIR /app/frontend
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=3000
COPY --from=build /app/frontend ./
EXPOSE 3000
# next start binds 0.0.0.0 and reads BACKEND_INTERNAL_URL server-side at runtime.
CMD ["pnpm", "start"]
