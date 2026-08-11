if (
  !Object.isFrozen(mqp.java) ||
  !Object.isFrozen(mqp.java.type) ||
  !Object.isFrozen(mqp.java.visibility)
) {
  throw new Error("mutable Java API");
}

const originalJavaApi = mqp.java;
try {
  mqp.java = {};
} catch (error) {}
if (mqp.java !== originalJavaApi) throw new Error("mutable Java API member");
const originalJavaTypes = mqp.java.type;
try {
  mqp.java.type = {};
} catch (error) {}
if (mqp.java.type !== originalJavaTypes) throw new Error("mutable Java type API");
const originalIntType = mqp.java.type.int;
try {
  mqp.java.type.int = null;
} catch (error) {}
if (mqp.java.type.int !== originalIntType) throw new Error("mutable Java primitive type");

const visibilityNames = ["PRIVATE", "PACKAGE", "PROTECTED", "PUBLIC"];
if (Object.keys(mqp.java.visibility).join(",") !== visibilityNames.join(",")) {
  throw new Error("unexpected Java visibility members");
}
if (typeof mqp.java.defineClass !== "function" || typeof mqp.java.defineInterface !== "function") {
  throw new Error("missing Java type definition APIs");
}

const HostString = Java.type("java.lang.String");
if (HostString.valueOf(42) !== "42") throw new Error("host lookup failed");
const imported = importClass("java.lang.Double");
const packaged = packages.java.lang.Double;
if (!imported || imported !== packaged) throw new Error("class proxies differ");

const primitiveTypes = {
  void: Java.type("java.lang.Void").TYPE,
  boolean: Java.type("java.lang.Boolean").TYPE,
  byte: Java.type("java.lang.Byte").TYPE,
  short: Java.type("java.lang.Short").TYPE,
  int: Java.type("java.lang.Integer").TYPE,
  long: Java.type("java.lang.Long").TYPE,
  float: Java.type("java.lang.Float").TYPE,
  double: Java.type("java.lang.Double").TYPE,
  char: Java.type("java.lang.Character").TYPE,
};
if (Object.keys(mqp.java.type).join(",") !== Object.keys(primitiveTypes).join(",")) {
  throw new Error("unexpected Java primitive type members");
}
for (const [name, type] of Object.entries(primitiveTypes)) {
  if (mqp.java.type[name] !== type) throw new Error(`invalid Java primitive type: ${name}`);
}
if (!importClass("java.lang.Integer").TYPE._equals(mqp.java.type.int)) {
  throw new Error("Java primitive type alias differs");
}
const valueOfInt = importClass("java.lang.String")._class.getMethod("valueOf", mqp.java.type.int);
if (valueOfInt.invoke(null, 42) !== "42") throw new Error("primitive reflection lookup failed");

const GeneratedCounter = mqp.java
  .defineClass("GeneratedCounter")
  .field({
    name: "value",
    type: mqp.java.type.int,
    value: 2,
    visibility: mqp.java.visibility.PUBLIC,
  })
  .constructor({
    argTypes: mqp.java.type.int,
    visibility: mqp.java.visibility.PUBLIC,
    implementation: function ($self, $super, value) {
      $super();
      $self.value = value;
    },
  })
  .method({
    name: "increment",
    returnType: mqp.java.type.int,
    argTypes: mqp.java.type.int,
    visibility: mqp.java.visibility.PUBLIC,
    implementation: function ($self, $super, amount) {
      $self.value += amount;
      return $self.value;
    },
  })
  .field({
    name: "NAME",
    type: Java.type("java.lang.String"),
    value: "counter",
    visibility: mqp.java.visibility.PUBLIC,
    isStatic: true,
    isFinal: true,
  })
  .field({
    name: "id",
    type: mqp.java.type.int,
    value: 5,
    visibility: mqp.java.visibility.PUBLIC,
    isFinal: true,
  })
  .method({
    name: "twice",
    returnType: mqp.java.type.int,
    argTypes: mqp.java.type.int,
    visibility: mqp.java.visibility.PUBLIC,
    isStatic: true,
    implementation: function (value) {
      return value * 2;
    },
  })
  .build();
