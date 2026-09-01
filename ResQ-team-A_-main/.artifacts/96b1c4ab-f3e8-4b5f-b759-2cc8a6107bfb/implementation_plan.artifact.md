# resQ Grid Layout Overhaul

This plan outlines the steps to transform the PheonixNet UI into the "resQ" 10x10 grid layout as specified in the design reference.

## User Review Required

> [!IMPORTANT]
> The entire layout will be replaced with a 10x10 grid. This is a significant UI overhaul. Some existing elements (like the message input and bottom sheet) will be integrated into the new grid or adjusted.

> [!WARNING]
> This change includes a "Flip" toggle and "Fire/Earthquake" modes which are new functional additions.

## Proposed Changes

### Resources

#### [MODIFY] [colors.xml](file:///C:/Users/palwe/AndroidStudioProjects/pheonixnet/app/src/main/res/values/colors.xml)
- Add "resQ" specific color tokens (Police Navy, Medical Green, Fire Orange, etc.).

#### [MODIFY] [activity_main.xml](file:///C:/Users/palwe/AndroidStudioProjects/pheonixnet/app/src/main/res/layout/activity_main.xml)
- Replace the `ConstraintLayout` with a grid-based structure (likely nested `LinearLayout`s with weights to ensure 10x10 cell consistency).
- Row 1: Status, Fire Toggle, Flip, Earthquake Toggle, Theme.
- Row 2: Bluetooth, WiFi P2P.
- Row 3: Emergency Services (Police, Medical, Fire).
- Rows 4-7: Map Space (OSMDroid integration).
- Row 8: App Status, Map Service.
- Row 9: Offline Call Controls (PTT, CALL, MUTE, END, SPK).
- Row 10: Reserve Buttons B1-B10.

### Logic

#### [MODIFY] [MainActivity.java](file:///C:/Users/palwe/AndroidStudioProjects/pheonixnet/app/src/main/java/com/phoenix/phoenixnet/MainActivity.java)
- Update view bindings for the new grid components.
- Implement "Flip" logic to toggle between Fire and Earthquake modes.
- Implement "Fire" and "Earthquake" switch behaviors.
- Update PTT, CALL, END button wiring.
- Add stubs/logic for MUTE and SPK toggles.
- Handle dark mode switching according to the design spec (cell 9-10).

## Verification Plan

### Automated Tests
- Build and run the app to ensure layout renders correctly on multiple screen sizes (simulated 390x844 px).

### Manual Verification
- Test all grid buttons for correct action triggers.
- Verify "Flip" toggle changes active states of Fire/Earthquake switches.
- Verify Dark/Light mode toggle updates all cell colors and map filter.
- Ensure OSMDroid Map occupies the designated 4x10 grid area correctly.
