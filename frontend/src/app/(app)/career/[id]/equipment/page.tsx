import { EquipmentView } from "@/components/manage/equipment-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function EquipmentPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <EquipmentView id={id} />;
}
