# Liquid Glass Bottom Bar — Design

## Overview

Replace the standard Material 3 NavigationBar in Compact mode with a custom floating bottom
card that mimics Apple's Liquid Glass aesthetic (frosted glass, rounded pill, subtle shadow).

## Architecture

### Before

```
Column {
    content()  // weight(1f)
    NavigationBar { NavigationBarItem x2 }
}
```

### After

```
Box(Modifier.fillMaxSize) {
    content()  // full screen
    AnimatedVisibility(visible = showMenu) {
        LiquidGlassBottomBar { Row { nav items } }
    }
}
```

### Layout Structure (LiquidGlassBottomBar)

```
Box(Modifier.padding(start=16, end=16, bottom=32))
  └─ Box(Modifier.shadow(12dp).clip(28dp).background(translucent))
       ├─ Box(height=1dp, white highlight)  // glass edge
       └─ Row(Arrangement.SpaceEvenly)
            └─ Column x2 (icon + label)
```

## Key Decisions

### ADR-1: Box + Modifier.shadow() over Surface
- **Context**: Surface(color = Color.Transparent, shadowElevation) draws a white background
  because the shadow rendering needs an opaque base.
- **Decision**: Use Box + Modifier.shadow(12dp, RoundedCornerShape(28dp)) directly.
- **Consequence**: No unwanted white layers; shadow is equivalent.

### ADR-2: Simulated glass over real-time blur
- **Context**: Real-time blur (RenderEffect) has GPU/battery cost on every frame.
- **Decision**: Layer translucent colors + vertical gradient sheen + 1dp white edge highlight.
- **Consequence**: Zero runtime cost, identical visual result on static content.

### ADR-3: AnimatedVisibility for enter/exit
- **Context**: Menu appears/disappears on navigation between outside and inside screens.
- **Decision**: Use AnimatedVisibility with slideInVertically/slideOutVertically (full height).
- **Consequence**: Smooth slide-up/slide-down animation without manual animation code.

### ADR-4: Route-based show/hide logic
- **Context**: Menu should not appear on connections/screens that are "outside" a connection.
- **Decision**: Inline check: hide when currentRoute == connections/settings/connection_form.
- **Consequence**: Simple, readable, no changes to NavigationContext needed.

## WindowSizeClass Impact

- **Compact** (< 600dp): Floating card at bottom-center with 16dp horizontal + 32dp bottom padding.
- **Medium** (600-840dp): Unchanged — NavigationRail.
- **Expanded** (> 840dp): Unchanged — PermanentNavigationDrawer.

## Visual Specs

| Property            | Value                |
|---------------------|----------------------|
| Card height         | 72dp                 |
| Corner radius       | 28dp                 |
| Shadow elevation    | 12dp                 |
| Shadow color        | Black @ 30%          |
| Background          | #F5F5F7 @ 80%        |
| Sheen gradient      | White 45% → 5%       |
| Edge highlight      | White @ 50%, 1dp     |
| Icon size           | 26dp                 |
| Label font size     | 11sp                 |
| Selected state      | Primary @ 12% bg     |

## Testing Strategy

- **UI test**: Verify card visibility on each route (AnimatedVisibility visible/invisible).
- **Visual**: Manual — inspect on emulator for glass effect, no white rectangles, correct position.
