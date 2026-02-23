export type BridgeErrorCode = "BRIDGE_UNAVAILABLE" | "INVALID_PAYLOAD" | "REQUEST_FAILED";

export const BRIDGE_UNAVAILABLE_MESSAGE =
  "Graphene bridge is unavailable. Open this page from MQS in Minecraft.";

export class BridgeError extends Error {
  public readonly code: BridgeErrorCode;

  constructor(code: BridgeErrorCode, message: string) {
    super(message);
    this.name = "BridgeError";
    this.code = code;
  }
}

export function getBridgeErrorMessage(
  error: unknown,
  fallbackMessage = "Bridge request failed.",
): string {
  if (error instanceof BridgeError) {
    return error.message;
  }

  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }

  return fallbackMessage;
}
