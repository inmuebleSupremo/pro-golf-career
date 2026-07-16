import { PlayEvent } from "@/components/play/play-event";

// Per-session play state — never cached.
export const dynamic = "force-dynamic";

export default async function PlayPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <PlayEvent id={id} />;
}
