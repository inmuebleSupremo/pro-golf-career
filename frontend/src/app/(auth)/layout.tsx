import Link from "next/link";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-full flex-1 flex-col items-center justify-center px-6 py-16">
      <div className="w-full max-w-[var(--container-form)]">
        <div className="mb-10 text-center">
          <Link
            href="/"
            className="text-foreground font-serif text-xl font-medium tracking-[-0.01em]"
          >
            Pro Golf Career
          </Link>
        </div>
        {children}
      </div>
    </div>
  );
}
