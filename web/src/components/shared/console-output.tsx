import * as React from 'react';

import type { ConsoleMessageItem } from '@/bridge/services/console-service';
import { cn } from '@/lib/utils';

const AUTO_SCROLL_THRESHOLD_PX = 32;

type ConsoleOutputProps = {
  readonly messages: ReadonlyArray<ConsoleMessageItem>;
  readonly isLoading?: boolean;
  readonly className?: string;
};

function isNearBottom(element: HTMLDivElement): boolean {
  const remainingScroll = element.scrollHeight - element.scrollTop - element.clientHeight;
  return remainingScroll <= AUTO_SCROLL_THRESHOLD_PX;
}

function getMessageTextClassName(type: ConsoleMessageItem['type']): string {
  switch (type) {
    case 'COMMAND': {
      return 'text-amber-600 dark:text-amber-300';
    }
    case 'ERROR': {
      return 'text-rose-600 dark:text-rose-300';
    }
    case 'SUCCESS': {
      return 'text-emerald-600 dark:text-emerald-300';
    }
    case 'INFO': {
      return 'text-foreground';
    }
  }
}

export function ConsoleOutput({ messages, isLoading = false, className }: ConsoleOutputProps) {
  const viewportRef = React.useRef<HTMLDivElement | null>(null);
  const stickToBottomRef = React.useRef(true);

  let content: React.ReactNode;
  if (messages.length > 0) {
    content = (
      <div className="flex flex-col gap-0">
        {messages.map((message, index) => {
          return (
            <div
              key={`${message.timestamp}-${message.type}-${index}-${message.text}`}
              className="grid grid-cols-[auto_1fr] items-start gap-x-3"
            >
              <span className="w-14 shrink-0 text-[12px] text-muted-foreground/70 tabular-nums">
                {message.timestamp}
              </span>
              <span
                className={cn(
                  'min-w-0 whitespace-pre-wrap wrap-break-words',
                  getMessageTextClassName(message.type)
                )}
              >
                {message.text}
              </span>
            </div>
          );
        })}
      </div>
    );
  } else if (isLoading) {
    content = <div className="h-full" />;
  } else {
    content = (
      <div className="flex h-full min-h-40 items-center justify-center text-center text-sm text-muted-foreground">
        Console output will appear here.
      </div>
    );
  }

  React.useEffect(() => {
    const viewportElement = viewportRef.current;
    if (!viewportElement) {
      return;
    }

    const handleScroll = () => {
      stickToBottomRef.current = isNearBottom(viewportElement);
    };

    handleScroll();
    viewportElement.addEventListener('scroll', handleScroll, { passive: true });

    return () => {
      viewportElement.removeEventListener('scroll', handleScroll);
    };
  }, []);

  React.useLayoutEffect(() => {
    const viewportElement = viewportRef.current;
    if (!viewportElement || !stickToBottomRef.current) {
      return;
    }

    viewportElement.scrollTop = viewportElement.scrollHeight;
  }, [messages]);

  return (
    <div
      className={cn(
        'min-h-0 flex-1 overflow-hidden rounded-xl border border-border bg-background text-foreground shadow-xs transition-[border-color,box-shadow] focus-within:border-border focus-within:ring-[3px] focus-within:ring-ring/50 dark:border-input dark:bg-input/30 dark:focus-within:border-input',
        className
      )}
    >
      <div
        ref={viewportRef}
        className="mqs-scrollbar h-full overflow-x-hidden overflow-y-auto px-5 py-4 font-mono text-[13px] leading-5 outline-none focus:outline-none focus-visible:outline-none"
      >
        {content}
      </div>
    </div>
  );
}
