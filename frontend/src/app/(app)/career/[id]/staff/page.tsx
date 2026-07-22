import { StaffLanding } from "@/components/manage/staff-landing";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function StaffPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <StaffLanding id={id} />;
}
