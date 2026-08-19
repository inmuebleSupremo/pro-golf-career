import { Wordmark } from "@/components/app/wordmark";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="command-centre flex min-h-dvh flex-1 flex-col items-center justify-center px-6 py-16">
      <div className="w-full max-w-[var(--container-form)]">
        <div className="mb-10 flex justify-center">
          <Wordmark href="/" subtitle="Command Centre" />
        </div>
        <div className="border-border bg-surface rounded-xl border p-6 shadow-[var(--shadow-md)] sm:p-8">
          {children}
        </div>
      </div>
    </div>
  );
}
