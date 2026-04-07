import { expectObject, readArray, readBoolean, readString } from '@/bridge/contracts/json';

export const OPTIONS_BRIDGE_CHANNELS = {
  get: 'mqs:options:get',
  set: 'mqs:options:set',
  openPath: 'mqs:options:open-path',
} as const;

export const OPTIONS_BRIDGE_EVENTS = {
  updated: 'mqs:options:updated',
} as const;

export type OptionsUpdateRequest = {
  logRedirect?: boolean;
  allowAllClasses?: boolean;
  defaultIdeCommand?: string;
  defaultProjectPath?: string;
  additionalScriptDirectories?: Array<string>;
};

export type OptionsSnapshotResponse = {
  logRedirect: boolean;
  allowAllClasses: boolean;
  defaultIdeCommand: string;
  defaultProjectPath: string;
  additionalScriptDirectories: Array<string>;
  defaultScriptDirectory: string;
};

export type OptionsUpdateResponse = {
  success: boolean;
  action: string;
  message: string;
  options: OptionsSnapshotResponse;
};

export type OptionsOpenPathRequest = {
  path?: string;
  defaultIdeCommand?: string;
  target?: string;
};

export type OptionsOpenPathResponse = {
  success: boolean;
  action: string;
  message: string;
  openedPath: string;
};

export function parseOptionsSnapshotResponse(value: unknown): OptionsSnapshotResponse {
  const objectValue = expectObject(value, 'options snapshot');
  return {
    logRedirect: readBoolean(objectValue.logRedirect, 'options snapshot logRedirect'),
    allowAllClasses: readBoolean(objectValue.allowAllClasses, 'options snapshot allowAllClasses'),
    defaultIdeCommand: readString(
      objectValue.defaultIdeCommand,
      'options snapshot defaultIdeCommand'
    ),
    defaultProjectPath: readString(
      objectValue.defaultProjectPath,
      'options snapshot defaultProjectPath'
    ),
    additionalScriptDirectories: readArray(
      objectValue.additionalScriptDirectories,
      'options snapshot additionalScriptDirectories'
    ).map((entry) => {
      return readString(entry, 'options snapshot additionalScriptDirectories entry');
    }),
    defaultScriptDirectory: readString(
      objectValue.defaultScriptDirectory,
      'options snapshot defaultScriptDirectory'
    ),
  };
}

export function parseOptionsUpdateResponse(value: unknown): OptionsUpdateResponse {
  const objectValue = expectObject(value, 'options update');
  return {
    success: readBoolean(objectValue.success, 'options update success'),
    action: readString(objectValue.action, 'options update action'),
    message: readString(objectValue.message, 'options update message'),
    options: parseOptionsSnapshotResponse(objectValue.options),
  };
}

export function parseOptionsOpenPathResponse(value: unknown): OptionsOpenPathResponse {
  const objectValue = expectObject(value, 'options open path');
  return {
    success: readBoolean(objectValue.success, 'options open path success'),
    action: readString(objectValue.action, 'options open path action'),
    message: readString(objectValue.message, 'options open path message'),
    openedPath: readString(objectValue.openedPath, 'options open path openedPath'),
  };
}
