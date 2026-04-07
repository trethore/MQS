import {
  expectObject,
  readArray,
  readBoolean,
  readOptionalString,
  readNumber,
  readString,
} from '@/bridge/contracts/json';

export const SCRIPTS_BRIDGE_CHANNELS = {
  list: 'mqs:scripts:list',
  info: 'mqs:scripts:info',
  toggle: 'mqs:scripts:toggle',
  refresh: 'mqs:scripts:refresh',
  refreshAndReenable: 'mqs:scripts:refresh-and-reenable',
  disableAll: 'mqs:scripts:disable-all',
} as const;

export const SCRIPTS_BRIDGE_EVENTS = {
  updated: 'mqs:scripts:updated',
} as const;

export type ScriptIdRequest = {
  scriptId: string;
};

export type ScriptStateResponse = {
  id: string;
  scriptName: string;
  version: string | undefined;
  mainClass: string | undefined;
  path: string;
  running: boolean;
};

export type ScriptsSnapshotResponse = {
  scripts: Array<ScriptStateResponse>;
  runningCount: number;
  totalCount: number;
};

export type ScriptOperationResponse = {
  success: boolean;
  action: string;
  message: string;
  script: ScriptStateResponse | undefined;
  snapshot: ScriptsSnapshotResponse;
};

function parseScriptStateResponse(value: unknown): ScriptStateResponse {
  const objectValue = expectObject(value, 'script state');
  return {
    id: readString(objectValue.id, 'script state id'),
    scriptName: readString(objectValue.scriptName, 'script state scriptName'),
    version: readOptionalString(objectValue.version, 'script state version'),
    mainClass: readOptionalString(objectValue.mainClass, 'script state mainClass'),
    path: readString(objectValue.path, 'script state path'),
    running: readBoolean(objectValue.running, 'script state running'),
  };
}

export function parseScriptsSnapshotResponse(value: unknown): ScriptsSnapshotResponse {
  const objectValue = expectObject(value, 'scripts snapshot');
  return {
    scripts: readArray(objectValue.scripts, 'scripts snapshot scripts').map((entry) => {
      return parseScriptStateResponse(entry);
    }),
    runningCount: readNumber(objectValue.runningCount, 'scripts snapshot runningCount'),
    totalCount: readNumber(objectValue.totalCount, 'scripts snapshot totalCount'),
  };
}

export function parseScriptOperationResponse(value: unknown): ScriptOperationResponse {
  const objectValue = expectObject(value, 'script operation');
  return {
    success: readBoolean(objectValue.success, 'script operation success'),
    action: readString(objectValue.action, 'script operation action'),
    message: readString(objectValue.message, 'script operation message'),
    script:
      objectValue.script == undefined ? undefined : parseScriptStateResponse(objectValue.script),
    snapshot: parseScriptsSnapshotResponse(objectValue.snapshot),
  };
}
