import { DrivingRange } from "@/components/play/driving-range";

export const dynamic = "force-dynamic";

export default async function PracticePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <DrivingRange id={id} />;
}
