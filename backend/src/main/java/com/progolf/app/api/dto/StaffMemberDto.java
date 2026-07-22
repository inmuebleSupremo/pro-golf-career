package com.progolf.app.api.dto;

/**
 * The GraphQL view of a staff candidate/member (capability graphql-api): the {@code role}, name, age,
 * nationality, and personality (enums as their names), the quality in [0,1], the one-off hiring cost, and the
 * seasonal salary. Projects {@code sim.staff.StaffMember}.
 */
public record StaffMemberDto(String role, String name, int age, String nationality, String personality,
                             double quality, double hiringCost, double seasonalSalary) {
}
