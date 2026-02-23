import { asBoolean, asFiniteNumber, asString, isObject } from "@/bridge/contracts/parsing";

export const SCRIPTS_CHANNEL_LIST = "mqs:scripts:list";
export const SCRIPTS_CHANNEL_INFO = "mqs:scripts:info";
export const SCRIPTS_CHANNEL_TOGGLE = "mqs:scripts:toggle";
export const SCRIPTS_CHANNEL_REFRESH = "mqs:scripts:refresh";
export const SCRIPTS_CHANNEL_REFRESH_AND_REENABLE = "mqs:scripts:refresh-and-reenable";
export const SCRIPTS_CHANNEL_DISABLE_ALL = "mqs:scripts:disable-all";
export const SCRIPTS_EVENT_UPDATED = "mqs:scripts:updated";

export interface ScriptState {
  id: string;
  moduleName: string;
  version: string;
  mainClass: string;
  path: string;
  running: boolean;
}

export interface ScriptsSnapshot {
  scripts: ScriptState[];
  runningCount: number;
  totalCount: number;
}

export interface ScriptOperation {
  success: boolean;
  action: string;
  message: string;
  script: ScriptState | null;
  snapshot: ScriptsSnapshot | null;
}

function parseScriptState(rawScript: unknown): ScriptState | null {
  if (!isObject(rawScript)) {
    return null;
  }

  const id = asString(rawScript.id);
  if (!id) {
    return null;
  }

  const moduleName = asString(rawScript.moduleName);
  return {
    id,
    moduleName: moduleName || id,
    version: asString(rawScript.version),
    mainClass: asString(rawScript.mainClass),
    path: asString(rawScript.path),
    running: asBoolean(rawScript.running),
  };
}

export function parseScriptsSnapshot(rawSnapshot: unknown): ScriptsSnapshot | null {
  if (!isObject(rawSnapshot) || !Array.isArray(rawSnapshot.scripts)) {
    return null;
  }

  const scripts = rawSnapshot.scripts
    .map((rawScript) => parseScriptState(rawScript))
    .filter((script): script is ScriptState => script !== null);

  const defaultRunningCount = scripts.filter((script) => script.running).length;

  return {
    scripts,
    runningCount: asFiniteNumber(rawSnapshot.runningCount, defaultRunningCount),
    totalCount: asFiniteNumber(rawSnapshot.totalCount, scripts.length),
  };
}

export function parseScriptOperation(rawOperation: unknown): ScriptOperation | null {
  if (!isObject(rawOperation)) {
    return null;
  }

  return {
    success: asBoolean(rawOperation.success),
    action: asString(rawOperation.action),
    message: asString(rawOperation.message),
    script: parseScriptState(rawOperation.script),
    snapshot: parseScriptsSnapshot(rawOperation.snapshot),
  };
}
