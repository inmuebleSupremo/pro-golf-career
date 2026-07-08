package com.progolf.sim.player;

/**
 * Who makes a golfer's decisions (REQ-114). Assigned at creation and immutable. Control Type determines
 * decision-making only; it SHALL NOT be read by any gameplay-rule calculation — human and simulation
 * golfers share identical systems (REQ-104/110/123).
 */
public enum ControlType {
    HUMAN,
    SIMULATION
}
