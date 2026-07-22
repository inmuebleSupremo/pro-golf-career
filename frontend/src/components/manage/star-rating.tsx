import { Star } from "lucide-react";

/** Staff quality (0–1) as a 1–3 star tier. Higher-quality staff cost more and have more effect. */
export function staffTier(quality: number): number {
  if (quality >= 0.8) return 3;
  if (quality >= 0.55) return 2;
  return 1;
}

export function StarRating({ quality }: { quality: number }) {
  const tier = staffTier(quality);
  return (
    <span className="inline-flex items-center gap-0.5" aria-label={`${tier} of 3 stars`}>
      {[1, 2, 3].map((i) => (
        <Star
          key={i}
          className={`size-3.5 ${i <= tier ? "fill-gold text-gold" : "fill-none text-subtle-foreground/40"}`}
          aria-hidden="true"
        />
      ))}
    </span>
  );
}
