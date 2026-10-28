import { PremiumHeathlandPreview } from "@/components/play/premium-heathland-preview";

export default function PremiumHeathlandPreviewPage() {
  if (process.env.NODE_ENV !== "development") return null;
  return <PremiumHeathlandPreview />;
}
