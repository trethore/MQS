import { useState } from "react";

import { ScriptEntrySwitch } from "./ScriptEntrySwitch";

interface ScriptEntryWidgetProps {
  title: string;
  path: string;
  enabled?: boolean;
}

export function ScriptEntryWidget({ title, path, enabled = false }: ScriptEntryWidgetProps) {
  const [checked, setChecked] = useState(enabled);

  const toggleChecked = () => {
    setChecked((previous) => !previous);
  };

  return (
    <button
      type="button"
      aria-pressed={checked}
      onClick={toggleChecked}
      className="flex w-full items-center justify-between gap-5 rounded-xl border border-border bg-background/50 px-5 py-4 text-left shadow-[0_0.75rem_1.75rem_-1.15rem_rgb(0_0_0_/_0.78)] transition-colors duration-150 hover:bg-accent/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring"
    >
      <span className="min-w-0 flex-1">
        <span className="block truncate text-2xl font-semibold leading-tight text-foreground">
          {title}
        </span>
        <span className="mt-1 block truncate text-sm text-muted-foreground">{path}</span>
      </span>

      <ScriptEntrySwitch checked={checked} className="scale-125" />
    </button>
  );
}
