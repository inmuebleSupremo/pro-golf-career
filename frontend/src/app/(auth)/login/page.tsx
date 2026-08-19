import Link from "next/link";

import { LoginForm } from "@/components/auth/login-form";

export default function LoginPage() {
  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-2 text-center">
        <h1 className="text-2xl font-bold tracking-[-0.02em]">Welcome back</h1>
        <p className="text-muted-foreground">Sign in to continue your career.</p>
      </div>
      <LoginForm />
      <p className="text-muted-foreground text-center text-sm">
        New here?{" "}
        <Link href="/register" className="text-accent font-medium hover:underline">
          Create an account
        </Link>
      </p>
    </div>
  );
}
