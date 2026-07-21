import { HallOfFameView } from "@/components/career/views/hall-of-fame-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function HallOfFamePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <HallOfFameView id={id} />;
}
