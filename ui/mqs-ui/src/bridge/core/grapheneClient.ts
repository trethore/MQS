import { BRIDGE_UNAVAILABLE_MESSAGE, BridgeError } from "@/bridge/core/bridgeError";

type GrapheneMessageKind = "event" | "request" | "response" | "ready";

interface GrapheneBridgeMessage {
  bridge: string;
  version: number;
  kind: GrapheneMessageKind;
  id?: string;
  channel?: string;
  payload?: unknown;
  ok?: boolean;
  error?: {
    code?: string;
    message?: string;
  };
}

interface CefQueryCallbacks {
  request: string;
  onSuccess?: (responseText: string) => void;
  onFailure?: (errorCode: number, errorMessage: string) => void;
}

type GrapheneBridgeEventListener = (payload: unknown) => void;
type GrapheneBridgeRequestHandler = (payload: unknown) => unknown;

export interface GrapheneBridgeApi {
  __grapheneInstalled?: boolean;
  request: (channel: string, payload?: unknown) => Promise<unknown>;
  on: (channel: string, listener: (payload: unknown) => void) => () => void;
  off?: (channel: string, listener: (payload: unknown) => void) => void;
  emit?: (channel: string, payload?: unknown) => Promise<unknown>;
  handle?: (channel: string, handler: (payload: unknown) => unknown) => () => void;
}

const GRAPHENE_BRIDGE_NAME = "graphene-ui";
const GRAPHENE_PROTOCOL_VERSION = 1;
const GRAPHENE_ERROR_NO_HANDLER = "handler_not_found";
const GRAPHENE_ERROR_HANDLER_FAILURE = "js_handler_error";
const GRAPHENE_ERROR_INVALID_RESPONSE = "invalid_response";
const GRAPHENE_RECEIVE_FN_NAME = "__grapheneBridgeReceiveFromJava";

let nextRequestSequence = 0;

declare global {
  interface Window {
    cefQuery?: (callbacks: CefQueryCallbacks) => void;
    grapheneBridge?: GrapheneBridgeApi;
    __grapheneBridgeReceiveFromJava?: (messageJson: string) => void;
  }
}

function hasGrapheneRuntime(): boolean {
  if (globalThis.window === undefined) {
    return false;
  }

  return typeof globalThis.window.cefQuery === "function";
}

function isInstalledBridge(bridge: unknown): bridge is GrapheneBridgeApi {
  if (typeof bridge !== "object" || bridge === null) {
    return false;
  }

  const maybeBridge = bridge as Partial<GrapheneBridgeApi>;
  return typeof maybeBridge.request === "function" && typeof maybeBridge.on === "function";
}

function normalizePayload(payload: unknown): unknown {
  return payload === undefined ? null : payload;
}

function nextRequestId(): string {
  nextRequestSequence += 1;
  return `js-${Date.now()}-${nextRequestSequence}`;
}

function createMessage(kind: GrapheneMessageKind): GrapheneBridgeMessage {
  return {
    bridge: GRAPHENE_BRIDGE_NAME,
    version: GRAPHENE_PROTOCOL_VERSION,
    kind,
  };
}

function parseJsonOrNull(value: unknown): GrapheneBridgeMessage | null {
  try {
    return typeof value === "string" ? (JSON.parse(value) as GrapheneBridgeMessage) : null;
  } catch {
    return null;
  }
}

function isGrapheneMessage(
  message: GrapheneBridgeMessage | null,
): message is GrapheneBridgeMessage {
  return message?.bridge === GRAPHENE_BRIDGE_NAME;
}

function createBridgeError(response: GrapheneBridgeMessage): Error {
  const message = response.error?.message?.trim() || "Bridge request failed";
  const error = new Error(message) as Error & { code?: string };
  if (response.error?.code) {
    error.code = response.error.code;
  }

  return error;
}

function parseBridgeResponse(responseText: string): unknown {
  if (!responseText) {
    return null;
  }

  const response = parseJsonOrNull(responseText);
  if (!response) {
    throw createBridgeError({
      ...createMessage("response"),
      ok: false,
      error: {
        code: GRAPHENE_ERROR_INVALID_RESPONSE,
        message: "Bridge returned invalid JSON",
      },
    });
  }

  if (response.ok === false) {
    throw createBridgeError(response);
  }

  return response.payload ?? null;
}

function sendToJava(message: GrapheneBridgeMessage): Promise<unknown> {
  if (!hasGrapheneRuntime()) {
    return Promise.reject(new BridgeError("BRIDGE_UNAVAILABLE", BRIDGE_UNAVAILABLE_MESSAGE));
  }

  return new Promise((resolve, reject) => {
    globalThis.window.cefQuery?.({
      request: JSON.stringify(message),
      onSuccess: (responseText) => {
        resolve(parseBridgeResponse(responseText));
      },
      onFailure: (_errorCode, errorMessage) => {
        reject(new Error(errorMessage));
      },
    });
  });
}