const counter = new GeneratedCounter(4);
if (
  counter.value !== 4 ||
  counter.increment(3) !== 7 ||
  counter.value !== 7 ||
  GeneratedCounter.NAME !== "counter" ||
  GeneratedCounter.twice(8) !== 16
) {
  throw new Error("generated class did not execute JavaScript callbacks");
}
let generatedFinalWriteRejected = false;
try {
  counter.id = 6;
} catch (error) {
  generatedFinalWriteRejected = true;
}
if (!generatedFinalWriteRejected || counter.id !== 5) {
  throw new Error("generated final field was writable");
}

const GeneratedMultiplier = mqp.java
  .defineInterface("GeneratedMultiplier")
  .field({
    name: "FACTOR",
    type: mqp.java.type.int,
    value: 3,
  })
  .method({
    name: "triple",
    returnType: mqp.java.type.int,
    argTypes: mqp.java.type.int,
    visibility: mqp.java.visibility.PRIVATE,
    implementation: function ($self, $super, value) {
      return value * GeneratedMultiplier.FACTOR;
    },
  })
  .method({
    name: "multiply",
    returnType: mqp.java.type.int,
    argTypes: [mqp.java.type.int, mqp.java.type.int],
    implementation: function ($self, $super, left, right) {
      return left * right;
    },
  })
  .method({
    name: "applyFactor",
    returnType: mqp.java.type.int,
    argTypes: mqp.java.type.int,
    implementation: function ($self, $super, value) {
      return $self.triple(value);
    },
  })
  .method({
    name: "identity",
    returnType: mqp.java.type.int,
    argTypes: mqp.java.type.int,
    isStatic: true,
    implementation: function (value) {
      return value;
    },
  })
  .build();
const GeneratedMultiplierImpl = mqp.java
  .defineClass("GeneratedMultiplierImpl")
  .implements(GeneratedMultiplier)
  .build();
const multiplier = new GeneratedMultiplierImpl();
if (multiplier.multiply(6, 7) !== 42) throw new Error("generated default interface method failed");
if (
  GeneratedMultiplier.FACTOR !== 3 ||
  GeneratedMultiplier.identity(5) !== 5 ||
  multiplier.applyFactor(4) !== 12
) {
  throw new Error("generated interface field, static method, or private method failed");
}

const Fixture = Java.type(
  "io.github.trethore.myqolpackages.internal.runtime.graal.GeneratedTypeFixture",
);
const Base = Java.type(
  "io.github.trethore.myqolpackages.internal.runtime.graal.GeneratedTypeFixture$Base",
);
const GeneratedDerived = mqp.java
  .defineClass("GeneratedDerived")
  .extends(Base)
  .field({
    name: "value",
    type: Java.type("java.lang.String"),
    value: "generated",
    visibility: mqp.java.visibility.PUBLIC,
  })
  .constructor({
    argTypes: mqp.java.type.int,
    visibility: mqp.java.visibility.PUBLIC,
    implementation: function ($self, $super, amount) {
      $super(amount);
      $self.value = `derived:${amount}`;
    },
  })
  .method({
    name: "greet",
    returnType: Java.type("java.lang.String"),
    argTypes: Java.type("java.lang.String"),
    visibility: mqp.java.visibility.PUBLIC,
    override: true,
    implementation: function ($self, $super, name) {
      return `${$super.greet(name)}:${$super.value}:${$self.value}`;
    },
  })
  .build();
const derived = new GeneratedDerived(9);
if (derived.greet("test") !== "base:test:number:9:derived:9") {
  throw new Error("generated superclass call or hidden field access failed");
}
if (derived.callsVirtual("virtual") !== "base:virtual:number:9:derived:9") {
  throw new Error("superclass virtual dispatch did not reach generated override");
}

const ParentDefault = Java.type(
  "io.github.trethore.myqolpackages.internal.runtime.graal.GeneratedTypeFixture$ParentDefault",
);
const GeneratedChildDefault = mqp.java
  .defineInterface("GeneratedChildDefault")
  .extends(ParentDefault)
  .method({
    name: "label",
    returnType: Java.type("java.lang.String"),
    visibility: mqp.java.visibility.PUBLIC,
    override: true,
    implementation: function ($self, $super) {
      return `${$super.label()}:child`;
    },
  })
  .build();
