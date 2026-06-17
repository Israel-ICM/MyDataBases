# Liquid Glass Bottom Bar — Tasks

## Phase 1: Implementation

### 1.1 Replace NavigationBar with custom Box layout
- [x] Replace `Column { content() + NavigationBar }` with `Box { content() + floating card }`
- [x] Use `Modifier.align(BottomCenter)` for card positioning
- [x] Remove NavigationBar and NavigationBarItem imports

### 1.2 Implement LiquidGlassBottomBar composable
- [x] Create private composable with Box + shadow(12dp) + clip(28dp)
- [x] Add translucent base background (#F5F5F7 @ 80%)
- [x] Add vertical gradient sheen (White 45% → 5%)
- [x] Add 1dp white edge highlight at top
- [x] Add Row with icon + label for each destination
- [x] Style selected state with primary color @ 12% background
- [x] Import all needed Compose APIs (Box, Row, Column, background, clip, shadow, etc.)

### 1.3 Fix white rectangle bug
- [x] Remove Surface(color = Transparent, shadowElevation) wrapper
- [x] Replace with Box + Modifier.shadow() directly

### 1.4 Add conditional visibility
- [x] Show menu only when not on connections, settings, or connection_form routes
- [x] Use inline check with `currentRoute` comparisons

### 1.5 Add slide-up animation
- [x] Wrap LiquidGlassBottomBar in AnimatedVisibility
- [x] Use slideInVertically(initialOffsetY = { fullHeight })
- [x] Use slideOutVertically(targetOffsetY = { fullHeight })

### 1.6 Adjust card position
- [x] Change bottom padding from 12dp to 32dp to raise card higher
- [x] Fix Modifier.padding signature (horizontal + vertical → start + end + top + bottom)

## Phase 2: Verification

### 2.1 Build verification
- [x] `./gradlew compileDebugKotlin` — passes
- [x] `./gradlew assembleDebug` — passes
- [x] No new warnings introduced
