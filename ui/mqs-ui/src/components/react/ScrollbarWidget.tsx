import * as React from "react";

import { cn } from "@/lib/utils";

type ScrollbarWidgetProps = React.ComponentProps<"div">;

export const ScrollbarWidget = React.forwardRef<HTMLDivElement, ScrollbarWidgetProps>(
  ({ className, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(
          "mqs-scrollbar min-h-0 flex-1 overflow-x-hidden overflow-y-scroll",
          className,
        )}
        {...props}
      />
    );
  },
);

ScrollbarWidget.displayName = "ScrollbarWidget";
