import { StaffView } from "@/components/manage/staff-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function ManageStaffPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <StaffView id={id} />;
}
