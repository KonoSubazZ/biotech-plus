---
version: alpha
colors:
  primary: "#0E42D2"
  success: "#009A29"
  warning: "#D25F00"
  danger: "#CB2634"
typography:
  body:
    fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif"
rounded:
  DEFAULT: "6px"
spacing:
  panel: "16px"
---

## Overview
This is the existing Soybean / Naive UI administration application. Tenant management follows the user and notice workflows without changing the application's visual identity. The durable signature is a readable tenant identifier beside its name, so administrators can distinguish data ownership.

The user-facing business name is 药企管理 (Pharmaceutical Company Management); labels use 药企 while the underlying tenant isolation remains unchanged.

## Colors
Runtime ownership remains `src/theme/settings.ts`, `src/theme/vars.ts` and the theme store. Values above document the existing defaults; user theme preferences take precedence. Naive UI components provide light and dark states.

## Typography
Use existing application typography. Tenant identifiers remain selectable text and are not truncated without a tooltip.

## Layout
Use 16px panel gaps, a visible responsive search card, a toolbar and a server-paginated table. The table owns remaining-height scrolling; the shared shell keeps its existing scroll behavior. Drawers fit within 90% of the viewport.

## Elevation & Depth
Reuse `card-wrapper`, shared cards and Naive UI overlays. Do not add page-specific shadows or theme colors.

## Shapes
Use existing 6px theme radius and standard control sizes.

## Components
Reuse `TableHeaderOperation`, `useNaivePaginatedTable`, `useTableOperate`, `ButtonIcon`, Naive UI forms and `useNaiveForm`. Single-selects use `NSelect`; notifications use the application message provider. Scrollbars are owned by `src/styles/scss/scrollbar.scss` and global styles.

## Do's and Don'ts
Show business labels, preserve failed form input, confirm deletion, disable duplicate submissions, and use the active locale. Never derive permissions or tenant access from a hidden frontend control.
