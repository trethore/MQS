import { waitForGrapheneBridge } from "@/bridge/core/grapheneClient";

export function subscribeBridge(channel: string, listener: (payload: unknown) => void): () => void {
  let active = true;
  let unsubscribe: (() => void) | null = null;

  waitForGrapheneBridge()
    .then((bridge) => {
      if (!active) {
        return;
      }

      unsubscribe = bridge.on(channel, listener);
    })
    .catch(() => {});

  return () => {
    active = false;

    if (unsubscribe) {
      unsubscribe();
    }
  };
}

export async function requestBridge(channel: string, payload?: unknown): Promise<unknown> {
  const bridge = await waitForGrapheneBridge();
  return bridge.request(channel, payload);
}
