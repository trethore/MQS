import { BridgeError } from "@/bridge/core/bridgeError";
import { requestBridge, subscribeBridge } from "@/bridge/core/bridgeTransport";
import {
  CONSOLE_CHANNEL_CLEAR,
  CONSOLE_CHANNEL_COMMANDS,
  CONSOLE_CHANNEL_EXECUTE,
  CONSOLE_CHANNEL_SNAPSHOT,
  CONSOLE_EVENT_CLEARED,
  CONSOLE_EVENT_MESSAGE,
  CONSOLE_EVENT_SNAPSHOT_UPDATED,
  parseConsoleClearResponse,
  parseConsoleClearedEvent,
  parseConsoleCommandsResponse,
  parseConsoleExecuteResponse,
  parseConsoleMessageEvent,
  parseConsoleSnapshot,
  type ConsoleClearResponse,
  type ConsoleClearedEvent,
  type ConsoleCommandsResponse,
  type ConsoleExecuteResponse,
  type ConsoleMessage,
  type ConsoleSnapshot,
} from "@/bridge/contracts/console";

export async function fetchConsoleSnapshot(): Promise<ConsoleSnapshot> {
  const rawSnapshot = await requestBridge(CONSOLE_CHANNEL_SNAPSHOT, null);
  const snapshot = parseConsoleSnapshot(rawSnapshot);
  if (!snapshot) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid console snapshot payload.");
  }

  return snapshot;
}

export async function fetchConsoleCommands(): Promise<ConsoleCommandsResponse> {
  const rawResponse = await requestBridge(CONSOLE_CHANNEL_COMMANDS, null);
  const response = parseConsoleCommandsResponse(rawResponse);
  if (!response) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid console commands payload.");
  }

  return response;
}

export async function executeConsoleCommand(input: string): Promise<ConsoleExecuteResponse> {
  const rawResponse = await requestBridge(CONSOLE_CHANNEL_EXECUTE, { input });
  const response = parseConsoleExecuteResponse(rawResponse);
  if (!response) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid console execute payload.");
  }

  return response;
}

export async function clearConsole(): Promise<ConsoleClearResponse> {
  const rawResponse = await requestBridge(CONSOLE_CHANNEL_CLEAR, null);
  const response = parseConsoleClearResponse(rawResponse);
  if (!response) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid console clear payload.");
  }

  return response;
}

export function subscribeToConsoleMessage(listener: (message: ConsoleMessage) => void): () => void {
  return subscribeBridge(CONSOLE_EVENT_MESSAGE, (rawEvent) => {
    const message = parseConsoleMessageEvent(rawEvent);
    if (!message) {
      return;
    }

    listener(message);
  });
}

export function subscribeToConsoleCleared(
  listener: (event: ConsoleClearedEvent) => void,
): () => void {
  return subscribeBridge(CONSOLE_EVENT_CLEARED, (rawEvent) => {
    const event = parseConsoleClearedEvent(rawEvent);
    if (!event) {
      return;
    }

    listener(event);
  });
}

export function subscribeToConsoleSnapshotUpdated(
  listener: (snapshot: ConsoleSnapshot) => void,
): () => void {
  return subscribeBridge(CONSOLE_EVENT_SNAPSHOT_UPDATED, (rawEvent) => {
    const snapshot = parseConsoleSnapshot(rawEvent);
    if (!snapshot) {
      return;
    }

    listener(snapshot);
  });
}
