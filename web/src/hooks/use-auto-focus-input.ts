import { useEffect, type RefObject } from 'react';

type FocusableInputElement = HTMLInputElement | HTMLTextAreaElement;

export function useAutoFocusInput(ref: RefObject<FocusableInputElement | null>, enabled = true) {
  useEffect(() => {
    if (!enabled) {
      return;
    }

    const focusInput = () => {
      const inputElement = ref.current;
      if (!inputElement || document.activeElement === inputElement) {
        return;
      }

      inputElement.focus({ preventScroll: true });
    };

    const animationFrameId = globalThis.requestAnimationFrame(focusInput);
    const timeoutIds = [
      globalThis.setTimeout(focusInput, 0),
      globalThis.setTimeout(focusInput, 80),
      globalThis.setTimeout(focusInput, 200),
    ];

    return () => {
      globalThis.cancelAnimationFrame(animationFrameId);
      for (const timeoutId of timeoutIds) {
        globalThis.clearTimeout(timeoutId);
      }
    };
  }, [enabled, ref]);
}
