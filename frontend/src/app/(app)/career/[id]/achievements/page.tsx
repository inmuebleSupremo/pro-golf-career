import { AchievementsView } from "@/components/career/views/achievements-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function AchievementsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <AchievementsView id={id} />;
}
