"use client";

import { Trophy } from "lucide-react";

import { SpokeShell, SpokeEmpty, useSpokeGate } from "@/components/career/spoke";
import { usePlayerProfile, useRecords } from "@/lib/api/queries";
import { recordTypeLabel, recordValueText } from "@/lib/career/labels";

type Record = {
  type: string;
  holderGolferId: string;
  holderName: string;
  value: number;
  season: number;
};

/** Records spoke: the world Record Book, the player's own held records highlighted. */
export function RecordsView({ id }: { id: string }) {
  const query = useRecords(id);
  const playerGolferId = usePlayerProfile(id).data?.playerProfile?.golferId ?? null;
  const gate = useSpokeGate(query);
  if (gate) return gate;

  const records: Record[] = query.data?.records ?? [];

  return (
    <SpokeShell title="Records" description="The marks to beat across the professional game.">
      {records.length === 0 ? (
        <SpokeEmpty>No records set yet — they’ll appear as milestones are reached.</SpokeEmpty>
      ) : (
        <ul className="divide-divider border-border bg-surface flex flex-col divide-y overflow-hidden rounded-lg border">
          {records.map((record) => (
            <RecordRow
              key={record.type}
              record={record}
              isPlayer={playerGolferId != null && record.holderGolferId === playerGolferId}
            />
          ))}
        </ul>
      )}
    </SpokeShell>
  );
}

function RecordRow({ record, isPlayer }: { record: Record; isPlayer: boolean }) {
  return (
    <li className={`flex items-center justify-between gap-4 px-5 py-4 ${isPlayer ? "bg-primary/[0.06]" : ""}`}>
      <div className="flex min-w-0 flex-col gap-1">
        <span className="text-subtle-foreground text-xs tracking-[0.08em] uppercase">
          {recordTypeLabel(record.type)}
        </span>
        <span className="flex items-center gap-2">
          <span className={`truncate font-medium ${isPlayer ? "text-primary" : ""}`}>
            {record.holderName}
          </span>
          {isPlayer ? (
            <span className="bg-gold/[0.14] text-gold inline-flex shrink-0 items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium">
              <Trophy className="size-3" aria-hidden="true" />
              You
            </span>
          ) : null}
        </span>
        <span className="text-muted-foreground text-sm">Set in Season {record.season}</span>
      </div>
      <span className="text-foreground shrink-0 font-mono text-lg tabular-nums">
        {recordValueText(record.type, record.value)}
      </span>
    </li>
  );
}
