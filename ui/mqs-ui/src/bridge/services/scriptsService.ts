import { BridgeError } from "@/bridge/core/bridgeError";
import { requestBridge, subscribeBridge } from "@/bridge/core/bridgeTransport";
import {
  SCRIPTS_CHANNEL_DISABLE_ALL,
  SCRIPTS_CHANNEL_INFO,
  parseScriptOperation,
  parseScriptsSnapshot,
  SCRIPTS_CHANNEL_LIST,
  SCRIPTS_CHANNEL_REFRESH,
  SCRIPTS_CHANNEL_REFRESH_AND_REENABLE,
  SCRIPTS_CHANNEL_TOGGLE,
  SCRIPTS_EVENT_UPDATED,
  type ScriptOperation,
  type ScriptsSnapshot,
} from "@/bridge/contracts/scripts";

export async function fetchScriptsSnapshot(): Promise<ScriptsSnapshot> {
  const rawSnapshot = await requestBridge(SCRIPTS_CHANNEL_LIST, null);
  const snapshot = parseScriptsSnapshot(rawSnapshot);
  if (!snapshot) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid scripts snapshot payload.");
  }

  return snapshot;
}

export function subscribeToScriptsUpdated(
  listener: (snapshot: ScriptsSnapshot) => void,
): () => void {
  return subscribeBridge(SCRIPTS_EVENT_UPDATED, (rawSnapshot) => {
    const snapshot = parseScriptsSnapshot(rawSnapshot);
    if (!snapshot) {
      return;
    }

    listener(snapshot);
  });
}

async function runScriptOperation(channel: string, payload: unknown): Promise<ScriptOperation> {
  const rawOperation = await requestBridge(channel, payload);
  const operation = parseScriptOperation(rawOperation);
  if (!operation) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid scripts operation payload.");
  }

  return operation;
}

export function fetchScriptInfo(scriptId: string): Promise<ScriptOperation> {
  return runScriptOperation(SCRIPTS_CHANNEL_INFO, { scriptId });
}

export function toggleScript(scriptId: string): Promise<ScriptOperation> {
  return runScriptOperation(SCRIPTS_CHANNEL_TOGGLE, { scriptId });
}

export function refreshScripts(): Promise<ScriptOperation> {
  return runScriptOperation(SCRIPTS_CHANNEL_REFRESH, null);
}

export function refreshAndReenableScripts(): Promise<ScriptOperation> {
  return runScriptOperation(SCRIPTS_CHANNEL_REFRESH_AND_REENABLE, null);
}

export function disableAllScripts(): Promise<ScriptOperation> {
  return runScriptOperation(SCRIPTS_CHANNEL_DISABLE_ALL, null);
}
