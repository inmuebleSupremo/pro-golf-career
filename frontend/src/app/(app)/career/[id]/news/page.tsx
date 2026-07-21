import { NewsView } from "@/components/career/views/news-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function NewsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <NewsView id={id} />;
}
