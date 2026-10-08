import { notFound } from "next/navigation";

import { WindDisplayPreview } from "@/components/play/wind-display-preview";

/** This route is deliberately omitted from production behaviour. */
export default function WindPreviewPage() {
  if (process.env.NODE_ENV !== "development") notFound();
  return <WindDisplayPreview />;
}
