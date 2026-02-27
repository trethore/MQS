import { BridgeError } from "@/bridge/core/bridgeError";
import { requestBridge } from "@/bridge/core/bridgeTransport";
import {
  CODE_CHANNEL_PREPARE,
  parseCodePrepareResponse,
  type CodePrepareResponse,
} from "@/bridge/contracts/code";

export async function prepareCodeWorkspace(): Promise<CodePrepareResponse> {
  const rawResponse = await requestBridge(CODE_CHANNEL_PREPARE, null);
  const response = parseCodePrepareResponse(rawResponse);
  if (!response) {
    throw new BridgeError("INVALID_PAYLOAD", "Invalid code prepare payload.");
  }

  return response;
}
