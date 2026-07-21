import { FinancesView } from "@/components/career/views/finances-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function FinancesPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <FinancesView id={id} />;
}
