import { notFound } from "next/navigation";

import { PremiumParklandPreview } from "@/components/play/premium-parkland-preview";

export default async function Page({ searchParams }: {
  searchParams: Promise<{ narrow?: string; unlit?: string; noMottle?: string; noPath?: string }>;
}) {
  if (process.env.NODE_ENV !== "development") notFound();
  const params = await searchParams;
  return <PremiumParklandPreview initialNarrow={params.narrow === "1"} unlitAudit={params.unlit === "1"}
    noMottleAudit={params.noMottle === "1"} noPathAudit={params.noPath === "1"} />;
}
