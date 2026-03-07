import { BridgeError } from "@/bridge/core/bridgeError";
import { requestBridge, subscribeBridge } from "@/bridge/core/bridgeTransport";
import {
  OPTIONS_CHANNEL_GET,
  OPTIONS_CHANNEL_OPEN_PATH,
  OPTIONS_CHANNEL_SET,
  OPTIONS_EVENT_UPDATED,
  parseOptionsOpenPathResponse,
  parseOptionsSnapshot,
  parseOptionsUpdateResponse,
  type OptionsOpenPathRequest,
  type OptionsOpenPathResponse,
  type OptionsSnapshot,
  type OptionsUpdateRequest,
  type OptionsUpdateResponse,
} from "@/bridge/contracts/options";

export async function fetchOptionsSnapshot(): Promise<OptionsSnapshot> {
  const rawSnapshot = await requestBridge(OPTIONS_CHANNEL_GET, null);
  const snapshot = parseOptionsSnapshot(rawSnapshot);
  if (!snapshot) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid options snapshot payload.");
  }

  return snapshot;
}

export async function updateOptions(
  updateRequest: OptionsUpdateRequest,
): Promise<OptionsUpdateResponse> {
  const rawResponse = await requestBridge(OPTIONS_CHANNEL_SET, updateRequest);
  const response = parseOptionsUpdateResponse(rawResponse);
  if (!response) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid options update payload.");
  }

  return response;
}

export async function openPathWithIde(
  openPathRequest: OptionsOpenPathRequest,
): Promise<OptionsOpenPathResponse> {
  const rawResponse = await requestBridge(OPTIONS_CHANNEL_OPEN_PATH, openPathRequest);
  const response = parseOptionsOpenPathResponse(rawResponse);
  if (!response) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid options open path payload.");
  }

  return response;
}

export function subscribeToOptionsUpdated(
  listener: (snapshot: OptionsSnapshot) => void,
): () => void {
  return subscribeBridge(OPTIONS_EVENT_UPDATED, (rawSnapshot) => {
    const snapshot = parseOptionsSnapshot(rawSnapshot);
    if (!snapshot) {
      return;
    }

    listener(snapshot);
  });
}
