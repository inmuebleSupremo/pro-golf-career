"use client";

import { useEffect, useRef, useState } from "react";
import { useIsMutating } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { FolderOpen, LogOut, Save } from "lucide-react";

import { useLogoutAction } from "@/components/app/logout-button";
import { useForegroundOperation } from "@/components/loading/foreground-loading-provider";
import { Button } from "@/components/ui/button";
import { useSaveCareer } from "@/lib/api/mutations";
import { GraphQLRequestError } from "@/lib/api/graphql-client";

type Destination = "saves" | "logout";
type SaveError = { kind: "conflict" | "pending" | "other"; message: string };

function describeSaveError(error: unknown): SaveError {
  if (error instanceof GraphQLRequestError && error.classification === "CONFLICT") {
    return {
      kind: "conflict",
      message: "This saved career was updated in another session. Your current progress is still here, but it cannot replace that newer save.",
    };
  }
  if (error instanceof GraphQLRequestError && /pending event|cannot snapshot/i.test(error.message)) {
    return { kind: "pending", message: "Finish the current event before saving your progress." };
  }
  if (error instanceof GraphQLRequestError && /no saved career|no revision/i.test(error.message)) {
    return { kind: "other", message: "This session is not linked to a saved career. Your progress is still here; saving could not be completed." };
  }
  return { kind: "other", message: "The career could not be saved. Your progress is still here. Please try again." };
}

