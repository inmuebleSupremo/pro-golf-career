import { CoastalLinksPreview } from "@/components/play/coastal-links-preview";

export default async function CoastalLinksPreviewPage({ searchParams }: {
  searchParams: Promise<{ hole?: string | string[] }>;
}) {
  if (process.env.NODE_ENV !== "development") return null;
  const number = Number((await searchParams).hole);
  return <CoastalLinksPreview initialHole={Number.isInteger(number) && number >= 1 && number <= 18 ? number : 7} />;
}
