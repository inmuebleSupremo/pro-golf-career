import { CareerHub } from "@/components/career/career-hub";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function CareerPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <CareerHub id={id} />;
}