/** Account exits are kept separate from the career destination list. */
export function CareerAccountMenu({ id, hasPendingEvent, statusReady, statusError = false }: {
  id: string;
  hasPendingEvent: boolean;
  statusReady: boolean;
  statusError?: boolean;
}) {
  const router = useRouter();
  const saveCareer = useSaveCareer(id);
  const { logout } = useLogoutAction();
  const foreground = useForegroundOperation();
  const mutationsInFlight = useIsMutating();
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);
  const busyRef = useRef(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [destination, setDestination] = useState<Destination | null>(null);
  const [discardConfirm, setDiscardConfirm] = useState(false);
  const [saveError, setSaveError] = useState<SaveError | null>(null);
  const [saving, setSaving] = useState(false);
  const [exiting, setExiting] = useState(false);
  const activeWork = mutationsInFlight > 0 || foreground.hasPendingForegroundOperation;
  const busy = saving || exiting || activeWork;
  const pendingSave = hasPendingEvent || saveError?.kind === "pending";
  const canSave = statusReady && !pendingSave && saveError?.kind !== "conflict" && !busy;

  useEffect(() => {
    if (!menuOpen) return;
    menuRef.current?.querySelector<HTMLButtonElement>('button[role="menuitem"]')?.focus();
    function onPointerDown(event: PointerEvent) {
      if (!menuRef.current?.contains(event.target as Node) && !triggerRef.current?.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener("pointerdown", onPointerDown);
    return () => document.removeEventListener("pointerdown", onPointerDown);
  }, [menuOpen]);

  useEffect(() => {
    if (!destination) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    cancelRef.current?.focus();
    return () => { document.body.style.overflow = previousOverflow; };
  }, [destination, discardConfirm]);

  function choose(target: Destination) {
    if (activeWork || busyRef.current) return;
    setMenuOpen(false);
    setDestination(target);
    setDiscardConfirm(false);
    setSaveError(null);
    dialogRef.current?.showModal();
  }

  function closeConfirmation() {
    if (busyRef.current || saving || exiting) return;
    dialogRef.current?.close();
  }

  async function finishExit(target: Destination) {
    if (target === "saves") {
      router.push("/saves");
    } else {
      await logout();
    }
  }

  async function saveAndExit() {
    if (!destination || !canSave || busyRef.current) return;
    busyRef.current = true;
    setSaving(true);
    setSaveError(null);
    try {
      await saveCareer.mutateAsync();
    } catch (error) {
      setSaveError(describeSaveError(error));
      busyRef.current = false;
      setSaving(false);
      return;
    }
    try {
      // The mutation has confirmed durable success and invalidated the saves list.
      await finishExit(destination);
    } catch {
      setSaveError({ kind: "other", message: "Your career was saved, but the account action could not be completed. Please try again." });
    } finally {
      busyRef.current = false;
      setSaving(false);
    }
  }

  async function discardAndExit() {
    if (!destination || !discardConfirm || busy || busyRef.current) return;
    busyRef.current = true;
    setExiting(true);
    try {
      await finishExit(destination);
    } catch {
      setSaveError({ kind: "other", message: "The account action could not be completed. Your career is still open." });
      setDiscardConfirm(false);
    } finally {
      busyRef.current = false;
      setExiting(false);
    }
  }

  const signingOut = destination === "logout";
  const saveActionLabel = signingOut ? "Save & Sign Out" : "Save & Return";
  const discardActionLabel = signingOut ? "Sign Out without Saving" : "Return without Saving";

  return (
    <>
      <div className="relative ml-auto shrink-0 lg:ml-0">
        <button
          ref={triggerRef}
          type="button"
          aria-label="Account menu"
          title="Account menu"
          aria-haspopup="menu"
          aria-expanded={menuOpen}
          aria-controls="career-account-menu"
          disabled={activeWork || saving || exiting}
          onClick={() => setMenuOpen((open) => !open)}
          onKeyDown={(event) => {
            if (event.key === "ArrowDown") {
              event.preventDefault();
              setMenuOpen(true);
            } else if (event.key === "Escape" && menuOpen) {
              setMenuOpen(false);
            }
          }}
          className="border-border text-foreground hover:border-primary/50 hover:text-primary focus-visible:outline-primary inline-flex size-10 shrink-0 items-center justify-center rounded-lg border focus-visible:outline-2 focus-visible:outline-offset-2 disabled:opacity-50 lg:w-12"
        >
          <Save className="size-5" aria-hidden="true" />
        </button>
        {menuOpen ? (
          <div
            ref={menuRef}
            id="career-account-menu"
            role="menu"
            aria-label="Account"
            onKeyDown={(event) => {
              if (event.key === "Escape") {
                event.preventDefault();
                setMenuOpen(false);
                triggerRef.current?.focus();
              }
              if (event.key === "ArrowDown" || event.key === "ArrowUp") {
                event.preventDefault();
                const items = [...event.currentTarget.querySelectorAll<HTMLButtonElement>('button[role="menuitem"]')];
                const current = items.indexOf(document.activeElement as HTMLButtonElement);
                items[(current + (event.key === "ArrowDown" ? 1 : items.length - 1)) % items.length]?.focus();
              }
            }}
            className="border-border bg-surface text-foreground absolute top-full right-0 z-[var(--z-sticky)] mt-2 min-w-44 rounded-lg border p-1 shadow-[var(--shadow-lg)]"
          >
            <button type="button" role="menuitem" onClick={() => choose("saves")}
              className="hover:bg-surface-elevated focus-visible:bg-surface-elevated flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm">
              <FolderOpen className="size-4" aria-hidden="true" /> My Careers
            </button>
            <button type="button" role="menuitem" onClick={() => choose("logout")}
              className="hover:bg-surface-elevated focus-visible:bg-surface-elevated flex w-full items-center gap-2 rounded-md px-3 py-2 text-left text-sm">
              <LogOut className="size-4" aria-hidden="true" /> Sign Out
            </button>
          </div>
        ) : null}
      </div>
      <dialog
        ref={dialogRef}
        aria-labelledby="career-exit-title"
        aria-describedby="career-exit-description"
        aria-modal="true"
        onCancel={(event) => { if (busyRef.current || saving || exiting) event.preventDefault(); }}
        onClose={() => {
          setDestination(null);
          setDiscardConfirm(false);
          setSaveError(null);
          triggerRef.current?.focus();
        }}
        className="border-border bg-surface text-foreground backdrop:bg-background/80 m-auto max-h-dvh w-[calc(100vw-2rem)] max-w-md overflow-y-auto rounded-xl border p-5 shadow-[var(--shadow-lg)] sm:p-6"
      >
        <h2 id="career-exit-title" className="flex items-center gap-2 text-lg font-bold">
          {signingOut ? <LogOut className="size-5 shrink-0" aria-hidden="true" />
            : <FolderOpen className="size-5 shrink-0 text-primary" aria-hidden="true" />}
          {discardConfirm ? `${discardActionLabel}?`
            : signingOut ? "Sign out of Pro Golf Career?" : "Return to My Careers?"}
        </h2>
        <p id="career-exit-description" className="text-muted-foreground mt-2 text-sm leading-relaxed">
          {discardConfirm
            ? `Unsaved progress in this career will be lost. This cannot be undone. ${signingOut ? "You will return to sign-in." : "You will return to My Careers."}`
            : signingOut
              ? "Save your current career progress before leaving your account and returning to sign-in."
              : "Save your current career progress before returning to the signed-in career-selection screen."}
        </p>
        {!discardConfirm && pendingSave ? (
          <p role="status" className="text-warning mt-3 text-sm">Finish the current event before progress can be saved.</p>
        ) : null}
        {!discardConfirm && !statusReady && !pendingSave ? (
          <p role="status" className="text-muted-foreground mt-3 text-sm">
            {statusError ? "Career status could not be checked. Refresh the page before trying to save." : "Checking career status before saving…"}
          </p>
        ) : null}
        {!discardConfirm && saveError ? (
          <p role="alert" className="text-destructive mt-3 text-sm">{saveError.message}</p>
        ) : null}
        {!discardConfirm && activeWork && !saving ? (
          <p role="status" className="text-muted-foreground mt-3 text-sm">Wait for the current action to finish before leaving.</p>
        ) : null}
        <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:flex-wrap sm:justify-end">
          {discardConfirm ? (
            <>
              <Button ref={cancelRef} type="button" variant="secondary" className="w-full sm:w-auto" disabled={saving || exiting}
                onClick={() => setDiscardConfirm(false)}>Keep playing</Button>
              <Button type="button" variant="destructive" className="w-full sm:w-auto" disabled={busy} onClick={discardAndExit}>
                {exiting ? "Leaving…" : `Confirm ${discardActionLabel}`}
              </Button>
            </>
          ) : (
            <>
              <Button ref={cancelRef} type="button" variant="secondary" className="w-full sm:w-auto" disabled={saving || exiting} onClick={closeConfirmation}>Cancel</Button>
              <Button type="button" variant="ghost" className="w-full sm:w-auto" disabled={busy} onClick={() => setDiscardConfirm(true)}>
                {discardActionLabel}
              </Button>
              <Button type="button" variant={signingOut ? "secondary" : "primary"}
                className={`w-full sm:w-auto ${signingOut ? "border-warning text-warning hover:bg-surface-elevated" : ""}`}
                disabled={!canSave} onClick={saveAndExit}>
                {saving ? "Saving…" : saveError?.kind === "other" ? `Retry ${saveActionLabel}` : saveActionLabel}
              </Button>
            </>
          )}
        </div>
      </dialog>
    </>
  );
}
