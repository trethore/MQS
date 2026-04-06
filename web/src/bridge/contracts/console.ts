import {
  expectObject,
  readArray,
  readBoolean,
  readNumber,
  readString,
} from '@/bridge/contracts/json';

export const CONSOLE_BRIDGE_CHANNELS = {
  snapshot: 'mqs:console:snapshot',
  commands: 'mqs:console:commands',
  execute: 'mqs:console:execute',
  clear: 'mqs:console:clear',
} as const;

export const CONSOLE_BRIDGE_EVENTS = {
  message: 'mqs:console:message',
  cleared: 'mqs:console:cleared',
  snapshotUpdated: 'mqs:console:snapshot:updated',
} as const;

export type ConsoleExecuteRequest = {
  input: string;
};

export type ConsoleMessageResponse = {
  text: string;
  type: string;
  timestamp: string;
};

export type ConsoleSnapshotResponse = {
  messages: Array<ConsoleMessageResponse>;
  commandHistory: Array<string>;
  messageCount: number;
};

export type ConsoleExecuteResponse = {
  success: boolean;
  message: string;
  snapshot: ConsoleSnapshotResponse;
};

export type ConsoleClearedResponse = {
  cleared: boolean;
};

function parseConsoleMessageResponse(value: unknown): ConsoleMessageResponse {
  const objectValue = expectObject(value, 'console message');
  return {
    text: readString(objectValue.text, 'console message text'),
    type: readString(objectValue.type, 'console message type'),
    timestamp: readString(objectValue.timestamp, 'console message timestamp'),
  };
}

export function parseConsoleSnapshotResponse(value: unknown): ConsoleSnapshotResponse {
  const objectValue = expectObject(value, 'console snapshot');
  return {
    messages: readArray(objectValue.messages, 'console snapshot messages').map(
      parseConsoleMessageResponse
    ),
    commandHistory: readArray(objectValue.commandHistory, 'console snapshot commandHistory').map(
      (entry) => {
        return readString(entry, 'console history entry');
      }
    ),
    messageCount: readNumber(objectValue.messageCount, 'console snapshot messageCount'),
  };
}

export function parseConsoleExecuteResponse(value: unknown): ConsoleExecuteResponse {
  const objectValue = expectObject(value, 'console execute response');
  return {
    success: readBoolean(objectValue.success, 'console execute response success'),
    message: readString(objectValue.message, 'console execute response message'),
    snapshot: parseConsoleSnapshotResponse(objectValue.snapshot),
  };
}

export function parseConsoleClearedResponse(value: unknown): ConsoleClearedResponse {
  const objectValue = expectObject(value, 'console cleared response');
  return {
    cleared: readBoolean(objectValue.cleared, 'console cleared response cleared'),
  };
}

export function parseConsoleMessageEvent(value: unknown): ConsoleMessageResponse {
  return parseConsoleMessageResponse(value);
}
