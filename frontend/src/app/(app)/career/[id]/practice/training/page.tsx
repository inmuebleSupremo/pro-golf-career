import { CareerTraining } from "@/components/play/career-training";

export const dynamic = "force-dynamic";

export default async function CareerTrainingPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <CareerTraining id={id} />;
}
