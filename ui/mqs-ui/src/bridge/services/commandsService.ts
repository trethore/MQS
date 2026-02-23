import { BridgeError } from "@/bridge/core/bridgeError";
import { requestBridge } from "@/bridge/core/bridgeTransport";
import {
  COMMANDS_CHANNEL_LIST,
  parseCommandsSnapshot,
  type CommandsSnapshot,
} from "@/bridge/contracts/commands";

export async function fetchCommandsSnapshot(): Promise<CommandsSnapshot> {
  const rawSnapshot = await requestBridge(COMMANDS_CHANNEL_LIST, null);
  const snapshot = parseCommandsSnapshot(rawSnapshot);
  if (!snapshot) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid commands snapshot payload.");
  }

  return snapshot;
}
