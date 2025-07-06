# My QOL Scripts - A Modern Scripting Mod for Minecraft

[![Requires: Fabric API](https://img.shields.io/badge/Requires-Fabric%20API-blueviolet.svg)](https://modrinth.com/mod/fabric-api)
[![Minecraft: 1.21.4](https://img.shields.io/badge/Minecraft-1.21.4-green.svg)](https://www.minecraft.net)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

**My QOL Scripts (MQS)** is a powerful, client-side scripting mod for Minecraft 1.21.4 that runs on the Fabric mod loader. It provides a high-performance JavaScript engine and a rich set of APIs, allowing you to create complex scripts to enhance your gameplay.

---

## Key Features

*   **Modern JavaScript Engine:** Write scripts using the latest features from **ECMAScript 2024**, powered by the high-performance GraalJS engine.
*   **Comprehensive APIs:** A rich, well-documented set of APIs gives you full control over the client:
    *   **`EventManager`**: React to game ticks, packet events, rendering, and more.
    *   **`CommandManager`**: Create custom client-side commands with arguments and tab-completion.
    *   **`KeybindManager`**: Register custom keybinds that can be re-configured in-game.
    *   **`ConfigManager`**: Easily save and load settings for your scripts.
    *   **`MQSUtils`**: A library of helpers for 2D/3D rendering, chat, and more.
*   **Deep Java Interoperability:** Directly `importClass`, `wrap`, and `extendMapped` to use, interact with, and even extend Minecraft's own Java classes, all with stable Yarn mappings.
*   **Advanced Method Hooking:** For advanced users, an in-memory `HookManager` allows you to intercept and modify Java method calls at runtime, giving you unparalleled control.
*   **User-Friendly Management:** Manage all your scripts through a clean in-game GUI and a powerful integrated console.

## Quick Start

1.  **Install [Fabric Loader](https://fabricmc.net/use/installer/)** for Minecraft 1.21.4.
2.  **Download** the latest versions of **[Fabric API](https://modrinth.com/mod/fabric-api)** and **[My QOL Scripts](https://github.com/trethore/MQS/releases)**.
3.  **Place** both `.jar` files into your `.minecraft/mods` folder.
4.  **Launch** the game!

For detailed instructions, see the **[[Installation Guide|wiki/user/Installation]]** in our wiki.

## Documentation

The official wiki is the central resource for all users and developers.

- **[[User Guide|wiki/user/User-Guide]]**
  <br>A complete guide for users on how to install the mod, find and manage scripts, and use all the in-game features.

- **[[Developer API Reference|wiki/dev/API-Reference]]**
  <br>The complete technical documentation for the MQS scripting APIs, with tutorials and examples to get you started.

## Performance Tuning

For the best experience, it is **highly recommended** that all users add a few JVM arguments to their Minecraft installation. This allows the GraalJS engine to use its high-speed JIT compiler, resulting in a massive performance boost for scripts.

**➡️ [[Learn how to tune performance here|Performance-Tuning]]**

## License

This project is licensed under the **GNU Affero General Public License v3.0 (AGPLv3)**.

The AGPLv3 is a strong copyleft license chosen specifically to ensure that this project and any derivative works remain free and open-source for the entire community. It guarantees that if someone uses and modifies the code, they must share their improvements back under the same terms.

## Contributing & Alternative Licensing

Contributions are welcome! If you want to help improve MQS, please feel free to fork the repository, make your changes, and submit a pull request.

#### Using the Code in Your Project

The public license for MQS is the AGPLv3. However, I understand that these terms might not be suitable for all open-source projects. As the copyright holder, I can grant an alternative, more permissive license (like the **MIT License**) on a case-by-case basis. This offer is generally available for **non-competing, open-source projects** that would benefit from using parts of this codebase.

**If you wish to use code from this project under a different license, please contact me on Discord (my username is `Tyt2`) to discuss it.**

## Support & Feedback

Found a bug? Have a great idea for a new feature? Please open an issue on our [**GitHub Issues page**](https://github.com/trethore/MQS/issues)!
