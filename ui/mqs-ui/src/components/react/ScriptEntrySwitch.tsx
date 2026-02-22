import { cn } from "@/lib/utils";

interface ScriptEntrySwitchProps {
  checked: boolean;
  className?: string;
}

export function ScriptEntrySwitch({ checked, className }: ScriptEntrySwitchProps) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        "inline-flex h-6 w-11 shrink-0 items-center rounded-full border-2 border-transparent transition-colors",
        checked ? "bg-primary" : "bg-input",
        className,
      )}
    >
      <span
        className={cn(
          "h-5 w-5 rounded-full bg-background shadow-lg transition-transform",
          checked ? "translate-x-5" : "translate-x-0",
        )}
      />
    </span>
  );
}
