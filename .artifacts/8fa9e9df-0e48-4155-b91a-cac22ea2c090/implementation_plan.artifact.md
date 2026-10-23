# Implementation Plan - Seamless Single-Screen Loading & Centered FinAdapt Splash

Consolidating app startup into a single, seamless, flawless loading screen featuring a dark background, a rotating multi-color loading ring, and "FinAdapt" / "Loading insights..." positioned directly in the middle of the ring.

## User Review Required

> [!IMPORTANT]
> - **Single Unified Screen**: Eliminates the jarring handoff between the native splash screen and the Compose UI, replacing it with a single, flawless Compose splash screen from app launch.
> - **Centered Ring & Branding**: Features the rotating multi-color loading ring with "FinAdapt" and loading status text centered directly in the middle of the ring.

## Proposed Changes

### [Splash Feature & MainActivity]

#### [MODIFY] [SplashScreen.kt](file:///D:/THIRD_YEAR/AdaptiveFinance/app/src/main/java/com/finadapt/adaptivefinance/feature/splash/SplashScreen.kt)
- Redesign `SplashScreen` to render a full-screen dark background (`#070B10`).
- Incorporate a smooth animated rotating multi-color ring (matching the app's signature accent colors: Emerald `#10B981`, Blue `#3B82F6`, Purple `#8B5CF6`, Amber `#F59E0B`).
- Position "FinAdapt" branding and "Loading insights..." text **directly in the center** of the rotating ring.

#### [MODIFY] [MainActivity.kt](file:///D:/THIRD_YEAR/AdaptiveFinance/app/src/main/java/com/finadapt/adaptivefinance/MainActivity.kt)
- Streamline app startup and remove redundant native splash hooks so that the Compose `SplashScreen` is the sole loading screen displayed while background tasks (database initialization, repository setup, data loading) run seamlessly.
- Transition immediately and smoothly to the main `NavGraph` once `isDataLoaded` becomes true.

## Verification Plan

### Automated Tests
- Build verification via `gradle_build(":app:assembleDebug")`.

### Manual Verification
- Cold start the app and verify:
  - Only one loading screen is shown with a dark background and rotating ring.
  - "FinAdapt" and loading text are centered perfectly inside the ring.
  - Background data loading occurs smoothly, transitioning seamlessly to the dashboard once ready.
