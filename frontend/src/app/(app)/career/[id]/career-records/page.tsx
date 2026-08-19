import { CareerRecordsView } from "@/components/career/views/career-records-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function CareerRecordsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <CareerRecordsView id={id} />;
}
