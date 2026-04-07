import {
  OPTIONS_BRIDGE_CHANNELS,
  OPTIONS_BRIDGE_EVENTS,
  parseOptionsOpenPathResponse,
  parseOptionsSnapshotResponse,
  parseOptionsUpdateResponse,
  type OptionsOpenPathRequest,
  type OptionsOpenPathResponse,
  type OptionsSnapshotResponse,
  type OptionsUpdateRequest,
  type OptionsUpdateResponse,
} from '@/bridge/contracts/options';
import {
  requestGrapheneBridge,
  subscribeToGrapheneBridgeEvent,
  type GrapheneBridgeWaitOptions,
} from '@/bridge/core/graphene-bridge';

export type OptionsSnapshot = OptionsSnapshotResponse;

export type OptionsUpdateResult = OptionsUpdateResponse;

export type OptionsOpenPathResult = OptionsOpenPathResponse;

function mapSnapshot(snapshot: OptionsSnapshotResponse): OptionsSnapshot {
  return {
    ...snapshot,
    additionalScriptDirectories: [...snapshot.additionalScriptDirectories],
  };
}

function mapUpdateResult(result: OptionsUpdateResponse): OptionsUpdateResult {
  return {
    ...result,
    options: mapSnapshot(result.options),
  };
}

export async function getOptionsSnapshot(
  options?: GrapheneBridgeWaitOptions
): Promise<OptionsSnapshot> {
  const payload = await requestGrapheneBridge<unknown>(
    OPTIONS_BRIDGE_CHANNELS.get,
    undefined,
    options
  );
  return mapSnapshot(parseOptionsSnapshotResponse(payload));
}

export async function updateOptions(payload: OptionsUpdateRequest): Promise<OptionsUpdateResult> {
  const responsePayload = await requestGrapheneBridge<unknown>(
    OPTIONS_BRIDGE_CHANNELS.set,
    payload
  );
  return mapUpdateResult(parseOptionsUpdateResponse(responsePayload));
}

export async function openOptionsPath(
  payload: OptionsOpenPathRequest,
  options?: GrapheneBridgeWaitOptions
): Promise<OptionsOpenPathResult> {
  const responsePayload = await requestGrapheneBridge<unknown>(
    OPTIONS_BRIDGE_CHANNELS.openPath,
    payload,
    options
  );
  return parseOptionsOpenPathResponse(responsePayload);
}

export async function subscribeToOptionsUpdated(
  listener: (snapshot: OptionsSnapshot) => void,
  options?: GrapheneBridgeWaitOptions
): Promise<() => void> {
  return await subscribeToGrapheneBridgeEvent(
    OPTIONS_BRIDGE_EVENTS.updated,
    (payload) => {
      listener(mapSnapshot(parseOptionsSnapshotResponse(payload)));
    },
    options
  );
}
