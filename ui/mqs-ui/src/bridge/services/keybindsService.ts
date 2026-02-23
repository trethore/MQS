import { BridgeError } from "@/bridge/core/bridgeError";
import { requestBridge, subscribeBridge } from "@/bridge/core/bridgeTransport";
import {
  KEYBINDS_CHANNEL_CREATE,
  KEYBINDS_CHANNEL_DELETE,
  KEYBINDS_CHANNEL_LIST,
  KEYBINDS_CHANNEL_UPDATE,
  KEYBINDS_EVENT_TRIGGERED,
  KEYBINDS_EVENT_UPDATED,
  parseKeybindMutationResponse,
  parseKeybindSnapshot,
  parseKeybindTriggeredEvent,
  type KeybindMutationRequest,
  type KeybindMutationResponse,
  type KeybindSnapshot,
  type KeybindTriggeredEvent,
} from "@/bridge/contracts/keybinds";

export async function fetchKeybindsSnapshot(): Promise<KeybindSnapshot> {
  const rawSnapshot = await requestBridge(KEYBINDS_CHANNEL_LIST, null);
  const snapshot = parseKeybindSnapshot(rawSnapshot);
  if (!snapshot) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid keybinds snapshot payload.");
  }

  return snapshot;
}

async function runKeybindMutation(
  channel: string,
  request: KeybindMutationRequest,
): Promise<KeybindMutationResponse> {
  const rawResponse = await requestBridge(channel, request);
  const response = parseKeybindMutationResponse(rawResponse);
  if (!response) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid keybind mutation payload.");
  }

  return response;
}

export function createKeybind(request: KeybindMutationRequest): Promise<KeybindMutationResponse> {
  return runKeybindMutation(KEYBINDS_CHANNEL_CREATE, request);
}

export function updateKeybind(request: KeybindMutationRequest): Promise<KeybindMutationResponse> {
  return runKeybindMutation(KEYBINDS_CHANNEL_UPDATE, request);
}

export function deleteKeybind(request: KeybindMutationRequest): Promise<KeybindMutationResponse> {
  return runKeybindMutation(KEYBINDS_CHANNEL_DELETE, request);
}

export function subscribeToKeybindsUpdated(
  listener: (snapshot: KeybindSnapshot) => void,
): () => void {
  return subscribeBridge(KEYBINDS_EVENT_UPDATED, (rawEvent) => {
    const snapshot = parseKeybindSnapshot(rawEvent);
    if (!snapshot) {
      return;
    }

    listener(snapshot);
  });
}

export function subscribeToKeybindTriggered(
  listener: (event: KeybindTriggeredEvent) => void,
): () => void {
  return subscribeBridge(KEYBINDS_EVENT_TRIGGERED, (rawEvent) => {
    const event = parseKeybindTriggeredEvent(rawEvent);
    if (!event) {
      return;
    }

    listener(event);
  });
}
