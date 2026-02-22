import * as React from "react";

import { Switch } from "@/components/ui/switch";
import { cn } from "@/lib/utils";

type ScriptEntrySwitchProps = React.ComponentProps<typeof Switch>;

export function ScriptEntrySwitch({ className, ...props }: ScriptEntrySwitchProps) {
  return (
    <Switch
      className={cn(
        "scale-125 data-[state=checked]:bg-primary data-[state=unchecked]:bg-input",
        className
      )}
      {...props}
    />
  );
}
