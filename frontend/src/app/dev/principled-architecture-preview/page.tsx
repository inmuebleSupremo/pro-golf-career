import { notFound } from "next/navigation";

import { PrincipledArchitecturePreview } from "@/components/play/principled-architecture-preview";

/** Development-only route for reviewing the non-persistent architecture experiment. */
export default function PrincipledArchitecturePreviewPage() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <PrincipledArchitecturePreview />;
}
