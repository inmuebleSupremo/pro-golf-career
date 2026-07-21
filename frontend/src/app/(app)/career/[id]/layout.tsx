import { CommandShell } from "@/components/command/command-shell";

/*
 * The career surface's persistent shell (spec: command-centre IA). The sidebar and
 * identity strip live here — mounted once — so navigating between the hub and its
 * spokes swaps only the page body. Auth is enforced by the parent (app) layout.
 */
export default async function CareerLayout({
  children,
  params,
}: {
  children: React.ReactNode;
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <CommandShell id={id}>{children}</CommandShell>;
}
