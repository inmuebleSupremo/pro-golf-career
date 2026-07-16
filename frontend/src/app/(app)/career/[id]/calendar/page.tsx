import { CalendarView } from "@/components/calendar/calendar-view";

// Per-session, per-user state — never cached (design D6/D1).
export const dynamic = "force-dynamic";

export default async function CalendarPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <CalendarView id={id} />;
}
