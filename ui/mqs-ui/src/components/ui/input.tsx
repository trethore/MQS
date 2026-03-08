import * as React from "react";

import { cn } from "@/lib/utils";

interface InputProps extends React.ComponentProps<"input"> {
  "data-slot"?: string;
}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, "data-slot": dataSlot, ...props }, ref) => {
    const standaloneInput = dataSlot !== "input-group-control";

    return (
      <input
        data-slot="input"
        type={type}
        className={cn(
          "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-sm transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium file:text-foreground placeholder:text-muted-foreground focus-visible:ring-0 disabled:cursor-not-allowed disabled:opacity-50 md:text-sm",
          standaloneInput ? "mqs-focus-highlight-always" : null,
          className,
        )}
        ref={ref}
        {...props}
      />
    );
  },
);
Input.displayName = "Input";

export { Input };
