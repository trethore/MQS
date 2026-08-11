# Java Runtime Type Generation

MQP packages can generate real JVM classes and interfaces with `mqp.java.defineClass` and
`mqp.java.defineInterface`. `build()` returns an MQP class proxy, so generated types use the same constructor,
field, method, mapping, wrapping, and conversion behavior as imported classes.

## Shared API

Primitive types are available from `mqp.java.type`. Member visibility values are available from the frozen
`mqp.java.visibility` object:

- `PRIVATE`
- `PACKAGE`
- `PROTECTED`
- `PUBLIC`

Builders accept raw classes from `Java.type` and MQP class proxies from `importClass`, `packages`, or `net`.
Mapped overrides use their named method in JavaScript and the mapped runtime name in generated JVM bytecode.

A simple type name is placed in a package derived from the SHA-256 hash of the package ID. A fully qualified
binary name is used without modification. Generated types are public and are loaded into the configured MQP
runtime classloader.

## Classes

```js
const Counter = mqp.java
  .defineClass("Counter")
  .field({
    name: "value",
    type: mqp.java.type.int,
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
      return ($self.value += amount);
    },
  })
  .build();
```

Class builders provide `extends`, `implements`, `final`, `abstract`, `field`, `constructor`, `method`, and
`build`.

If no constructor is declared, MQP creates a public no-argument constructor and requires an accessible
no-argument constructor on the direct superclass. A declared constructor must begin with exactly one direct
top-level `$super(...)` call. The call cannot be conditional or asynchronous and its arguments cannot use
`$self`.

MQP selects and invokes the direct superclass constructor before exposing the new instance to JavaScript. It
then initializes generated instance fields and continues with the JavaScript constructor body. Expressions in
the leading `$super(...)` argument list must not have side effects because MQP evaluates that argument list
during superclass selection and again when running the constructor body.

A superclass constructor can call an overridable method before it returns. Normal Java virtual dispatch can
therefore enter a generated JavaScript override while the instance is only partially initialized. Generated
instance field initializers and the JavaScript constructor body have not run at that point.

Instance method callbacks receive `$self`, `$super`, and their declared arguments. `$self` is an MQP object
proxy. `$super` is an invocation-scoped proxy for nonvirtual superclass calls and hidden superclass field access.
Use `$super.of(ParentInterface)` to select a direct parent interface default method when Java requires an
explicit parent.

Static method callbacks receive only their declared arguments.

## Interfaces

```js
const Named = mqp.java
  .defineInterface("Named")
  .field({
    name: "DEFAULT_NAME",
    type: Java.type("java.lang.String"),
    value: "MQP",
  })
  .method({
    name: "name",
    returnType: Java.type("java.lang.String"),
    isAbstract: true,
  })
  .build();
```

Interface builders provide `extends`, `field`, `method`, and `build`. Fields are always public, static, and
final. Methods can be public abstract methods, public default methods, static methods, or private helper methods.
Protected, package-private, and final interface methods are rejected.

## Lifecycle and Reloads

Generated callbacks execute synchronously on the Java thread that invoked the generated member. MQP serializes
generated callback entry per package while permitting same-thread reentrant calls. `$super` is valid only for
the current callback and current thread.

Closing a package context invalidates its generated callback bindings. Calling a generated member afterward
throws an error instead of entering the closed GraalJS context.

The JVM cannot redefine an already loaded type structure. A package reload can reuse a generated type when its
owner, binary name, hierarchy, fields, constructors, methods, descriptors, and modifiers are unchanged. MQP then
replaces its callback bindings with the validated callbacks from the new context. A structural change requires a
game restart. Another package cannot claim an existing generated binary name.
