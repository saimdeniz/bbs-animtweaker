# Agent Rules & Coding Guidelines for BBS Anim Tweaker Addon

This file contains crucial instructions, architectural details, and lessons learned for any AI agent working on the `bbs_animtweaker_addon` project. Please read and follow these guidelines strictly to avoid compilation failures, application crashes, or broken animation behaviors.

---

## 1. Project Specifications & Environment

- **Target Game Version:** Minecraft 1.20.1 (Fabric)
- **Fabric Loader:** 0.19.2 (or compatible)
- **Java Version:** Java 17
- **Target API/Mod:** Blockbuster Mod (BBS) CML Edition (`bbs-cml-edition-2.0-dev-1-1.20.1`)
- **Main Addon Registration:** Implements BBS's custom Addon API and registers via standard `fabric.mod.json` client entrypoints.

---

## 2. Compilation & Build Guidelines

- Always build the addon from the project root directory: `C:\Users\saimd\Downloads\betterparticle\bbs_animtweaker_addon`
- Use the Gradle wrapper batch file for compiling:
  ```powershell
  # Faster compilation (recommended)
  .\gradlew.bat remapJar

  # Full build with tests
  .\gradlew.bat build
  ```
- Output JAR will be located at: `build/libs/bbs_animtweaker_addon-1.0.0.jar`
- **Never** attempt to copy build outputs to destinations outside the project workspace automatically unless explicitly requested by the user. Let the user manage file transfers manually.

---

## 3. Key Architecture & File Map

- **Main Addon Registry:**
  - `src/main/java/com/example/bbsanimtweaker/BBSAnimTweakerAddon.java` (Server/Main entrypoint)
  - `src/client/java/com/example/bbsanimtweaker/client/BBSAnimTweakerClientAddon.java` (Client entrypoint, registers GUI dashboards)
- **GUI Control Panel:**
  - [UIAnimTweakerPanel.java](file:///C:/Users/saimd/Downloads/betterparticle/bbs_animtweaker_addon/src/client/java/com/example/bbsanimtweaker/client/gui/UIAnimTweakerPanel.java)
    - Custom editor panel integrated into BBS's model configurator dashboard.
    - Handles independent X, Y, Z axis selection toggles, target bone input, math modifier inputs, and trigger action buttons.
- **Animation Injection Engine:**
  - [AnimTweakerEngine.java](file:///C:/Users/saimd/Downloads/betterparticle/bbs_animtweaker_addon/src/client/java/com/example/bbsanimtweaker/client/logic/AnimTweakerEngine.java)
    - Reads, parses, modifies, and saves target `.bbs.json` animation structures.
    - Uses GSON to manipulate rotation keyframe arrays for both List-based and Map-based keyframe layouts.

---

## 4. Coding Rules & Lessons Learned (Do NOT Repeat Past Mistakes)

### Immutability of Configs in BBS
- BBS's `ActionsConfig.getActions()` returns an **immutable list**. If you ever modify or add actions, you must copy it into a new mutable list (e.g., `new ArrayList<>(config.getActions())`) before modifying.
- When retrieving keys from a configuration object, use `config.getConfig("key")` (never `config.actions.get("key")` which will crash).

### Crash Safety & Null Checks
- BBS will crash with a NullPointerException (NPE) during model editor initialization if `ActionConfig` is null or if its `name` attribute is null or empty. Always check `if (actionConfig != null && !actionConfig.name.isEmpty())` before registering or modifying actions.

### Fabric Mixin Injection Rules
- When writing Fabric Mixins that use `@Inject` with a cancellable callback (e.g., `ci.setReturnValue(...)`), you **must** set `cancellable = true` on the `@Inject` annotation. Failing to do so causes compilation or runtime crashes.
- Never write variable declarations inside an if-else chain that can interrupt compile-time flow analysis.

### Animation Keyframe Query Rules
- **No Automatic Asymmetry Mirroring:** Never implement automatic sign inversion rules (left/right asymmetry mirroring) for queries based on bone names (e.g. `left_arm`, `l_wheel`). While useful for humanoid models, it breaks custom vehicle models (such as steering wheels). Always write the exact sign (`+` or `-`) selected by the user in the UI.
- **Query Mappings:**
  - **X Axis:** Maps to Pitch query (`query.head_pitch`)
  - **Y Axis:** Maps to Yaw query (`query.head_yaw`)
  - **Z Axis:** Maps to Yaw query (`query.head_yaw`) — specifically for steering wheels to allow wheel rotation matching the car's turn angle.
