import { FitnessView } from "@/components/career/views/fitness-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function FitnessPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <FitnessView id={id} />;
}
