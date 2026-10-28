import { PremiumDesertPreview } from "@/components/play/premium-desert-preview";

export default function PremiumDesertPreviewPage() {
  if (process.env.NODE_ENV !== "development") return null;
  return <PremiumDesertPreview />;
}

