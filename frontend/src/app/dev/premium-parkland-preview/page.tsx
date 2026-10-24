import { notFound } from "next/navigation";

import { PremiumParklandPreview } from "@/components/play/premium-parkland-preview";

export default function Page() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <PremiumParklandPreview />;
}
