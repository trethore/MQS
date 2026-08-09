// Class lookup and proxy identity

const Fixture = importClass("net.minecraft.test.FakeMappedClass");
const partialFixture = importClass("FakeMappedClass");
const packagedFixture = packages.net.minecraft.test.FakeMappedClass;
const aliasedFixture = net.minecraft.test.FakeMappedClass;
if (Fixture !== partialFixture
    || partialFixture !== packagedFixture
    || packagedFixture !== aliasedFixture) {
  throw new Error("class proxy identity differs");
}
const AlphaComponent = importClass("net.minecraft.alpha.Component");
const BetaComponent = importClass("net.minecraft.beta.Component");
if (AlphaComponent.alphaValue !== "initial-static"
    || BetaComponent.betaValue !== "initial-static") {
  throw new Error("class alias mappings interfered");
}

// Static methods and overload resolution

if (Fixture.greeting("MQP") !== "hello MQP") throw new Error("static method failed");
if (Fixture.choose(4) !== "number:4") throw new Error("numeric overload failed");
if (Fixture.choose("four") !== "string:four") throw new Error("string overload failed");
if (Fixture.specific("value") !== "string:value") {
  throw new Error("specific overload failed");
}
if (Fixture.specific(null) !== "string:null") {
  throw new Error("null overload specificity failed");
}
if (Fixture.numberOnly(4) !== "shared-number:4") {
  throw new Error("mapped numeric signature failed");
}
if (Fixture.stringOnly("four") !== "shared-string:four") {
  throw new Error("mapped string signature failed");
}
let mismatchedSignatureRejected = false;
try { Fixture.numberOnly("four"); } catch (error) { mismatchedSignatureRejected = true; }
if (!mismatchedSignatureRejected) throw new Error("mapped signature leaked overload");

// Static fields and name collisions

if (Fixture.staticValue !== "initial-static") throw new Error("static field failed");
if (Fixture.staticValue$ !== "initial-static") throw new Error("static $ field failed");
Fixture.staticValue$ = "changed-static";
if (Fixture.staticValue !== "changed-static") throw new Error("static write failed");
if (typeof Fixture.staticCollision !== "function") {
  throw new Error("static method did not win collision");
}
if (Fixture.staticCollision() !== "static-method") {
  throw new Error("static collision method failed");
}
if (Fixture.staticCollision$ !== "static-field") {
  throw new Error("static collision field failed");
}
let ambiguousWriteRejected = false;
try { Fixture.staticCollision = "changed"; } catch (error) { ambiguousWriteRejected = true; }
if (!ambiguousWriteRejected) throw new Error("ambiguous static write was accepted");

// Construction, instance members, and inheritance

const instance = new Fixture("fixture", 2);
if (instance.name !== "fixture" || instance.count !== 2) {
  throw new Error("private constructor or fields failed");
}
if (instance.increment(3) !== 5 || instance.count$ !== 5) {
  throw new Error("instance method failed");
}
instance.name$ = "renamed";
if (instance.name !== "renamed") throw new Error("instance write failed");
instance.name = "renamed-again";
if (instance.name$ !== "renamed-again") throw new Error("plain field write failed");
if (typeof instance.value !== "function") {
  throw new Error("instance method did not win collision");
}
if (instance.value() !== "instance-method" || instance.value$ !== "instance-field") {
  throw new Error("instance collision failed");
}
ambiguousWriteRejected = false;
try { instance.value = "changed"; } catch (error) { ambiguousWriteRejected = true; }
if (!ambiguousWriteRejected) throw new Error("ambiguous instance write was accepted");
instance.value$ = "changed-field";
if (instance.value$ !== "changed-field") throw new Error("collision write failed");
if (instance.join("joined", "a", "b") !== "joined:a,b") {
  throw new Error("varargs method failed");
}
if (instance.baseValue !== "base-field" || instance.baseMethod() !== "base-method") {
  throw new Error("inherited private members failed");
}

// Return values, wrapped arguments, and raw Java objects

const copy = instance.copy();
if (copy.name !== "renamed-again" || copy.count !== 5) {
  throw new Error("return wrapping failed");
}
if (!instance.same(instance) || instance.same(copy)) {
  throw new Error("wrapped object argument failed");
}
if (!instance._self || !instance._class || Fixture._class !== instance._class) {
  throw new Error("raw escape members failed");
}
if (!instance._equals(instance) || !instance._equals(instance._self)) {
  throw new Error("Java equality failed for the same object");
}
if (instance._equals(copy) || instance._equals(copy._self)) {
  throw new Error("Java equality failed for different objects");
}

// Java type checks

const BaseFixture = importClass("net.minecraft.test.FakeMappedBase");
if (!instance._instanceof(Fixture)
    || !instance._instanceof(Fixture._class)
    || !instance._instanceof(BaseFixture)) {
  throw new Error("Java instanceof failed");
}
let objectTypeRejected = false;
try { instance._instanceof(copy); } catch (error) { objectTypeRejected = true; }
if (!objectTypeRejected) throw new Error("instanceof accepted an object as a type");
let nullTypeRejected = false;
try { instance._instanceof(null); } catch (error) { nullTypeRejected = true; }
if (!nullTypeRejected) throw new Error("instanceof accepted null as a type");

// Explicit wrapping

const wrappedAgain = wrap(instance._self);
if (wrappedAgain.hiddenName !== "renamed-again") {
  throw new Error("wrapped field read failed");
}
if (!wrappedAgain.hiddenSame(instance)) {
  throw new Error("wrapped object identity failed");
}
if (!wrap(instance).same(instance)) {
  throw new Error("wrapper wrapping failed");
}

// Final fields must remain read-only

let finalWriteRejected = false;
try { instance.finalValue$ = "changed"; } catch (error) { finalWriteRejected = true; }
if (!finalWriteRejected || instance.finalValue !== "instance-final") {
  throw new Error("final instance field was writable");
}
finalWriteRejected = false;
try { Fixture.staticFinalValue$ = "changed"; } catch (error) { finalWriteRejected = true; }
if (!finalWriteRejected || Fixture.staticFinalValue !== "static-final") {
  throw new Error("final static field was writable");
}

export function onEnable() {}
export function onDisable() {}