const GeneratedChildDefaultImpl = mqp.java
  .defineClass("GeneratedChildDefaultImpl")
  .implements(GeneratedChildDefault)
  .build();
if (new GeneratedChildDefaultImpl().label() !== "parent:child") {
  throw new Error("generated parent default method call failed");
}

const Left = Java.type(
  "io.github.trethore.myqolpackages.internal.runtime.graal.GeneratedTypeFixture$Left",
);
const Right = Java.type(
  "io.github.trethore.myqolpackages.internal.runtime.graal.GeneratedTypeFixture$Right",
);
const GeneratedChoice = mqp.java
  .defineClass("GeneratedChoice")
  .implements([Left, Right])
  .method({
    name: "choose",
    returnType: Java.type("java.lang.String"),
    visibility: mqp.java.visibility.PUBLIC,
    override: true,
    implementation: function ($self, $super) {
      return `${$super.of(Left).choose()}:${$super.of(Right).choose()}`;
    },
  })
  .build();
if (new GeneratedChoice().choose() !== "left:right") {
  throw new Error("explicit parent interface default calls failed");
}

const AbstractBase = Java.type(
  "io.github.trethore.myqolpackages.internal.runtime.graal.GeneratedTypeFixture$AbstractBase",
);
const GeneratedConcrete = mqp.java
  .defineClass("GeneratedConcrete")
  .extends(AbstractBase)
  .method({
    name: "compute",
    returnType: mqp.java.type.int,
    argTypes: mqp.java.type.int,
    visibility: mqp.java.visibility.PUBLIC,
    override: true,
    implementation: function ($self, $super, value) {
      return value + 1;
    },
  })
  .build();
if (new GeneratedConcrete().compute(41) !== 42) {
  throw new Error("generated abstract method implementation failed");
}

const EarlyBase = Java.type(
  "io.github.trethore.myqolpackages.internal.runtime.graal.GeneratedTypeFixture$EarlyBase",
);
const GeneratedEarly = mqp.java
  .defineClass("GeneratedEarly")
  .extends(EarlyBase)
  .field({
    name: "state",
    type: Java.type("java.lang.String"),
    value: "initialized",
    visibility: mqp.java.visibility.PUBLIC,
  })
  .method({
    name: "describe",
    returnType: Java.type("java.lang.String"),
    visibility: mqp.java.visibility.PUBLIC,
    override: true,
    implementation: function ($self, $super) {
      return $self.state === null ? "early" : $self.state;
    },
  })
  .build();
const early = new GeneratedEarly();
if (early.observed !== "early" || early.describe() !== "initialized") {
  throw new Error("early constructor override dispatch failed");
}

const oneShotBuilder = mqp.java.defineClass("GeneratedOneShot");
oneShotBuilder.build();
let builderReuseRejected = false;
try {
  oneShotBuilder.build();
} catch (error) {
  builderReuseRejected = true;
}
if (!builderReuseRejected) throw new Error("builder accepted a second build");

let duplicateTypeRejected = false;
try {
  mqp.java.defineClass("GeneratedOneShot").build();
} catch (error) {
  duplicateTypeRejected = true;
}
if (!duplicateTypeRejected) throw new Error("duplicate generated type was accepted");

for (const invalidName of ["", "bad-name", "java.lang.GeneratedForbidden", " spaced.Name"]){
  let invalidNameRejected = false;
  try {
    mqp.java.defineClass(invalidName);
  } catch (error) {
    invalidNameRejected = true;
  }
  if (!invalidNameRejected) throw new Error(`invalid generated name was accepted: ${invalidName}`);
}

const recoverableBuilder = mqp.java.defineClass("GeneratedRecoverable").method({
  name: "missing",
  returnType: mqp.java.type.void,
  visibility: mqp.java.visibility.PUBLIC,
  isAbstract: true,
});
let concreteAbstractRejected = false;
try {
  recoverableBuilder.build();
} catch (error) {
  concreteAbstractRejected = true;
}
if (!concreteAbstractRejected) throw new Error("concrete class accepted an abstract method");
recoverableBuilder.abstract().build();

export function onEnable() {}
export function onDisable() {}
