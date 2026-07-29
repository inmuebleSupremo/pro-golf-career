package com.progolf.app.api.dto;

import java.util.List;

/**
 * One distance band of a shot's reachable surface profile (capability graphql-api, spec: web-hole-visualization):
 * the half-open carry interval {@code [startDistance, endDistance)} yards, partitioned laterally into ordered
 * {@code regions} from the centre outward. Together the bands describe the same reachable surfaces the shot
 * engine resolves against, so the client can render a truthful shot-preview overlay. Projects
 * {@code sim.spatial.ZoneBand}.
 */
public record SurfaceBandDto(double startDistance, double endDistance, List<SurfaceRegionDto> regions) {
}
