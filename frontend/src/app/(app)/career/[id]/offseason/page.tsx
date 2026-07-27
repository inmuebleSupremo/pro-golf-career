import { OffSeasonReview } from "@/components/offseason/off-season-review";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function OffSeasonPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <OffSeasonReview id={id} />;
}
