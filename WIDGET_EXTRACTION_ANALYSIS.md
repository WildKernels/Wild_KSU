# Widget Extraction Analysis and Implementation Plan

## 1. Feasibility Analysis
Extracting widgets into a standalone APK with root access is **highly feasible**. The current widget implementation relies on standard Android `AppWidgetProvider` classes that launch specific `Intents`. The primary challenge is not the widgets themselves, but the **dependency on the Manager's UI and Logic**.

Since the widgets currently serve as shortcuts to specific screens (Module List, Superuser List, WebUI), the Standalone Widget APK must effectively become a **"Lite Manager"**. It must contain the logic to render these screens and communicate with `ksud` when the main Manager is removed.

## 2. Integration Examination

### Current Architecture
*   **Module Loading**: `ksud` (daemon) handles module operations. The Android app talks to `ksud` via `KsuCli` (shell wrapper).
*   **Widgets**: Located in `com.rifsxd.ksunext.ui.widget`. They broadcast `PendingIntents` targeting `MainActivity`.
*   **Action Buttons**: Module "Action Scripts" are triggered via `ksud module action`. This logic resides in `KsuCli.kt`.
*   **Manager Dependencies**: The widgets assume `MainActivity` exists. If the Manager is uninstalled, these Intents will fail unless the Standalone APK registers a matching Receiver/Activity.

### Root Access Mechanism
*   **Persistence**: KernelSU stores root permissions in `/data/adb/ksu/.allowlist` (userspace) or kernel structures, keyed by UID.
*   **Strategy**:
    1.  Install Standalone Widget APK (new package name/UID).
    2.  Grant Root to Widget APK using the main Manager.
    3.  Uninstall Manager.
    4.  Widget APK retains root access because its UID is still in the allowlist.

## 3. Separation Design

### Architecture Overview
We will create a new Android Application Module (`:widgets`) that functions independently.

1.  **Shared Logic (`:core` module)**:
    *   Extract `KsuCli.kt` and data models (Module, Superuser) into a shared library.
    *   Both Manager and Widget APK will depend on this.
2.  **Standalone Widget APK (`:widgets`)**:
    *   Contains the `AppWidgetProvider` classes.
    *   Contains a **Minimal UI** (Activities/Compose screens) to handle the widget clicks (e.g., displaying the Module List).
    *   Contains `WebUIActivity` to support KPatch/Zygisk/Dynamic shortcuts.
    *   **Manifest**: Declares the same `intent-filter`s or uses explicit internal routing.

### Communication
*   **With System**: Direct `su` calls to `ksud` (via shared `KsuCli`).
*   **With Manager**: None required after separation. The Widget APK is self-contained.

## 4. Implementation Steps

### Phase 1: Module Restructuring
1.  **Create `:core` Module**:
    *   Move `KsuCli.kt`, `Natives.java` (if needed), and common data classes here.
    *   Refactor `manager` app to depend on `:core`.
2.  **Create `:widgets` Module**:
    *   New Android App module. Package: `com.rifsxd.ksunext.widgets`.
    *   Depend on `:core`.

### Phase 2: Widget Migration
1.  **Move Code**: Copy/Move `BaseIconWidget` and all concrete widget classes (`ModulesWidget`, etc.) to `:widgets`.
2.  **Resource Migration**: Move `res/xml/widget_info_*.xml` and widget icons to `:widgets`.
3.  **Manifest Registration**: Register widgets in `:widgets/src/main/AndroidManifest.xml`.

### Phase 3: Logic Implementation
1.  **Port UI**: The Widget APK needs screens to show when a widget is clicked.
    *   *Option A (Full)*: Move `ModulesScreen`, `SuperuserScreen` to a shared `:ui` module.
    *   *Option B (Lite)*: Implement simplified versions in `:widgets`.
    *   *Recommendation*: Option A allows for a consistent experience.
2.  **Routing**: Implement a `RouterActivity` in `:widgets` that handles the `ACTION_MODULES`, `ACTION_SUPERUSER` intents broadcast by the widgets.

### Phase 4: Root & Packaging
1.  **Root Request**: Ensure `:widgets` calls `Shell.cmd("su")` on first launch or action to trigger the root request (which must be approved by the *still-installed* Manager).

## 5. Testing Requirements

### Verification Scenarios
1.  **Standalone Functionality**:
    *   Click "Modules Widget" -> Opens Module List (hosted by Widget APK).
    *   Click "Action Button" in Module List -> Executes script successfully.
2.  **Manager Removal**:
    *   Grant Root to Widget APK.
    *   Uninstall Manager.
    *   Reboot.
    *   Verify Widget APK still has root (check `rootAvailable()` returns true).
    *   Verify widgets still launch and perform actions.
3.  **Conflict Resolution**:
    *   Ensure both Manager and Widget APK can coexist (different package names).
    *   Verify Widgets point to the Widget APK (or prompt user).

### Performance
*   Verify Widget APK size (should be significantly smaller if resources are optimized).
*   Check memory usage of the standalone process.
