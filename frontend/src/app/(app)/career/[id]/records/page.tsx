import { RecordsView } from "@/components/career/views/records-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function RecordsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <RecordsView id={id} />;
}
