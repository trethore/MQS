import { asBoolean, asFiniteNumber, asString, isObject } from "@/bridge/contracts/parsing";

export const COMMANDS_CHANNEL_LIST = "mqs:commands:list";

export interface ManagedCommand {
  name: string;
  scriptId: string;
  scriptName: string;
  queued: boolean;
}

export interface CommandsSnapshot {
  commands: ManagedCommand[];
  totalCount: number;
}

function parseManagedCommand(rawCommand: unknown): ManagedCommand | null {
  if (!isObject(rawCommand)) {
    return null;
  }

  const name = asString(rawCommand.name);
  if (!name) {
    return null;
  }

  return {
    name,
    scriptId: asString(rawCommand.scriptId),
    scriptName: asString(rawCommand.scriptName),
    queued: asBoolean(rawCommand.queued),
  };
}

export function parseCommandsSnapshot(rawSnapshot: unknown): CommandsSnapshot | null {
  if (!isObject(rawSnapshot) || !Array.isArray(rawSnapshot.commands)) {
    return null;
  }

  const commands = rawSnapshot.commands
    .map((rawCommand) => parseManagedCommand(rawCommand))
    .filter((command): command is ManagedCommand => command !== null);

  return {
    commands,
    totalCount: asFiniteNumber(rawSnapshot.totalCount, commands.length),
  };
}
