# Implementation Plan - Dashboard On-Track Card Polish & Relocation

Refining the "On-Track" (Hero Budget) Card on the Dashboard to feel exceptionally polished, human-crafted, and intuitive, while relocating it directly below the Tip Card and introducing a visual iterative pie chart / category distribution graphic.

## User Review Required

> [!IMPORTANT]
> - **Card Relocation**: The On-Track (Hero Budget) Card will move from below the Mascot card to directly below the Tip Card (position #2, right after the tip card and before the mascot card).
> - **Layout Redesign**: The circular progress ring will move to the **left** side of the card, and the bottom category section will feature a polished visual iterative pie/donut/category distribution graphic instead of plain text/segmented bar.

## Proposed Changes

### [Dashboard Feature]

#### [MODIFY] [DashboardScreen.kt](file:///D:/THIRD_YEAR/AdaptiveFinance/app/src/main/java/com/finadapt/adaptivefinance/feature/dashboard/DashboardScreen.kt)
- Relocate the Hero Budget / On-Track Card layout block in the `Column` so it renders immediately below the Actionable Tip Card.
- Redesign the card internal layout:
  - Place the animated progress ring on the left with status badge and remaining budget.
  - Replace/enhance the category share bar at the bottom with a refined visual iterative pie/donut category chart representation with category color pills and smooth layout.
  - Maintain 100% of existing functionality (click to open budget sheet, category breakdown sheet, calculations, over-budget states, and animations).

## Verification Plan

### Automated Tests
- Build verification via `gradle_build(":app:assembleDebug")`.

### Manual Verification
- Deploy app to device and inspect Dashboard UI:
  - Verify On-Track Card is positioned immediately below the Tip Card.
  - Verify progress ring is on the left.
  - Verify iterative pie chart / visual category breakdown looks polished and human-crafted.
  - Verify all touch targets and bottom sheets remain fully functional.
