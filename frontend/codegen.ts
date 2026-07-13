import type { CodegenConfig } from "@graphql-codegen/cli";

/*
 * Generates typed GraphQL operations from the backend schema (the source of truth).
 * Output lands in src/lib/graphql/generated/ (gitignored) and is consumed via the
 * generated `graphql()` function. Re-run with `pnpm codegen`; runs automatically
 * before dev/build so types never drift from the schema.
 *
 * The monorepo reads the checked-in schema file directly. A future deployment could
 * switch `schema` to an introspection URL without changing any operation code.
 */
const config: CodegenConfig = {
  schema: "../backend/src/main/resources/graphql/schema.graphqls",
  documents: ["src/**/*.{ts,tsx}", "!src/lib/graphql/generated/**"],
  ignoreNoDocuments: true,
  generates: {
    "./src/lib/graphql/generated/": {
      preset: "client",
      config: {
        useTypeImports: true,
        // Long is a 64-bit integer scalar; represent as string to avoid JS precision loss.
        scalars: { Long: "string" },
      },
    },
  },
};

export default config;
