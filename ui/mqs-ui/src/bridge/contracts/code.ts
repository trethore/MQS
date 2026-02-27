import { asBoolean, asString, isObject } from "@/bridge/contracts/parsing";

export const CODE_CHANNEL_PREPARE = "mqs:code:prepare";

export interface CodePrepareResponse {
  copied: boolean;
  modDirPath: string;
}

export function parseCodePrepareResponse(rawResponse: unknown): CodePrepareResponse | null {
  if (!isObject(rawResponse)) {
    return null;
  }

  return {
    copied: asBoolean(rawResponse.copied),
    modDirPath: asString(rawResponse.modDirPath),
  };
}
