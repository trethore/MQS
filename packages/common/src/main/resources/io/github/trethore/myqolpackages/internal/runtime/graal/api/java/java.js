export default function createJavaApi(
  defineClass,
  defineInterface,
  voidType,
  booleanType,
  byteType,
  shortType,
  intType,
  longType,
  floatType,
  doubleType,
  charType,
  privateVisibility,
  packageVisibility,
  protectedVisibility,
  publicVisibility,
) {
  return Object.freeze({
    defineClass,
    defineInterface,
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
    visibility: Object.freeze({
      PRIVATE: privateVisibility,
      PACKAGE: packageVisibility,
      PROTECTED: protectedVisibility,
      PUBLIC: publicVisibility,
    }),
  });
}
