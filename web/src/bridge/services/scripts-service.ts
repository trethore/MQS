import {
  parseScriptOperationResponse,
  parseScriptsSnapshotResponse,
  SCRIPTS_BRIDGE_CHANNELS,
  SCRIPTS_BRIDGE_EVENTS,
  type ScriptIdRequest,
  type ScriptOperationResponse,
  type ScriptStateResponse,
  type ScriptsSnapshotResponse,
} from '@/bridge/contracts/scripts';
import {
  requestGrapheneBridge,
  subscribeToGrapheneBridgeEvent,
  type GrapheneBridgeWaitOptions,
} from '@/bridge/core/graphene-bridge';

export type ScriptListItem = {
  id: string;
  name: string;
  version: string | null;
  mainClass: string | null;
  path: string;
  enabled: boolean;
};

export type ScriptsSnapshot = {
  scripts: Array<ScriptListItem>;
  runningCount: number;
  totalCount: number;
};

export type ScriptOperationResult = {
  success: boolean;
  action: string;
  message: string;
  script: ScriptListItem | null;
  snapshot: ScriptsSnapshot;
};

function normalizeNonEmptyString(value: string | null | undefined, fallbackValue: string): string {
  const normalizedValue = value?.trim();
  return normalizedValue && normalizedValue.length > 0 ? normalizedValue : fallbackValue;
}

function mapScript(script: ScriptStateResponse): ScriptListItem {
  return {
    id: script.id,
    name: normalizeNonEmptyString(script.scriptName, script.id),
    version: script.version,
    mainClass: script.mainClass,
    path: normalizeNonEmptyString(script.path, script.id),
    enabled: script.running,
  };
}

function mapSnapshot(snapshot: ScriptsSnapshotResponse): ScriptsSnapshot {
  return {
    scripts: snapshot.scripts.map(mapScript),
    runningCount: snapshot.runningCount,
    totalCount: snapshot.totalCount,
  };
}

function mapOperationResult(result: ScriptOperationResponse): ScriptOperationResult {
  return {
    success: result.success,
    action: result.action,
    message: result.message,
    script: result.script ? mapScript(result.script) : null,
    snapshot: mapSnapshot(result.snapshot),
  };
}

async function requestScriptsSnapshot(
  channel: string,
  options?: GrapheneBridgeWaitOptions
): Promise<ScriptsSnapshot> {
  const payload = await requestGrapheneBridge<unknown>(channel, null, options);
  return mapSnapshot(parseScriptsSnapshotResponse(payload));
}

async function requestScriptOperation(channel: string, payload: ScriptIdRequest | null): Promise<ScriptOperationResult> {
  const responsePayload = await requestGrapheneBridge<unknown>(channel, payload);
  return mapOperationResult(parseScriptOperationResponse(responsePayload));
}

export async function listScripts(options?: GrapheneBridgeWaitOptions): Promise<ScriptsSnapshot> {
  return await requestScriptsSnapshot(SCRIPTS_BRIDGE_CHANNELS.list, options);
}

export async function toggleScript(scriptId: string): Promise<ScriptOperationResult> {
  return await requestScriptOperation(SCRIPTS_BRIDGE_CHANNELS.toggle, { scriptId });
}

export async function refreshScripts(): Promise<ScriptOperationResult> {
  return await requestScriptOperation(SCRIPTS_BRIDGE_CHANNELS.refresh, null);
}

export async function refreshAndReenableScripts(): Promise<ScriptOperationResult> {
  return await requestScriptOperation(SCRIPTS_BRIDGE_CHANNELS.refreshAndReenable, null);
}

export async function disableAllScripts(): Promise<ScriptOperationResult> {
  return await requestScriptOperation(SCRIPTS_BRIDGE_CHANNELS.disableAll, null);
}

export async function subscribeToScriptsUpdated(
  listener: (snapshot: ScriptsSnapshot) => void,
  options?: GrapheneBridgeWaitOptions
): Promise<() => void> {
  return await subscribeToGrapheneBridgeEvent(
    SCRIPTS_BRIDGE_EVENTS.updated,
    (payload) => {
      listener(mapSnapshot(parseScriptsSnapshotResponse(payload)));
    },
    options
  );
}
