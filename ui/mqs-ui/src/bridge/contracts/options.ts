import { asBoolean, asString, asStringArray, isObject } from "@/bridge/contracts/parsing";

export const OPTIONS_CHANNEL_GET = "mqs:options:get";
export const OPTIONS_CHANNEL_SET = "mqs:options:set";
export const OPTIONS_CHANNEL_OPEN_PATH = "mqs:options:open-path";
export const OPTIONS_EVENT_UPDATED = "mqs:options:updated";

export interface OptionsSnapshot {
  logRedirect: boolean;
  allowAllClasses: boolean;
  defaultIdeCommand: string;
  defaultProjectPath: string;
  additionalScriptDirectories: string[];
  defaultScriptDirectory: string;
}

export interface OptionsUpdateRequest {
  logRedirect?: boolean;
  allowAllClasses?: boolean;
  defaultIdeCommand?: string;
  defaultProjectPath?: string;
  additionalScriptDirectories?: string[];
}

export interface OptionsUpdateResponse {
  success: boolean;
  action: string;
  message: string;
  options: OptionsSnapshot | null;
}

export interface OptionsOpenPathRequest {
  path?: string;
  defaultIdeCommand?: string;
  target?: "ide" | "picker";
}

export interface OptionsOpenPathResponse {
  success: boolean;
  action: string;
  message: string;
  openedPath: string;
}

export function parseOptionsSnapshot(rawSnapshot: unknown): OptionsSnapshot | null {
  if (!isObject(rawSnapshot)) {
    return null;
  }

  return {
    logRedirect: asBoolean(rawSnapshot.logRedirect),
    allowAllClasses: asBoolean(rawSnapshot.allowAllClasses),
    defaultIdeCommand: asString(rawSnapshot.defaultIdeCommand),
    defaultProjectPath: asString(rawSnapshot.defaultProjectPath),
    additionalScriptDirectories: asStringArray(rawSnapshot.additionalScriptDirectories),
    defaultScriptDirectory: asString(rawSnapshot.defaultScriptDirectory),
  };
}

export function parseOptionsUpdateResponse(rawResponse: unknown): OptionsUpdateResponse | null {
  if (!isObject(rawResponse)) {
    return null;
  }

  return {
    success: asBoolean(rawResponse.success),
    action: asString(rawResponse.action),
    message: asString(rawResponse.message),
    options: parseOptionsSnapshot(rawResponse.options),
  };
}

export function parseOptionsOpenPathResponse(rawResponse: unknown): OptionsOpenPathResponse | null {
  if (!isObject(rawResponse)) {
    return null;
  }

  return {
    success: asBoolean(rawResponse.success),
    action: asString(rawResponse.action),
    message: asString(rawResponse.message),
    openedPath: asString(rawResponse.openedPath),
  };
}
