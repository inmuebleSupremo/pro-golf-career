import { notFound } from "next/navigation";

import { LinksRenderPreview } from "@/components/play/links-render-preview";

/** Development-only Links visual evidence. */
export default function LinksPreviewPage() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <LinksRenderPreview />;
}
