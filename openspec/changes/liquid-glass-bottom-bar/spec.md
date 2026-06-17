# Liquid Glass Bottom Bar — Spec

## Problem
The Compact layout used Material 3 `NavigationBar` with a solid background, which looked
generic and didn't match the premium Apple-style aesthetic the app is aiming for.

## Requirements

1. **Replace NavigationBar** — SHALL replace `NavigationBar` + `NavigationBarItem` with a
   custom floating bottom card in Compact mode (width < 600dp).
2. **Liquid Glass aesthetic** — SHALL use Apple-style frosted glass: rounded pill shape,
   translucent layers, subtle shadow, glass edge highlight.
3. **No real-time blur** — MUST use layered translucent colors + gradients instead of
   `RenderEffect` blur to avoid battery/GPU impact.
4. **Floating layout** — SHALL use `Box` with `Modifier.align(BottomCenter)` so the card
   floats over full-screen content.
5. **Conditional visibility** — SHALL hide the menu on outside-connection screens
   (`connections`, `connection_form`, `settings`) and show it on inside-connection screens
   (databases, tables, table viewer, and contextual connection routes).
6. **Slide-up animation** — SHALL animate in/out with `AnimatedVisibility` +
   `slideInVertically`/`slideOutVertically` when navigating between outside and inside screens.
7. **No white rectangle** — MUST NOT render a white background behind the glass layers.
   `Surface(color = Color.Transparent, shadowElevation)` draws a white layer; SHALL use
   `Box` + `Modifier.shadow()` instead.
8. **Responsive position** — SHALL respect bottom padding adjustments without hardcoded
   screen height calculations.

## Scenarios

### GIVEN user is on Connections screen
  WHEN the screen renders
  THEN the floating card SHALL NOT be visible

### GIVEN user navigates to Databases screen
  WHEN the screen renders
  THEN the floating card SHALL slide up from the bottom
  AND SHOW two items: Connections and Settings

### GIVEN user navigates to Tables screen
  WHEN the screen renders
  THEN the floating card SHALL be visible
  AND SHOW the same two items

### GIVEN user is on an inside-connection screen and taps "Settings"
  WHEN navigation completes
  THEN the floating card SHALL slide down and disappear

### GIVEN the app first loads
  WHEN the Connections screen is the start destination
  THEN the floating card SHALL NOT be visible
