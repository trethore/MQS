import {
  CODE_BRIDGE_CHANNELS,
  parseCodePrepareResponse,
  type CodePrepareResponse,
} from '@/bridge/contracts/code';
import {
  requestGrapheneBridge,
  type GrapheneBridgeWaitOptions,
} from '@/bridge/core/graphene-bridge';

export async function prepareCodeWorkspace(
  options?: GrapheneBridgeWaitOptions
): Promise<CodePrepareResponse> {
  const payload = await requestGrapheneBridge<unknown>(
    CODE_BRIDGE_CHANNELS.prepare,
    undefined,
    options
  );
  return parseCodePrepareResponse(payload);
}
