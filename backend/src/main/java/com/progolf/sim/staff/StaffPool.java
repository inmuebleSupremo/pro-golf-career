package com.progolf.sim.staff;

import com.progolf.sim.core.Rng;
import com.progolf.sim.core.Seeds;
import com.progolf.sim.core.SplitMix64Rng;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A world's persistent pool of hireable staff (spec: support-team). Generated once — a fixed set of named
 * profiles across every role — and thereafter mutated only by hiring (a hired member leaves the pool for
 * good). Each season the world surfaces a rotating, variety-weighted subset as the player's offers. The pool
 * is captured in the world snapshot (the available profiles) so it round-trips exactly.
 */
public final class StaffPool {

    private final List<StaffMember> available;

    private StaffPool(List<StaffMember> members) {
        this.available = new ArrayList<>(members);
    }

    /** Generates a fresh pool: {@code perRole} deterministic profiles for each role. */
    public static StaffPool generate(long seed, StaffMarket market, int perRole) {
        List<StaffMember> members = new ArrayList<>();
        for (StaffRole role : StaffRole.values()) {
            for (int i = 0; i < perRole; i++) {
                Rng rng = new SplitMix64Rng(Seeds.deriveSeed(Seeds.deriveSeed(seed, role.ordinal()), i));
                members.add(market.generate(role, rng));
            }
        }
        return new StaffPool(members);
    }

    /** Restores a pool from its captured available profiles (spec: world-snapshot). */
    public static StaffPool restore(List<StaffMember> available) {
        return new StaffPool(available);
    }

    /** The profiles still available to hire. */
    public List<StaffMember> available() {
        return List.copyOf(available);
    }

    /** Removes a hired member from the pool (no-op if not present). */
    public void remove(StaffMember member) {
        available.remove(member);
    }

    /**
     * A variety-weighted offer of up to {@code count} available profiles limited to {@code allowedRoles}.
     * Candidates are grouped by role, each group deterministically shuffled (so the set rotates season to
     * season), then drawn round-robin across roles so no single role dominates the offer. Deterministic in
     * {@code rng}.
     */
    public List<StaffMember> offer(Set<StaffRole> allowedRoles, int count, Rng rng) {
        Map<StaffRole, List<StaffMember>> byRole = new EnumMap<>(StaffRole.class);
        for (StaffMember m : available) {
            if (allowedRoles.contains(m.role())) {
                byRole.computeIfAbsent(m.role(), k -> new ArrayList<>()).add(m);
            }
        }
        for (List<StaffMember> group : byRole.values()) {
            shuffle(group, rng);
        }
        List<StaffRole> roles = new ArrayList<>(byRole.keySet());
        shuffle(roles, rng); // rotate which roles lead the round-robin

        List<StaffMember> offered = new ArrayList<>();
        for (int depth = 0; offered.size() < count; depth++) {
            boolean tookAny = false;
            for (StaffRole role : roles) {
                List<StaffMember> group = byRole.get(role);
                if (group.size() > depth) {
                    offered.add(group.get(depth));
                    tookAny = true;
                    if (offered.size() >= count) {
                        break;
                    }
                }
            }
            if (!tookAny) {
                break; // pool exhausted for the allowed roles
            }
        }
        return offered;
    }

    /** Deterministic Fisher–Yates shuffle over the supplied Rng. */
    private static <T> void shuffle(List<T> list, Rng rng) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = (int) (rng.nextDouble() * (i + 1));
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }
}
