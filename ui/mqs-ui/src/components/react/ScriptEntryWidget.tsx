import { ScriptEntrySwitch } from "./ScriptEntrySwitch";

interface ScriptEntryWidgetProps {
  title: string;
  path: string;
  version: string;
  scriptId: string;
  running: boolean;
  disabled?: boolean;
  onToggle: () => void;
}

export function ScriptEntryWidget({
  title,
  path,
  version,
  scriptId,
  running,
  disabled = false,
  onToggle,
}: ScriptEntryWidgetProps) {
  return (
    <button
      type="button"
      aria-pressed={running}
      onClick={onToggle}
      disabled={disabled}
      className="flex w-full items-center justify-between gap-5 rounded-xl border border-border bg-background/50 px-5 py-4 text-left shadow-[0_0.75rem_1.75rem_-1.15rem_rgb(0_0_0_/_0.78)] transition-colors duration-150 enabled:hover:bg-accent/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring disabled:cursor-not-allowed"
    >
      <span className="min-w-0 flex-1">
        <span className="block truncate text-2xl font-semibold leading-tight text-foreground">
          {title}
        </span>
        <span className="mt-1 block truncate text-sm text-muted-foreground">{path}</span>
        <span className="mt-1 block truncate text-xs text-muted-foreground/90">
          {version ? `v${version} ` : ""}
          {scriptId}
        </span>
      </span>

      <ScriptEntrySwitch checked={running} className="scale-125" />
    </button>
  );
}
