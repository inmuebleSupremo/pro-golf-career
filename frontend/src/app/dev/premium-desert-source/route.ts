import { readFile } from "node:fs/promises";
import { resolve } from "node:path";

/** Development-only access to the exact approved SVG; the source file is never rewritten. */
export async function GET() {
  if (process.env.NODE_ENV !== "development") return new Response(null, { status: 404 });
  const source = resolve(process.cwd(), "../docs/course-holes/prototypes/premium-desert-vector-reference/premium_desert_vector.svg");
  return new Response(await readFile(source), { headers: { "Content-Type": "image/svg+xml; charset=utf-8" } });
}

