import * as React from "react";

import { cn } from "@/lib/utils";

type ScrollbarWidgetProps = React.ComponentProps<"div">;

export function ScrollbarWidget({ className, ...props }: ScrollbarWidgetProps) {
  return (
    <div
      className={cn(
        "mqs-scrollbar min-h-0 flex-1 overflow-x-hidden overflow-y-scroll",
        className
      )}
      {...props}
    />
  );
}
