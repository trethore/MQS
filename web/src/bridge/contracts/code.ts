import { expectObject, readBoolean, readString } from '@/bridge/contracts/json';

export const CODE_BRIDGE_CHANNELS = {
  prepare: 'mqs:code:prepare',
} as const;

export const VSCODE_WEB_URL = 'https://vscode.dev/' as const;

export type CodePrepareResponse = {
  copied: boolean;
  modDirPath: string;
};

export function parseCodePrepareResponse(value: unknown): CodePrepareResponse {
  const objectValue = expectObject(value, 'code prepare');
  return {
    copied: readBoolean(objectValue.copied, 'code prepare copied'),
    modDirPath: readString(objectValue.modDirPath, 'code prepare modDirPath'),
  };
}
