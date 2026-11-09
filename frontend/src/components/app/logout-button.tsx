"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { LogOut } from "lucide-react";

import { Button } from "@/components/ui/button";

export function LogoutButton() {
  const { logout, pending } = useLogoutAction();

  return (
    <Button variant="ghost" size="sm" onClick={logout} disabled={pending}>
      <LogOut className="size-4" aria-hidden="true" />
      Sign out
    </Button>
  );
}

/** The existing logout sequence shared by the standard header and career account menu. */
export function useLogoutAction() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState(false);
  const running = useRef(false);

  async function logout() {
    if (running.current) return;
    running.current = true;
    setPending(true);
    try {
      await fetch("/api/auth/logout", { method: "POST" });
      // Clear cached authenticated data so nothing lingers after sign-out (design D5).
      queryClient.clear();
      router.push("/login");
      router.refresh();
    } catch (error) {
      running.current = false;
      setPending(false);
      throw error;
    }
  }

  return { logout, pending };
}
