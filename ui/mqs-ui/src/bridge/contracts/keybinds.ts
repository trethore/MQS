import { asBoolean, asFiniteNumber, asString, isObject } from "@/bridge/contracts/parsing";

export const KEYBINDS_CHANNEL_LIST = "mqs:keybinds:list";
export const KEYBINDS_CHANNEL_CREATE = "mqs:keybinds:create";
export const KEYBINDS_CHANNEL_UPDATE = "mqs:keybinds:update";
export const KEYBINDS_CHANNEL_DELETE = "mqs:keybinds:delete";

export const KEYBINDS_EVENT_UPDATED = "mqs:keybinds:updated";
export const KEYBINDS_EVENT_TRIGGERED = "mqs:keybinds:triggered";

export interface KeybindState {
  scope: string;
  scriptId: string;
  scriptName: string;
  name: string;
  keyCode: number;
  keyName: string;
}

export interface KeybindSnapshot {
  keybinds: KeybindState[];
  totalCount: number;
  scriptCount: number;
  hostCount: number;
}

export interface KeybindMutationRequest {
  scope?: string;
  scriptId?: string;
  name?: string;
  keyCode?: number;
  repeatable?: boolean;
  debounceMillis?: number;
}

export interface KeybindMutationResponse {
  success: boolean;
  action: string;
  message: string;
  keybind: KeybindState | null;
  snapshot: KeybindSnapshot | null;
}

export interface KeybindTriggeredEvent {
  name: string;
  keyCode: number;
  keyName: string;
}

function parseKeybindState(rawKeybind: unknown): KeybindState | null {
  if (!isObject(rawKeybind)) {
    return null;
  }

  const name = asString(rawKeybind.name);
  if (!name) {
    return null;
  }

  return {
    scope: asString(rawKeybind.scope),
    scriptId: asString(rawKeybind.scriptId),
    scriptName: asString(rawKeybind.scriptName),
    name,
    keyCode: asFiniteNumber(rawKeybind.keyCode, -1),
    keyName: asString(rawKeybind.keyName),
  };
}

export function parseKeybindSnapshot(rawSnapshot: unknown): KeybindSnapshot | null {
  if (!isObject(rawSnapshot) || !Array.isArray(rawSnapshot.keybinds)) {
    return null;
  }

  const keybinds = rawSnapshot.keybinds
    .map((rawKeybind) => parseKeybindState(rawKeybind))
    .filter((keybind): keybind is KeybindState => keybind !== null);

  return {
    keybinds,
    totalCount: asFiniteNumber(rawSnapshot.totalCount, keybinds.length),
    scriptCount: asFiniteNumber(rawSnapshot.scriptCount, 0),
    hostCount: asFiniteNumber(rawSnapshot.hostCount, 0),
  };
}

export function parseKeybindMutationResponse(rawResponse: unknown): KeybindMutationResponse | null {
  if (!isObject(rawResponse)) {
    return null;
  }

  return {
    success: asBoolean(rawResponse.success),
    action: asString(rawResponse.action),
    message: asString(rawResponse.message),
    keybind: parseKeybindState(rawResponse.keybind),
    snapshot: parseKeybindSnapshot(rawResponse.snapshot),
  };
}

export function parseKeybindTriggeredEvent(rawEvent: unknown): KeybindTriggeredEvent | null {
  if (!isObject(rawEvent)) {
    return null;
  }

  const name = asString(rawEvent.name);
  if (!name) {
    return null;
  }

  return {
    name,
    keyCode: asFiniteNumber(rawEvent.keyCode, -1),
    keyName: asString(rawEvent.keyName),
  };
}
