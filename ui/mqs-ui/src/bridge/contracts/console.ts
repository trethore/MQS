import {
  asBoolean,
  asFiniteNumber,
  asString,
  asStringArray,
  isObject,
} from "@/bridge/contracts/parsing";

export const CONSOLE_CHANNEL_SNAPSHOT = "mqs:console:snapshot";
export const CONSOLE_CHANNEL_COMMANDS = "mqs:console:commands";
export const CONSOLE_CHANNEL_EXECUTE = "mqs:console:execute";
export const CONSOLE_CHANNEL_CLEAR = "mqs:console:clear";

export const CONSOLE_EVENT_MESSAGE = "mqs:console:message";
export const CONSOLE_EVENT_CLEARED = "mqs:console:cleared";
export const CONSOLE_EVENT_SNAPSHOT_UPDATED = "mqs:console:snapshot:updated";

export interface ConsoleMessage {
  text: string;
  type: string;
  timestamp: string;
}

export interface ConsoleSnapshot {
  messages: ConsoleMessage[];
  commandHistory: string[];
  messageCount: number;
}

export interface ConsoleCommand {
  name: string;
  description: string;
  usage: string;
}

export interface ConsoleCommandsResponse {
  commands: ConsoleCommand[];
}

export interface ConsoleExecuteResponse {
  success: boolean;
  message: string;
  snapshot: ConsoleSnapshot | null;
}

export interface ConsoleClearResponse {
  success: boolean;
}

export interface ConsoleClearedEvent {
  cleared: boolean;
}

function parseConsoleMessage(rawMessage: unknown): ConsoleMessage | null {
  if (!isObject(rawMessage)) {
    return null;
  }

  return {
    text: asString(rawMessage.text),
    type: asString(rawMessage.type),
    timestamp: asString(rawMessage.timestamp),
  };
}

function parseConsoleCommand(rawCommand: unknown): ConsoleCommand | null {
  if (!isObject(rawCommand)) {
    return null;
  }

  const name = asString(rawCommand.name);
  if (!name) {
    return null;
  }

  return {
    name,
    description: asString(rawCommand.description),
    usage: asString(rawCommand.usage),
  };
}

export function parseConsoleSnapshot(rawSnapshot: unknown): ConsoleSnapshot | null {
  if (!isObject(rawSnapshot) || !Array.isArray(rawSnapshot.messages)) {
    return null;
  }

  const messages = rawSnapshot.messages
    .map((rawMessage) => parseConsoleMessage(rawMessage))
    .filter((message): message is ConsoleMessage => message !== null);

  return {
    messages,
    commandHistory: asStringArray(rawSnapshot.commandHistory),
    messageCount: asFiniteNumber(rawSnapshot.messageCount, messages.length),
  };
}

export function parseConsoleCommandsResponse(rawResponse: unknown): ConsoleCommandsResponse | null {
  if (!isObject(rawResponse) || !Array.isArray(rawResponse.commands)) {
    return null;
  }

  const commands = rawResponse.commands
    .map((rawCommand) => parseConsoleCommand(rawCommand))
    .filter((command): command is ConsoleCommand => command !== null);

  return { commands };
}

export function parseConsoleExecuteResponse(rawResponse: unknown): ConsoleExecuteResponse | null {
  if (!isObject(rawResponse)) {
    return null;
  }

  return {
    success: asBoolean(rawResponse.success),
    message: asString(rawResponse.message),
    snapshot: parseConsoleSnapshot(rawResponse.snapshot),
  };
}

export function parseConsoleClearResponse(rawResponse: unknown): ConsoleClearResponse | null {
  if (!isObject(rawResponse)) {
    return null;
  }

  return {
    success: asBoolean(rawResponse.success),
  };
}

export function parseConsoleClearedEvent(rawEvent: unknown): ConsoleClearedEvent | null {
  if (!isObject(rawEvent)) {
    return null;
  }

  return {
    cleared: asBoolean(rawEvent.cleared),
  };
}

export function parseConsoleMessageEvent(rawEvent: unknown): ConsoleMessage | null {
  return parseConsoleMessage(rawEvent);
}
