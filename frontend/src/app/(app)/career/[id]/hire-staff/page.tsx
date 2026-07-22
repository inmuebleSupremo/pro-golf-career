import { HireStaffView } from "@/components/manage/hire-staff-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function HireStaffPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <HireStaffView id={id} />;
}
