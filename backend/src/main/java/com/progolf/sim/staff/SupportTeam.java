package com.progolf.sim.staff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A Professional Golfer's Support Team (spec: support-team / staff-relationships, REQ-191/193/196): at most
 * one {@link StaffMember} per {@link StaffRole} (the current team) plus an append-only history of every
 * {@link StaffRelationship}. Hiring and releasing update the current team and the history together, so the
 * current team is always accurate and changing staff never invalidates history.
 */
public final class SupportTeam {

    private final Map<StaffRole, StaffMember> current = new EnumMap<>(StaffRole.class);
    private final List<StaffRelationship> history = new ArrayList<>();

    /** Hires a member into their role (replacing and closing any incumbent), recording the appointment. */
    public void hire(StaffMember member, int season) {
        Objects.requireNonNull(member, "member");
        if (current.containsKey(member.role())) {
            closeOpenRelationship(member.role(), season);
        }
        current.put(member.role(), member);
        history.add(StaffRelationship.opened(member, season));
    }

    /** Releases the member in a role (if any), closing the relationship but preserving its history. */
    public void release(StaffRole role, int season) {
        if (current.remove(role) != null) {
            closeOpenRelationship(role, season);
        }
    }

    private void closeOpenRelationship(StaffRole role, int season) {
        for (int i = history.size() - 1; i >= 0; i--) {
            StaffRelationship r = history.get(i);
            if (r.role() == role && r.isActive()) {
                history.set(i, r.closedAt(season));
                return;
            }
        }
    }

    public boolean has(StaffRole role) {
        return current.containsKey(role);
    }

    public int size() {
        return current.size();
    }

    /** The current staff members. */
    public List<StaffMember> members() {
        return new ArrayList<>(current.values());
    }

    /** The full, append-only relationship history (REQ-193/196). */
    public List<StaffRelationship> history() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    /** The role of the current member with the highest salary, for release-under-pressure decisions. */
    public StaffRole mostExpensiveRole() {
        StaffRole costliest = null;
        double max = -1;
        for (StaffMember m : current.values()) {
            if (m.seasonalSalary() > max) {
                max = m.seasonalSalary();
                costliest = m.role();
            }
        }
        return costliest;
    }

    /** The aggregated, quality-scaled influence of the current team (REQ-197). */
    public StaffEffects effects() {
        double development = 0;
        double recovery = 0;
        double mental = 0;
        double strategic = 0;
        for (StaffMember m : current.values()) {
            switch (m.role()) {
                case COACH -> development += StaffConstants.COACH_DEVELOPMENT_PER_QUALITY * m.quality();
                case FITNESS_COACH -> recovery += StaffConstants.FITNESS_RECOVERY_PER_QUALITY * m.quality();
                case PHYSIOTHERAPIST -> recovery += StaffConstants.PHYSIO_RECOVERY_PER_QUALITY * m.quality();
                case SPORTS_PSYCHOLOGIST -> mental += StaffConstants.PSYCH_MENTAL_PER_QUALITY * m.quality();
                case CADDIE -> strategic += StaffConstants.CADDIE_STRATEGIC_PER_QUALITY * m.quality();
            }
        }
        return new StaffEffects(development, recovery, mental, strategic);
    }
}
