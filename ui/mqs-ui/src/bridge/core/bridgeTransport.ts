import { requireGrapheneBridge } from "@/bridge/core/grapheneClient";

export function subscribeBridge(channel: string, listener: (payload: unknown) => void): () => void {
  return requireGrapheneBridge().on(channel, listener);
}

export function requestBridge(channel: string, payload?: unknown): Promise<unknown> {
  return requireGrapheneBridge().request(channel, payload);
}
