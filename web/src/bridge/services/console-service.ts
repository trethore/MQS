import {
  CONSOLE_BRIDGE_CHANNELS,
  CONSOLE_BRIDGE_EVENTS,
  parseConsoleClearedResponse,
  parseConsoleExecuteResponse,
  parseConsoleMessageEvent,
  parseConsoleSnapshotResponse,
  type ConsoleExecuteRequest,
  type ConsoleExecuteResponse,
  type ConsoleMessageResponse,
  type ConsoleSnapshotResponse,
} from '@/bridge/contracts/console';
import {
  requestGrapheneBridge,
  subscribeToGrapheneBridgeEvent,
  type GrapheneBridgeWaitOptions,
} from '@/bridge/core/graphene-bridge';

const CONSOLE_MESSAGE_TYPES = ['INFO', 'ERROR', 'COMMAND', 'SUCCESS'] as const;

export type ConsoleMessageType = (typeof CONSOLE_MESSAGE_TYPES)[number];

export type ConsoleMessageItem = {
  text: string;
  type: ConsoleMessageType;
  timestamp: string;
};

export type ConsoleSnapshot = {
  messages: Array<ConsoleMessageItem>;
  commandHistory: Array<string>;
  messageCount: number;
};

export type ConsoleExecuteResult = {
  success: boolean;
  message: string;
  snapshot: ConsoleSnapshot;
};

function normalizeConsoleMessageType(value: string): ConsoleMessageType {
  return CONSOLE_MESSAGE_TYPES.includes(value as ConsoleMessageType) ? (value as ConsoleMessageType) : 'INFO';
}

function mapMessage(message: ConsoleMessageResponse): ConsoleMessageItem {
  return {
    text: message.text,
    type: normalizeConsoleMessageType(message.type),
    timestamp: message.timestamp,
  };
}

function mapSnapshot(snapshot: ConsoleSnapshotResponse): ConsoleSnapshot {
  return {
    messages: snapshot.messages.map(mapMessage),
    commandHistory: snapshot.commandHistory,
    messageCount: snapshot.messageCount,
  };
}

function mapExecuteResult(result: ConsoleExecuteResponse): ConsoleExecuteResult {
  return {
    success: result.success,
    message: result.message,
    snapshot: mapSnapshot(result.snapshot),
  };
}

export async function getConsoleSnapshot(options?: GrapheneBridgeWaitOptions): Promise<ConsoleSnapshot> {
  const payload = await requestGrapheneBridge<unknown>(CONSOLE_BRIDGE_CHANNELS.snapshot, null, options);
  return mapSnapshot(parseConsoleSnapshotResponse(payload));
}

export async function executeConsoleCommand(
  input: string,
  options?: GrapheneBridgeWaitOptions
): Promise<ConsoleExecuteResult> {
  const payload = await requestGrapheneBridge<unknown>(CONSOLE_BRIDGE_CHANNELS.execute, { input } satisfies ConsoleExecuteRequest, options);
  return mapExecuteResult(parseConsoleExecuteResponse(payload));
}

export async function subscribeToConsoleMessage(
  listener: (message: ConsoleMessageItem) => void,
  options?: GrapheneBridgeWaitOptions
): Promise<() => void> {
  return await subscribeToGrapheneBridgeEvent(
    CONSOLE_BRIDGE_EVENTS.message,
    (payload) => {
      listener(mapMessage(parseConsoleMessageEvent(payload)));
    },
    options
  );
}

export async function subscribeToConsoleCleared(
  listener: () => void,
  options?: GrapheneBridgeWaitOptions
): Promise<() => void> {
  return await subscribeToGrapheneBridgeEvent(
    CONSOLE_BRIDGE_EVENTS.cleared,
    (payload) => {
      const response = parseConsoleClearedResponse(payload);
      if (response.cleared) {
        listener();
      }
    },
    options
  );
}

export async function subscribeToConsoleSnapshotUpdated(
  listener: (snapshot: ConsoleSnapshot) => void,
  options?: GrapheneBridgeWaitOptions
): Promise<() => void> {
  return await subscribeToGrapheneBridgeEvent(
    CONSOLE_BRIDGE_EVENTS.snapshotUpdated,
    (payload) => {
      listener(mapSnapshot(parseConsoleSnapshotResponse(payload)));
    },
    options
  );
}