function installFallbackBridge(): GrapheneBridgeApi | null {
  if (globalThis.window === undefined || !hasGrapheneRuntime()) {
    return null;
  }

  const existingBridge = globalThis.window.grapheneBridge;
  if (isInstalledBridge(existingBridge)) {
    return existingBridge;
  }

  const eventListenersByChannel = new Map<string, Set<GrapheneBridgeEventListener>>();
  const requestHandlersByChannel = new Map<string, GrapheneBridgeRequestHandler>();

  const addEventListener = (channel: string, listener: GrapheneBridgeEventListener) => {
    const listeners =
      eventListenersByChannel.get(channel) ?? new Set<GrapheneBridgeEventListener>();
    listeners.add(listener);
    eventListenersByChannel.set(channel, listeners);
  };

  const removeEventListener = (channel: string, listener: GrapheneBridgeEventListener) => {
    const listeners = eventListenersByChannel.get(channel);
    if (!listeners) {
      return;
    }

    listeners.delete(listener);
    if (listeners.size === 0) {
      eventListenersByChannel.delete(channel);
    }
  };

  const sendReady = () => {
    void sendToJava(createMessage("ready")).catch(() => {});
  };

  globalThis.window[GRAPHENE_RECEIVE_FN_NAME] = (messageJson: string) => {
    const message = parseJsonOrNull(messageJson);
    if (!isGrapheneMessage(message) || typeof message.channel !== "string") {
      return;
    }

    if (message.kind === "event") {
      const listeners = eventListenersByChannel.get(message.channel);
      if (!listeners) {
        return;
      }

      for (const listener of listeners) {
        try {
          listener(message.payload ?? null);
        } catch {}
      }

      return;
    }

    if (message.kind !== "request") {
      return;
    }

    const handler = requestHandlersByChannel.get(message.channel);
    if (!handler) {
      void sendToJava({
        ...createMessage("response"),
        id: message.id,
        channel: message.channel,
        ok: false,
        payload: null,
        error: {
          code: GRAPHENE_ERROR_NO_HANDLER,
          message: `No JS bridge handler for channel '${message.channel}'`,
        },
      }).catch(() => {});
      return;
    }

    void Promise.resolve(handler(message.payload ?? null))
      .then((responsePayload) => {
        return sendToJava({
          ...createMessage("response"),
          id: message.id,
          channel: message.channel,
          ok: true,
          payload: normalizePayload(responsePayload),
        });
      })
      .catch((error: unknown) => {
        const errorMessage = error instanceof Error ? error.message : String(error);
        return sendToJava({
          ...createMessage("response"),
          id: message.id,
          channel: message.channel,
          ok: false,
          payload: null,
          error: {
            code: GRAPHENE_ERROR_HANDLER_FAILURE,
            message: errorMessage,
          },
        });
      })
      .catch(() => {});
  };

  const fallbackBridge: GrapheneBridgeApi = {
    __grapheneInstalled: true,
    on: (channel, listener) => {
      addEventListener(channel, listener);
      return () => {
        removeEventListener(channel, listener);
      };
    },
    off: (channel, listener) => {
      removeEventListener(channel, listener);
    },
    handle: (channel, handler) => {
      requestHandlersByChannel.set(channel, handler);
      return () => {
        if (requestHandlersByChannel.get(channel) === handler) {
          requestHandlersByChannel.delete(channel);
        }
      };
    },
    emit: (channel, payload) => {
      return sendToJava({
        ...createMessage("event"),
        channel,
        payload: normalizePayload(payload),
      });
    },
    request: (channel, payload) => {
      return sendToJava({
        ...createMessage("request"),
        id: nextRequestId(),
        channel,
        payload: normalizePayload(payload),
      });
    },
  };

  globalThis.window.grapheneBridge = fallbackBridge;
  sendReady();

  return fallbackBridge;
}

export function getGrapheneBridge(): GrapheneBridgeApi | null {
  if (globalThis.window === undefined) {
    return null;
  }

  const bridge = globalThis.window.grapheneBridge;
  return isInstalledBridge(bridge) ? bridge : null;
}

export function requireGrapheneBridge(): GrapheneBridgeApi {
  const bridge = getGrapheneBridge() ?? installFallbackBridge();
  if (!bridge) {
    throw new BridgeError("BRIDGE_UNAVAILABLE", BRIDGE_UNAVAILABLE_MESSAGE);
  }

  return bridge;
}

export function waitForGrapheneBridge(): Promise<GrapheneBridgeApi> {
  const bridge = getGrapheneBridge() ?? installFallbackBridge();
  if (bridge) {
    return Promise.resolve(bridge);
  }

  return Promise.reject(new BridgeError("BRIDGE_UNAVAILABLE", BRIDGE_UNAVAILABLE_MESSAGE));
}
