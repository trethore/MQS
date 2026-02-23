import { asBoolean, asString, asStringArray, isObject } from "@/bridge/contracts/parsing";

export const OPTIONS_CHANNEL_GET = "mqs:options:get";
export const OPTIONS_CHANNEL_SET = "mqs:options:set";
export const OPTIONS_EVENT_UPDATED = "mqs:options:updated";

export interface OptionsSnapshot {
  logRedirect: boolean;
  allowAllClasses: boolean;
  defaultIdeCommand: string;
  additionalScriptDirectories: string[];
}

export interface OptionsUpdateRequest {
  logRedirect?: boolean;
  allowAllClasses?: boolean;
  defaultIdeCommand?: string;
}

export interface OptionsUpdateResponse {
  success: boolean;
  action: string;
  message: string;
  options: OptionsSnapshot | null;
}

export function parseOptionsSnapshot(rawSnapshot: unknown): OptionsSnapshot | null {
  if (!isObject(rawSnapshot)) {
    return null;
  }

  return {
    logRedirect: asBoolean(rawSnapshot.logRedirect),
    allowAllClasses: asBoolean(rawSnapshot.allowAllClasses),
    defaultIdeCommand: asString(rawSnapshot.defaultIdeCommand),
    additionalScriptDirectories: asStringArray(rawSnapshot.additionalScriptDirectories),
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
