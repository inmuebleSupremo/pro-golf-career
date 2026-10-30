/** One symmetric corridor-focus curve for full holes and the wider physical green camera. */
export function StageSideFocusGradient({
  id,
  shade,
  putting = false,
}: {
  id: string;
  shade: string;
  putting?: boolean;
}) {
  // A putting green occupies more of the stage than a full hole, so its clear centre is wider.
  const clearEdge = putting ? 0.35 : 0.4;
  // Full holes hold a dark outer plateau, then open quickly around the playable corridor.
  // The close-up putting camera retains its wider, gentler existing shoulders.
  const positions = putting ? [0, 0.095, 0.203, 0.287, clearEdge] : [0, 0.24, 0.3, 0.35, clearEdge];
  const opacities = putting ? [0.828, 0.72, 0.522, 0.162, 0] : [0.92, 0.92, 0.82, 0.34, 0];
  const stops = [
    ...positions.map((offset, index) => ({ offset, opacity: opacities[index] })),
    { offset: 1 - clearEdge, opacity: 0 },
    ...positions
      .slice(0, -1)
      .reverse()
      .map((offset, index) => ({
        offset: 1 - offset,
        opacity: opacities[opacities.length - 2 - index],
      })),
  ];
  return (
    <linearGradient id={id} x1="0" y1="0" x2="1" y2="0">
      {stops.map(({ offset, opacity }) => (
        <stop
          key={offset}
          offset={offset.toFixed(3)}
          stopColor={shade}
          stopOpacity={opacity.toFixed(3)}
        />
      ))}
    </linearGradient>
  );
}
