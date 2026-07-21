import { attributeShortLabel } from "@/lib/career/labels";

type Attribute = { attribute: string; value: number };

/*
 * A self-contained SVG radar (spider) chart of a golfer's attributes — no chart library.
 * Values are 0–100; the polygon area is the golfer's shape at a glance. Colours come from
 * the theme via Tailwind fill/stroke utilities, so it tracks light/dark like everything else.
 */
export function AttributeRadar({ attributes }: { attributes: Attribute[] }) {
  const size = 300;
  const center = size / 2;
  const maxRadius = 96;
  const n = attributes.length;
  if (n < 3) return null;

  const angleOf = (i: number) => -Math.PI / 2 + (i * 2 * Math.PI) / n;
  const pointAt = (i: number, radius: number): [number, number] => [
    center + radius * Math.cos(angleOf(i)),
    center + radius * Math.sin(angleOf(i)),
  ];
  const polygon = (radius: number) =>
    attributes.map((_, i) => pointAt(i, radius).map((v) => v.toFixed(1)).join(",")).join(" ");

  const rings = [0.25, 0.5, 0.75, 1];
  const dataPoints = attributes.map((a, i) =>
    pointAt(i, (maxRadius * clamp(a.value)) / 100),
  );

  return (
    <svg
      viewBox={`0 0 ${size} ${size}`}
      className="w-full max-w-[320px] overflow-visible"
      role="img"
      aria-label="Attribute radar chart"
    >
      {/* Concentric grid rings */}
      {rings.map((r) => (
        <polygon
          key={r}
          points={polygon(maxRadius * r)}
          className="fill-none stroke-border"
          strokeWidth={1}
        />
      ))}

      {/* Radial spokes */}
      {attributes.map((_, i) => {
        const [x, y] = pointAt(i, maxRadius);
        return (
          <line key={i} x1={center} y1={center} x2={x} y2={y} className="stroke-border" strokeWidth={1} />
        );
      })}

      {/* The golfer's shape */}
      <polygon
        points={dataPoints.map((p) => p.map((v) => v.toFixed(1)).join(",")).join(" ")}
        className="fill-primary/20 stroke-primary"
        strokeWidth={2}
        strokeLinejoin="round"
      />
      {dataPoints.map((p, i) => (
        <circle key={i} cx={p[0]} cy={p[1]} r={2.5} className="fill-primary" />
      ))}

      {/* Axis labels */}
      {attributes.map((a, i) => {
        const [x, y] = pointAt(i, maxRadius + 16);
        const cos = Math.cos(angleOf(i));
        const anchor = cos > 0.3 ? "start" : cos < -0.3 ? "end" : "middle";
        return (
          <text
            key={a.attribute}
            x={x}
            y={y}
            textAnchor={anchor}
            dominantBaseline="middle"
            className="fill-muted-foreground text-[9px] font-medium"
          >
            {attributeShortLabel(a.attribute)}
          </text>
        );
      })}
    </svg>
  );
}

function clamp(value: number): number {
  return Math.max(0, Math.min(100, value));
}
