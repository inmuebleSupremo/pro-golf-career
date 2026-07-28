"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { gqlRequest } from "@/lib/api/graphql-client";
import {
  AcceptEquipmentDealDocument,
  AcceptSponsorshipDocument,
  BuyEquipmentDocument,
  EnterEventDocument,
  EquipmentDocument,
  HireStaffDocument,
  PendingSponsorshipsDocument,
  PendingStaffDocument,
  PlayerScheduleDocument,
  PlayerStaffDocument,
  ReleaseStaffDocument,
  SponsorshipStatusDocument,
  SelectLoadoutItemDocument,
  SetDevelopmentFocusDocument,
  SetRestingDocument,
  SkipEventDocument,
  SpendDevelopmentPointsDocument,
} from "@/lib/graphql/operations";

/** The player's reviewable schedule — the manage view's editable source of truth. */
export function usePlayerSchedule(id: string) {
  return useQuery({
    queryKey: ["schedule", id],
    queryFn: () => gqlRequest(PlayerScheduleDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

/** Invalidate the schedule, calendar, and career overview after a management mutation. */
function useInvalidateManage(id: string) {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ["schedule", id] });
    queryClient.invalidateQueries({ queryKey: ["calendar", id] });
    queryClient.invalidateQueries({ queryKey: ["career", id] });
  };
}

export function useSkipEvent(id: string) {
  const invalidate = useInvalidateManage(id);
  return useMutation({
    mutationFn: (tournamentId: string) => gqlRequest(SkipEventDocument, { id, tournamentId }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

export function useEnterEvent(id: string) {
  const invalidate = useInvalidateManage(id);
  return useMutation({
    mutationFn: (tournamentId: string) => gqlRequest(EnterEventDocument, { id, tournamentId }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

/**
 * Rest (or un-rest) the golfer for the rest of the season. Write-only on the backend —
 * no query reflects the persisted flag — so callers track the intended state locally.
 */
export function useSetResting(id: string) {
  const invalidate = useInvalidateManage(id);
  return useMutation({
    mutationFn: (resting: boolean) => gqlRequest(SetRestingDocument, { id, resting }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

/**
 * Set the development focus (an ordered list of Attribute enum names). Write-only and
 * affects future training only — nothing observable changes now — so there is no
 * query to invalidate; callers track the chosen order locally.
 */
export function useSetDevelopmentFocus(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (focus: string[]) => gqlRequest(SetDevelopmentFocusDocument, { id, focus }),
    networkMode: "always",
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["focus", id] }),
  });
}

/** Spends banked Development Points to raise attributes; refreshes the balance, profile, and report. */
export function useSpendDevelopmentPoints(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (raises: { attribute: string; points: number }[]) =>
      gqlRequest(SpendDevelopmentPointsDocument, { id, raises }),
    networkMode: "always",
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["development", id] });
      queryClient.invalidateQueries({ queryKey: ["profile", id] });
      queryClient.invalidateQueries({ queryKey: ["development-report", id] });
    },
  });
}

/** The player's loadout, owned bag, and pending upgrade offers, in one request. */
export function useEquipment(id: string) {
  return useQuery({
    queryKey: ["equipment", id],
    queryFn: () => gqlRequest(EquipmentDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

/** Invalidate equipment (bag/loadout/offers) and the profile (funds change on purchase). */
function useInvalidateEquipment(id: string) {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ["equipment", id] });
    queryClient.invalidateQueries({ queryKey: ["profile", id] });
  };
}

export function useBuyEquipment(id: string) {
  const invalidate = useInvalidateEquipment(id);
  return useMutation({
    mutationFn: (index: number) => gqlRequest(BuyEquipmentDocument, { id, index }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

/** Sign a brand deal: refreshes equipment (deals/bag/loadout) and the profile (signing bonus credits funds). */
export function useAcceptEquipmentDeal(id: string) {
  const invalidate = useInvalidateEquipment(id);
  return useMutation({
    mutationFn: (index: number) => gqlRequest(AcceptEquipmentDealDocument, { id, index }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

export function useSelectLoadoutItem(id: string) {
  const invalidate = useInvalidateEquipment(id);
  return useMutation({
    mutationFn: (item: { category: string; name: string }) =>
      gqlRequest(SelectLoadoutItemDocument, { id, category: item.category, name: item.name }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

/** The player's pending staff candidates awaiting a hire decision (Hire Staff page). */
export function usePendingStaff(id: string) {
  return useQuery({
    queryKey: ["staff", id],
    queryFn: () => gqlRequest(PendingStaffDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

/** The player's current support-team roster (Manage Staff page). */
export function usePlayerStaff(id: string) {
  return useQuery({
    queryKey: ["roster", id],
    queryFn: () => gqlRequest(PlayerStaffDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

function useInvalidateStaff(id: string) {
  const queryClient = useQueryClient();
  return () => {
    // Hiring/releasing changes the roster, the pending offers, and (for a hire) available funds.
    queryClient.invalidateQueries({ queryKey: ["staff", id] });
    queryClient.invalidateQueries({ queryKey: ["roster", id] });
    queryClient.invalidateQueries({ queryKey: ["profile", id] });
  };
}

export function useHireStaff(id: string) {
  const invalidate = useInvalidateStaff(id);
  return useMutation({
    mutationFn: (index: number) => gqlRequest(HireStaffDocument, { id, index }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

export function useReleaseStaff(id: string) {
  const invalidate = useInvalidateStaff(id);
  return useMutation({
    mutationFn: (role: string) => gqlRequest(ReleaseStaffDocument, { id, role }),
    networkMode: "always",
    onSuccess: invalidate,
  });
}

/** The player's pending sponsorship offers awaiting a decision. */
export function usePendingSponsorships(id: string) {
  return useQuery({
    queryKey: ["sponsorships", id],
    queryFn: () => gqlRequest(PendingSponsorshipsDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

/** The player's sponsorship book — the concurrency cap and currently-active signed agreements. */
export function useSponsorshipStatus(id: string) {
  return useQuery({
    queryKey: ["sponsorship-status", id],
    queryFn: () => gqlRequest(SponsorshipStatusDocument, { id }),
    networkMode: "always",
    retry: false,
  });
}

export function useAcceptSponsorship(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (index: number) => gqlRequest(AcceptSponsorshipDocument, { id, index }),
    networkMode: "always",
    onSuccess: () => {
      // The accepted offer leaves the pending list, the signing bonus credits funds, and the book grows.
      queryClient.invalidateQueries({ queryKey: ["sponsorships", id] });
      queryClient.invalidateQueries({ queryKey: ["sponsorship-status", id] });
      queryClient.invalidateQueries({ queryKey: ["profile", id] });
    },
  });
}
