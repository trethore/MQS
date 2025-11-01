Before you can start using My QOL Scripts (MQS), you need to make sure you have the correct setup. This guide will walk you through the installation process step-by-step.

### Requirements

To use MQS, you will need the following:

1.  **Minecraft 1.21.4**.
2.  **[Fabric Loader](https://fabricmc.net/use/installer/)**: The mod loader that runs Fabric mods.
3.  **[Fabric API](https://modrinth.com/mod/fabric-api/versions?c=release&g=1.21.4)**: A core library required by almost all Fabric mods.
4.  **My QOL Scripts (MQS)**: The mod itself! You can download it from [the Releases page](https://github.com/trethore/MQS/releases).

---

### Installation Steps

#### Step 1: Install Fabric Loader

If you don't already have Fabric installed for Minecraft 1.21.4, follow these instructions. If you do, you can skip to Step 2.

1.  Go to the [Fabric Loader download page](https://fabricmc.net/use/installer/).
2.  Download the Universal JAR or the Windows installer.
3.  Run the installer.
4.  Make sure "Minecraft Version" is set to `1.21.4`.
5.  Click "Install". This will create a new "fabric-loader-1.21.4" profile in your Minecraft Launcher.

#### Step 2: Download the Mods

You need two files: **Fabric API** and **MQS**.

1.  Download the **Fabric API** for Minecraft `1.21.4`. You can find it on [Modrinth](https://modrinth.com/mod/fabric-api/versions?c=release&g=1.21.4).
2.  Download the latest version of **My QOL Scripts (MQS)** for Minecraft `1.21.4` from the location of your choice.

You should now have two `.jar` files.

#### Step 3: Add Mods to Your `mods` Folder

1.  Open your Minecraft game directory.
    *   **On Windows:** Press `WIN` + `R`, type `%appdata%\.minecraft`, and press Enter.
    *   **On macOS:** In Finder, click Go > Go to Folder, and type `~/Library/Application Support/minecraft`.
    *   **On Linux:** The folder is usually located at `~/.minecraft`.
2.  Find the `mods` folder inside your `.minecraft` directory. If it doesn't exist, simply create it.
3.  Move the two `.jar` files (Fabric API and MQS) you downloaded into this `mods` folder.

#### Step 4: Launch Minecraft

1.  Open the Minecraft Launcher.
2.  Select the **fabric-loader-1.21.4** profile from the installations list.
3.  Click "Play".

### How to Verify the Installation

You'll know the mod is working correctly if:

*   You can join a world (or server).
*   Typing `/mqs` into the chat opens the main MQS user interface.
*   A new folder named `my-qol-scripts` has been created inside your `.minecraft` directory.

---

You are now ready to start using MQS! Your next step is to learn how to add scripts.

**➡️ Next Step: [[Getting Started with Scripts|Getting-Started-with-Scripts]]**