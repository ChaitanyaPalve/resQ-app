# Implement Intended Disaster Response Functionality

The goal is to populate the reserve buttons (B1-B10) in the new grid interface with core disaster response features and utilities, making the app fully functional for emergency scenarios.

## User Review Required

> [!IMPORTANT]
> The "Simulation Mode" (B9) will be enabled by default for safety. Real emergency dialing is available but simulated SOS packets are marked with `SIMULATION_TEST_MODE_ONLY`.

## Proposed Changes

### [Component] UI - Grid Interface

#### [MODIFY] [activity_main.xml](file:///C:/Users/palwe/AndroidStudioProjects/pheonixnet/app/src/main/res/layout/activity_main.xml)
Update button labels for B1-B10 to reflect their new functions.

| ID | New Label | Function |
|---|---|---|
| `btn_b1` | SOS | Broadcast High-Priority SOS |
| `btn_b2` | SAFE | Send "I am Safe" Status |
| `btn_b3` | NOTE | Drop a Virtual Note (Breadcrumb) |
| `btn_b4` | LITE | Toggle Flashlight |
| `btn_b5` | ALRM | Trigger Audio Siren |
| `btn_b6` | DIAG | Hardware Diagnostic Report |
| `btn_b7` | SCAN | Force Mesh Peer Scan |
| `btn_b8` | SYNC | Force DTN Packet Sync |
| `btn_b9` | SIM | Toggle Sandbox/Simulation Mode |
| `btn_b10`| HELP | App Instructions & Legend |

### [Component] App Logic - MainActivity

#### [MODIFY] [MainActivity.java](file:///C:/Users/palwe/AndroidStudioProjects/pheonixnet/app/src/main/java/com/phoenix/phoenixnet/MainActivity.java)
- Wire up the 10 reserve buttons.
- Implement `broadcastSos()`: Sends a high-priority SOS message via `MeshService`.
- Implement `markSafe()`: Sends a status update indicating the user is safe.
- Implement `dropBreadcrumb()`: Displays a dialog for the user to enter a geo-tagged message.
- Implement `toggleFlashlight()`: Controls the device's camera flash.
- Implement `toggleSiren()`: Generates a loud distress signal using `ToneGenerator`.
- Implement `forceSync()`: Manually triggers the store-and-forward sync mechanism.
- Implement `toggleSandbox()`: Switches between simulation and real modes.
- Implement `showHelp()`: Displays a summary of app features and grid button meanings.

### [Component] Configuration & Permissions

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/palwe/AndroidStudioProjects/pheonixnet/app/src/main/AndroidManifest.xml)
- Add `CAMERA` permission for flashlight support.
- Add `uses-feature android.hardware.camera.flash`.

## Verification Plan

### Automated Tests
- N/A (UI-driven logic)

### Manual Verification
1.  **SOS/SAFE**: Verify that clicking these buttons updates the local status and (simulated) packet broadcast occurs.
2.  **NOTE**: Verify that a dialog appears and a breadcrumb packet is created.
3.  **LITE**: Verify the device's flashlight toggles.
4.  **ALRM**: Verify a loud tone is played.
5.  **DIAG/HELP**: Verify the respective dialogs are shown.
6.  **SIM**: Verify that the simulation mode toggle updates the `isSandboxMode` flag and UI status.
