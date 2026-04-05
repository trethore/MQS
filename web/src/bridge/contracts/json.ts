export type JsonObject = Record<string, unknown>;

export function expectObject(value: unknown, label: string): JsonObject {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new TypeError(`Invalid ${label} payload.`);
  }

  return value as JsonObject;
}

export function readString(value: unknown, label: string): string {
  if (typeof value !== 'string') {
    throw new TypeError(`Invalid ${label} value.`);
  }

  return value;
}

export function readNullableString(value: unknown, label: string): string | null {
  if (value == null) {
    return null;
  }

  return readString(value, label);
}

export function readBoolean(value: unknown, label: string): boolean {
  if (typeof value !== 'boolean') {
    throw new TypeError(`Invalid ${label} value.`);
  }

  return value;
}

export function readNumber(value: unknown, label: string): number {
  if (typeof value !== 'number' || Number.isNaN(value)) {
    throw new TypeError(`Invalid ${label} value.`);
  }

  return value;
}

export function readArray(value: unknown, label: string): Array<unknown> {
  if (!Array.isArray(value)) {
    throw new TypeError(`Invalid ${label} value.`);
  }

  return value;
}
