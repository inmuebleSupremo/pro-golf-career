import { PracticePuttingGreen } from "@/components/play/practice-putting-green";

export const dynamic = "force-dynamic";

export default async function PracticePuttingPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <PracticePuttingGreen id={id} />;
}
