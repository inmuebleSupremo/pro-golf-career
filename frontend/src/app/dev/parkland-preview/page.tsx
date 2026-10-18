import { notFound } from "next/navigation";

import { ParklandRenderPreview } from "@/components/play/parkland-render-preview";

/** Development-only visual evidence for the canonical renderer; never available in production. */
export default function ParklandPreviewPage() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <ParklandRenderPreview />;
}
