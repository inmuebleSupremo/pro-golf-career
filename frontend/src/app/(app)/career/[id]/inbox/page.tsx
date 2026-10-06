import { InboxView } from "@/components/career/views/inbox-view";

export const dynamic = "force-dynamic";

export default async function InboxPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <InboxView id={id} />;
}
