import * as React from "react";

import { cn } from "@/lib/utils";

interface ScrollbarWidgetProps extends React.ComponentProps<"div"> {
  bottomFade?: boolean;
}

export const ScrollbarWidget = React.forwardRef<HTMLDivElement, ScrollbarWidgetProps>(
  ({ bottomFade = true, children, className, ...props }, ref) => {
    return (
      <div className="relative min-h-0 flex-1">
        <div
          ref={ref}
          className={cn("mqs-scrollbar h-full overflow-x-hidden overflow-y-scroll", className)}
          {...props}
        >
          {children}
        </div>

        {bottomFade ? (
          <div
            aria-hidden="true"
            className="mqs-scrollbar-bottom-fade pointer-events-none absolute bottom-0 left-0"
          />
        ) : null}
      </div>
    );
  },
);

ScrollbarWidget.displayName = "ScrollbarWidget";
