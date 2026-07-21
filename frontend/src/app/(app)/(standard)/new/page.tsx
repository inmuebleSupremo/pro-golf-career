import { CreateGolferForm } from "@/components/onboarding/create-golfer-form";

export default function NewCareerPage() {
  return (
    <div className="mx-auto flex max-w-[var(--container-content)] flex-col gap-8">
      <div className="flex flex-col gap-2">
        <h1 className="font-serif text-3xl font-medium tracking-[-0.01em]">Create your golfer</h1>
        <p className="text-muted-foreground">
          This is the career you&apos;ll guide across decades. Choose who they are and how they
          play.
        </p>
      </div>
      <CreateGolferForm />
    </div>
  );
}
