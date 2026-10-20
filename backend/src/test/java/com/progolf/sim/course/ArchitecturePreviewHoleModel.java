package com.progolf.sim.course;

import com.progolf.sim.shot.HoleModel;
import com.progolf.sim.shot.Strategy;
import com.progolf.sim.spatial.ShotZoneProfile;

/** Test/export-only adapter that supplies ephemeral preview intent to the shared strategy evaluator. */
final class ArchitecturePreviewHoleModel implements HoleModel {
    private final HoleModel delegate;
    private final ArchitectureStrategyPlan strategy;

    ArchitecturePreviewHoleModel(HoleModel delegate, ArchitectureStrategyPlan strategy) {
        this.delegate = delegate;
        this.strategy = strategy;
    }

    @Override public double startDistance() { return delegate.startDistance(); }
    @Override public ShotZoneProfile zoneProfileFor(double remainingDistance) { return delegate.zoneProfileFor(remainingDistance); }
    @Override public int par() { return delegate.par(); }
    @Override public double pinLateral() { return delegate.pinLateral(); }
    @Override public CourseGeometry geometry() { return delegate.geometry(); }
    @Override public Position2d cupPosition() { return delegate.cupPosition(); }
    @Override public Position2d progressionTarget(Position2d ball, Strategy policy) {
        return delegate.progressionTarget(ball, policy);
    }
    @Override public HoleSpatialPlan strategicLandingPlan() { return delegate.strategicLandingPlan(); }
    @Override public ArchitectureStrategyPlan architectureStrategyPlan() { return strategy; }
}
