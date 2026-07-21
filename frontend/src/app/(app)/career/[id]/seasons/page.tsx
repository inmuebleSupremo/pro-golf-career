import { SeasonsView } from "@/components/career/views/seasons-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function SeasonsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <SeasonsView id={id} />;
}
