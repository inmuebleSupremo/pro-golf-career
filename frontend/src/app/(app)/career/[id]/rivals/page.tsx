import { RivalsView } from "@/components/career/views/rivals-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function RivalsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <RivalsView id={id} />;
}
