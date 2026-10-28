import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Local visual-review browser resolves the development server through loopback.
  allowedDevOrigins: ["127.0.0.1"],
};

export default nextConfig;
