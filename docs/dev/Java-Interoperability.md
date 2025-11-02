One of the most powerful features of MQS is its deep integration with Java. This allows your JavaScript code to directly create, interact with, and even extend Minecraft's own Java classes. This page covers the three core functions that make this possible: `importClass`, `wrap`, and `extendMapped`.

### Understanding the Environment

*   **Yarn Mappings:** Minecraft's code is obfuscated. The developer community maintains **Yarn mappings** to give classes, methods, and fields meaningful names (e.g., `net.minecraft.client.MinecraftClient`). MQS is built on these mappings. **You should always use Yarn names** when interacting with Java classes, and MQS will handle the conversion to the actual runtime names for you.

> You can browse all available mappings using the **[Fabric Yarn Mappings Browser (1.21.4)](https://maven.fabricmc.net/docs/yarn-1.21.4+build.8/)**. This is an essential tool for finding the correct class, method, and field names for your scripts.


*   **Global Packages:** As mentioned in [[The Scripting Environment|The-Scripting-Environment]], top-level packages like `net.minecraft` are globally available. This means you can often access classes directly without `importClass`.

---

### `importClass('className')`

This is your gateway to accessing Java classes that are not part of the default global packages, or when you want a more explicit reference.

*   **Functionality:** It takes the fully-qualified Yarn name of a class and returns a JavaScript object that represents that Java class.
*   **What you can do with it:**
    *   Call `new` on it to create new instances (e.g., `new MyClass()`).
    *   Access its static methods and fields.

**Example:**
```javascript
// Import the Vec3d class
const Vec3d = importClass('net.minecraft.util.math.Vec3d');

// Create a new instance (call its constructor)
const myVector = new Vec3d(10, 20, 30);

// Access a static field
const zeroVector = Vec3d.ZERO;

println(`My vector's X is: ${myVector.getX()}`);
println(`The zero vector is: ${zeroVector}`);
```

---

### `wrap(javaObject)`

When you receive an object from a Java source (like an event callback or a method's return value), it's a "raw" Java object. The `wrap` function converts this raw object into a smart JavaScript proxy that understands Yarn mappings.

*   **Functionality:** Takes a raw Java object and returns a JavaScript proxy.
*   **Why use it?** The wrapped proxy allows you to call methods and access fields using their Yarn names, which is much more convenient and reliable than using obfuscated runtime names.

**Example:**
Let's say a hypothetical event gives you a raw `itemStack` object.

```javascript
// Assume `rawItemStack` is a raw Java object from an event.
const wrappedStack = wrap(rawItemStack);

// Now you can use Yarn names to interact with it.
// MQS automatically maps 'getName' to its runtime equivalent.
const itemName = wrappedStack.getName().getString();

// You can also access fields. The '$' suffix ensures you are
// accessing a field, not a method with the same name.
const count = wrappedStack.count$;

println(`Item: ${itemName}, Count: ${count}`);
```
> **Note:** MQS often wraps return values for you automatically, but `wrap()` is there for cases where you receive a raw object.

---

### `extendMapped(config, implementation)`

This is the most advanced interoperability tool. It allows you to create a new JavaScript class that **extends a Java class** and/or **implements Java interfaces**, letting you override their methods with your own JavaScript logic.

#### Arguments

*   **`config`** (object): A configuration object specifying what to extend and implement.
    *   `extends`: The Java class you want to extend. This must be a reference obtained from `importClass` or a global package.
    *   `implements`: (Optional) An array of Java interfaces your new class should implement.
*   **`implementation`** (object): An object containing your JavaScript methods and properties. Methods whose names match methods in the parent class or interfaces will become **overrides**. All other members become **addons**.

#### Special Keywords in an Instance

When you are inside an overridden method, you have access to special keywords via `this`:

*   **`this._super`**: An object that allows you to call the original, non-overridden Java methods of the parent class.
*   **`this._self`**: The **raw**, unwrapped Java instance. You'll need this when passing the instance back to a vanilla Minecraft function (e.g., `mc.setScreen(this._self)`).

#### Example: Creating a Custom Screen

This example creates a new class that extends Minecraft's `Screen` class and implements Java's `Runnable` interface.

```javascript
// @module(main=TestExtends, name=Test Extends Module, version=0.0.1)

const Text = net.minecraft.text.Text;
const Screen = net.minecraft.client.gui.screen.Screen;
const Runnable = importClass("java.lang.Runnable");

// A factory function to create our custom screen
function createCustomScreen(name) {
    // Step 1: Define the new class using extendMapped
    const CustomScreen = extendMapped({
        // Configuration: extend Screen and implement Runnable
        extends: Screen,
        implements: [Runnable]
    }, {
        // Implementation: methods and properties

        // This overrides the init() method from the Screen class
        init: function () {
            // Call the original Java method using this._super
            this._super.init();
            println("Custom screen initialized: " + name);
        },

        // This implements the run() method from the Runnable interface
        run: function () {
            println("Custom screen running: " + name);
        },

        // This is a new, JavaScript-only addon method
        open: function () {
            const mc = MQS.utils.mc.client();
            if (mc) {
                // _self is the raw Java object instance, required by setScreen
                mc.send(() => mc.setScreen(this._self));
            }
            println("Person: " + this.data.name);
            this.run();
        },

        // Another addon method
        test: function() {
            println("Custom screen test: " + name);
        },

        // An addon property
        data: {
            name: "Joe",
            age: "3"
        }
    });

    // Step 2: Instantiate the class, passing constructor arguments
    // for the parent Java class (Screen's constructor takes a Text object).
    const customScreen = new CustomScreen(Text.literal(name));

    return customScreen;
}

class TestExtends {
    onEnable() {
        println("Hello from Test Extends Module!");
        const customScreen = createCustomScreen("My Custom Screen");
        customScreen.open(); // Call our addon method
    }

    onDisable() {
        println("Goodbye from Test Extends Module!");
    }
}

exportModule(TestExtends);
```

---

This concludes the core developer documentation. The next section will provide concrete examples and recipes.

**➡️ Next Step: [[Examples & Recipes|Examples-and-Recipes]]**
