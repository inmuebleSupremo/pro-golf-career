import { notFound } from "next/navigation";

import { ArchitectureRefinementPreview } from "@/components/play/architecture-refinement-preview";

/** Development-only visual review of canonical V5-derived geometry. */
export default function ArchitecturePreviewPage() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <ArchitectureRefinementPreview />;
}
