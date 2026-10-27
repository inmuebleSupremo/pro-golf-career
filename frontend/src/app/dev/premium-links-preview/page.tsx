import { PremiumLinksPreview } from "@/components/play/premium-links-preview";

export default function PremiumLinksPreviewPage() {
  if (process.env.NODE_ENV !== "development") return null;
  return <PremiumLinksPreview />;
}
