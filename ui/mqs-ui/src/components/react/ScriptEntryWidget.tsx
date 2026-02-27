import { ScriptEntrySwitch } from "./ScriptEntrySwitch";

interface ScriptEntryWidgetProps {
  title: string;
  path: string;
  version: string;
  running: boolean;
  disabled?: boolean;
  onToggle: () => void;
}

function formatPathBreadcrumb(path: string): string {
  const pathSegments = path.split(/[\\/]+/).filter(Boolean);

  if (pathSegments.length === 0) {
    return path;
  }

  const maxVisibleSegments = 4;
  const visibleSegments = pathSegments.slice(-maxVisibleSegments);
  const breadcrumb = visibleSegments.join("/");

  if (pathSegments.length <= maxVisibleSegments) {
    return breadcrumb;
  }

  return `.../${breadcrumb}`;
}

export function ScriptEntryWidget({
  title,
  path,
  version,
  running,
  disabled = false,
  onToggle,
}: ScriptEntryWidgetProps) {
  const pathBreadcrumb = formatPathBreadcrumb(path);

  return (
    <button
      type="button"
      aria-pressed={running}
      onClick={onToggle}
      disabled={disabled}
      className="mqs-focus-highlight flex w-full items-center justify-between gap-5 rounded-xl border border-border bg-background/50 px-5 py-3 text-left shadow-[0_0.75rem_1.75rem_-1.15rem_rgb(0_0_0_/_0.78)] transition-colors duration-150 enabled:hover:bg-accent/80 disabled:cursor-not-allowed"
    >
      <span className="min-w-0 flex-1">
        <span className="block truncate text-xl font-semibold leading-tight text-foreground">
          <span className="align-baseline">{title}</span>
          {version ? (
            <span className="ml-1.5 align-baseline text-xs font-medium text-muted-foreground">
              v{version}
            </span>
          ) : null}
        </span>
        <span className="mt-0.5 block truncate text-sm text-muted-foreground">{pathBreadcrumb}</span>
      </span>

      <ScriptEntrySwitch checked={running} className="scale-125" />
    </button>
  );
}
