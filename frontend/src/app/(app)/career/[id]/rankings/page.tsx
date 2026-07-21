import { RankingsView } from "@/components/career/views/rankings-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function RankingsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <RankingsView id={id} />;
}
