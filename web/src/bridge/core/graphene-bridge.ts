type GrapheneBridgeListener = (payload: unknown) => void;

type GrapheneBridgeHandler = (payload: unknown) => unknown | Promise<unknown>;

export type GrapheneBridgeApi = {
  readonly __grapheneInstalled: boolean;
  on(channel: string, listener: GrapheneBridgeListener): () => void;
  off(channel: string, listener: GrapheneBridgeListener): void;
  handle(channel: string, handler: GrapheneBridgeHandler): () => void;
  isReady(): boolean;
  onReady(listener: () => void): () => void;
  ready(): Promise<void>;
  emit(channel: string, payload?: unknown): Promise<unknown>;
  request<T = unknown>(channel: string, payload?: unknown): Promise<T>;
};

declare global {
  interface Window {
    grapheneBridge?: GrapheneBridgeApi;
  }

  var grapheneBridge: GrapheneBridgeApi | undefined;
}

const DEFAULT_BRIDGE_INSTALL_TIMEOUT_MS = 4_000;
const DEFAULT_BRIDGE_POLL_INTERVAL_MS = 25;

export class GrapheneBridgeUnavailableError extends Error {
  constructor(message = 'Graphene bridge is unavailable. Open the UI from Minecraft to use live MQS data.') {
    super(message);
    this.name = 'GrapheneBridgeUnavailableError';
  }
}

export type GrapheneBridgeWaitOptions = {
  timeoutMs?: number;
  pollIntervalMs?: number;
  signal?: AbortSignal;
};

function isGrapheneBridgeApi(value: unknown): value is GrapheneBridgeApi {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as Partial<GrapheneBridgeApi>;
  return (
    candidate.__grapheneInstalled === true &&
    typeof candidate.on === 'function' &&
    typeof candidate.off === 'function' &&
    typeof candidate.handle === 'function' &&
    typeof candidate.isReady === 'function' &&
    typeof candidate.onReady === 'function' &&
    typeof candidate.ready === 'function' &&
    typeof candidate.emit === 'function' &&
    typeof candidate.request === 'function'
  );
}

export function getInstalledGrapheneBridge(): GrapheneBridgeApi | null {
  const bridge = globalThis.grapheneBridge;
  return isGrapheneBridgeApi(bridge) ? bridge : null;
}

export function isGrapheneBridgeInstalled(): boolean {
  return getInstalledGrapheneBridge() !== null;
}

export async function waitForInstalledGrapheneBridge(
  options: GrapheneBridgeWaitOptions = {}
): Promise<GrapheneBridgeApi> {
  const timeoutMs = options.timeoutMs ?? DEFAULT_BRIDGE_INSTALL_TIMEOUT_MS;
  const pollIntervalMs = options.pollIntervalMs ?? DEFAULT_BRIDGE_POLL_INTERVAL_MS;
  const signal = options.signal;
  const installedBridge = getInstalledGrapheneBridge();
  if (installedBridge) {
    return installedBridge;
  }

  if (signal?.aborted) {
    throw createGrapheneBridgeAbortError();
  }

  return await new Promise<GrapheneBridgeApi>((resolve, reject) => {
    const startedAt = Date.now();
    let settled = false;

    const cleanup = () => {
      globalThis.clearInterval(intervalId);
      signal?.removeEventListener('abort', handleAbort);
    };

    const finish = (callback: () => void) => {
      if (settled) {
        return;
      }

      settled = true;
      cleanup();
      callback();
    };

    const handleAbort = () => {
      finish(() => reject(createGrapheneBridgeAbortError()));
    };

    const intervalId = globalThis.setInterval(() => {
      const currentBridge = getInstalledGrapheneBridge();
      if (currentBridge) {
        finish(() => resolve(currentBridge));
        return;
      }

      if (Date.now() - startedAt < timeoutMs) {
        return;
      }

      finish(() => reject(new GrapheneBridgeUnavailableError()));
    }, pollIntervalMs);

    signal?.addEventListener('abort', handleAbort, { once: true });
  });
}

export async function waitForReadyGrapheneBridge(
  options: GrapheneBridgeWaitOptions = {}
): Promise<GrapheneBridgeApi> {
  const bridge = await waitForInstalledGrapheneBridge(options);
  await bridge.ready();
  return bridge;
}

export async function requestGrapheneBridge<Response>(
  channel: string,
  payload?: unknown,
  options: GrapheneBridgeWaitOptions = {}
): Promise<Response> {
  const bridge = await waitForReadyGrapheneBridge(options);
  return await bridge.request<Response>(channel, payload ?? null);
}

export async function subscribeToGrapheneBridgeEvent(
  channel: string,
  listener: GrapheneBridgeListener,
  options: GrapheneBridgeWaitOptions = {}
): Promise<() => void> {
  const bridge = await waitForReadyGrapheneBridge(options);
  return bridge.on(channel, listener);
}

function createGrapheneBridgeAbortError(): Error {
  const abortError = new Error('Graphene bridge request was aborted.');
  abortError.name = 'AbortError';
  return abortError;
}

export function formatGrapheneBridgeError(error: unknown): string {
  if (error instanceof GrapheneBridgeUnavailableError) {
    return error.message;
  }

  if (error instanceof Error) {
    if (error.name === 'AbortError') {
      return 'Graphene bridge request was aborted.';
    }

    const message = error.message.trim();
    if (message.length > 0) {
      return message;
    }
  }

  return 'Unexpected Graphene bridge error.';
}
