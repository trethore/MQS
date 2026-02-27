import { useEffect, useMemo, useRef } from "react";

import type { ConsoleMessage } from "@/bridge/contracts/console";
import { ScrollbarWidget } from "@/components/react/ScrollbarWidget";
import { cn } from "@/lib/utils";

interface ConsoleOutputWidgetProps {
  loading: boolean;
  messages: ConsoleMessage[];
}

const MESSAGE_TYPE_CLASSNAME: Record<string, string> = {
  INFO: "text-foreground",
  ERROR: "text-destructive",
  COMMAND: "text-primary",
  SUCCESS: "text-success",
};

export function ConsoleOutputWidget({ loading, messages }: ConsoleOutputWidgetProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const stickToBottomRef = useRef(true);

  useEffect(() => {
    const container = containerRef.current;
    if (!container || !stickToBottomRef.current) {
      return;
    }

    container.scrollTop = container.scrollHeight;
  }, [messages.length]);

  const emptyStateText = useMemo(() => {
    if (loading) {
      return "Loading console...";
    }

    return "No console output yet.";
  }, [loading]);

  return (
    <section className="flex min-h-0 flex-1 flex-col rounded-xl border border-border bg-background/40">
      <div className="flex items-center border-b border-border px-3 py-2">
        <p className="text-sm font-semibold text-foreground">Console</p>
      </div>

      <ScrollbarWidget
        ref={containerRef}
        className="px-3 py-2"
        onScroll={(event) => {
          const target = event.currentTarget;
          const remainingDistance = target.scrollHeight - (target.scrollTop + target.clientHeight);
          stickToBottomRef.current = remainingDistance <= 16;
        }}
      >
        {messages.length === 0 ? (
          <p className="pt-2 text-sm text-muted-foreground">{emptyStateText}</p>
        ) : (
          <ul className="flex min-h-full flex-col gap-0 pb-2">
            {messages.map((message, index) => (
              <li
                key={`${message.timestamp}-${index}`}
                className="grid grid-cols-[auto_1fr] items-start gap-2 rounded-md px-2 py-0.5 text-sm leading-tight"
              >
                <span className="font-mono text-[0.7rem] leading-tight text-muted-foreground">
                  {message.timestamp}
                </span>
                <span
                  className={cn(
                    "font-mono whitespace-pre-wrap wrap-break-word leading-tight",
                    MESSAGE_TYPE_CLASSNAME[message.type] ?? "text-foreground",
                  )}
                >
                  {message.text}
                </span>
              </li>
            ))}
          </ul>
        )}
      </ScrollbarWidget>
    </section>
  );
}
