import * as React from 'react';

import { cn } from '@/lib/utils';

const SCROLL_FADE_EPSILON = 2;

function shouldShowBottomFade(element: HTMLDivElement) {
  const maxScrollTop = Math.max(0, element.scrollHeight - element.clientHeight);
  return (
    maxScrollTop > SCROLL_FADE_EPSILON && maxScrollTop - element.scrollTop > SCROLL_FADE_EPSILON
  );
}

type MqsListProps = {
  children: React.ReactNode;
  className?: string;
  viewportClassName?: string;
  contentClassName?: string;
  fadeClassName?: string;
};

function MqsList({
  children,
  className,
  viewportClassName,
  contentClassName,
  fadeClassName,
}: MqsListProps) {
  const viewportRef = React.useRef<HTMLDivElement | null>(null);
  const contentRef = React.useRef<HTMLDivElement | null>(null);
  const [showBottomFade, setShowBottomFade] = React.useState(false);
  const [enableFadeTransition, setEnableFadeTransition] = React.useState(false);

  React.useLayoutEffect(() => {
    const viewportElement = viewportRef.current;
    const contentElement = contentRef.current;

    if (!viewportElement) {
      return;
    }

    let animationFrameId = 0;

    const updateFadeVisibility = (animate: boolean) => {
      setEnableFadeTransition((current) => (current === animate ? current : animate));
      const next = shouldShowBottomFade(viewportElement);
      setShowBottomFade((current) => (current === next ? current : next));
    };

    const handleScroll = () => {
      updateFadeVisibility(true);
    };

    updateFadeVisibility(false);
    animationFrameId = globalThis.requestAnimationFrame(() => {
      updateFadeVisibility(false);
    });

    viewportElement.addEventListener('scroll', handleScroll, { passive: true });

    const resizeObserver = new ResizeObserver(() => {
      updateFadeVisibility(false);
    });

    resizeObserver.observe(viewportElement);

    if (contentElement) {
      resizeObserver.observe(contentElement);
    }

    return () => {
      globalThis.cancelAnimationFrame(animationFrameId);
      viewportElement.removeEventListener('scroll', handleScroll);
      resizeObserver.disconnect();
    };
  }, []);

  return (
    <div className={cn('relative min-h-0 flex-1', className)}>
      <div
        ref={viewportRef}
        className={cn(
          'mqs-scrollbar -mt-1.5 -ml-1.5 h-[calc(100%+0.375rem)] overflow-x-hidden overflow-y-auto pt-1.5 pl-1.5 [scrollbar-gutter:stable]',
          viewportClassName
        )}
      >
        <div ref={contentRef} className={cn('flex flex-col gap-3 pr-2 pb-5', contentClassName)}>
          {children}
        </div>
      </div>

      <div
        aria-hidden="true"
        className={cn(
          'pointer-events-none absolute right-2 bottom-0 left-0 z-20 h-9 bg-linear-to-t from-card via-card/92 to-transparent',
          showBottomFade ? 'opacity-100' : 'opacity-0',
          enableFadeTransition ? 'transition-opacity duration-150' : 'transition-none',
          fadeClassName
        )}
      />
    </div>
  );
}

export { MqsList };
