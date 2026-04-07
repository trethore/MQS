export function normalizePathListValue(value: string): Array<string> {
  return value
    .split(';')
    .map((entry) => entry.trim())
    .filter((entry) => entry.length > 0);
}

export function formatPathListValue(paths: ReadonlyArray<string>): string {
  return paths.join(';');
}

export function areStringArraysEqual(
  left: ReadonlyArray<string>,
  right: ReadonlyArray<string>
): boolean {
  return left.length === right.length && left.every((entry, index) => entry === right[index]);
}
