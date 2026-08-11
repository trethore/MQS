export default function createJavaApi(
  voidType,
  booleanType,
  byteType,
  shortType,
  intType,
  longType,
  floatType,
  doubleType,
  charType,
) {
  return Object.freeze({
    type: Object.freeze({
      void: voidType,
      boolean: booleanType,
      byte: byteType,
      short: shortType,
      int: intType,
      long: longType,
      float: floatType,
      double: doubleType,
      char: charType,
    }),
  });
}
