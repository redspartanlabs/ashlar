# Changelog

## Unreleased

### Changed

- Modal and Drawer are styled by Ashlar's own CSS (`components/overlay.css`, the
  internal primitives they share, plus `modal.css` and `drawer.css`) instead of
  Tailwind utility classes. Their template parameters and defaults, markup
  structure, ARIA and `data-*` hooks are unchanged, except as listed below. Each
  was compared against its previous Tailwind rendering in headless Chrome, in
  all 22 showcase configurations, with Tailwind present and removed: computed
  styles, motion, skin hooks, and pixels (exactly).
- `ModalSize.maxWidthClass()`, `DrawerStyles.alignment()`, `border()` and
  `closedTransform()` keep their names and signatures and now return Ashlar
  classes (`ash-modal__dialog--sm|md|lg`, `ash-drawer--left|right`,
  `ash-drawer__panel--left|right`).
- The open and closed state is no longer expressed with classes. A closed root
  carries the `hidden` attribute (it was the `hidden` class), and `data-state`
  (`closed` or `open`) on the root selects the visual state (the scripts used
  to toggle `opacity-*`, `scale-*` and `translate-*` classes). Code that read
  those classes to tell whether an overlay is open should read `hidden` or
  `data-state` instead.
- Modal and Drawer now coordinate through an internal module, `overlay.js`,
  served next to `modal.js` and `drawer.js` (which import it). When several
  overlays are open: the one opened last paints on top (an inline
  `--ash-overlay-stack` raises it, capped below the toast region); Escape and
  Tab go to the topmost one only, so one Escape closes one overlay and a
  topmost Modal that does not close on Escape is not bypassed; the page stays
  locked until the last overlay closes and its own inline `overflow` is then
  restored exactly; and restoring focus never moves it behind an overlay that
  is still open. An Escape that belongs to an open Popover, Tooltip, Dropdown
  Menu, Select, Combobox or Date Picker inside the topmost overlay closes only
  that popup.
- A Drawer now restores an inline `overflow` the page itself had set on
  `<body>` when it closes (it used to remove the property).
- An exit is over when every one of its transitions has finished, with a
  fallback read from the computed CSS instead of a fixed 200ms. A Modal closed
  before its first frame is hidden at once instead of staying mounted,
  invisible, for 250ms.

### Fixed

- Closing a Drawer under a Modal no longer unlocks the page while the Modal is
  open, and closing the Modal afterwards no longer leaves the page locked.
- Two stacked Modals closed by Escape no longer close together, and no longer
  can leave the page locked.
- A Drawer opened from a Modal no longer paints underneath it when it comes
  earlier in the document.

### Removed

- `data-drawer-closed-class` on the Drawer root. It carried the Tailwind class
  of the closed position for `drawer.js` to read back; the placement modifiers
  now carry it, and nothing reads the attribute.

## 0.2.0 - 2026-10-17

The first release with Ashlar's own CSS. Ashlar now ships a stylesheet, and five
components are styled by it instead of by Tailwind utility classes. The other 56
components are unchanged and still need Tailwind CSS v4.

### Added

- `ashlar.css`, an assembled stylesheet packaged in the jar at
  `META-INF/resources/ashlar/css/ashlar.css`.
- A cascade-layer order declared at the top of that file
  (`properties, theme, base, ash.tokens, ash.base, ash.layout, ash.components,
  ash.skin, ash.utilities, components, utilities`), which places Ashlar's layers
  between Tailwind's `base` and `components`. The stylesheet must be imported or
  linked before Tailwind's stylesheet for that order to hold.
- Design tokens (`--ash-color-*`, `--ash-radius-*`, `--ash-space-*` and focus
  tokens). They read the matching Tailwind theme variable when one is present and
  have a fixed fallback otherwise. Dark values apply under
  `:root[data-theme="dark"]`.
- A scoped base layer (`box-sizing` for `ash-` classes and an opt-in
  `.ash-focus-ring`) with no global reset.
- Layout primitives: `.ash-container`, `.ash-stack`, `.ash-grid`, `.ash-split`
  and `.ash-cluster`.
- Utilities: `.ash-sr-only` and `.ash-truncate`.
- Components styled by Ashlar's CSS: Badge, Alert, Divider, Progress and Empty
  State. Each has `ash-*` classes and, where it has colours or sizes to
  customise, `--ash-*` custom properties.
- Documentation of the stylesheet, its import order, the layer order and the
  `data-theme="dark"` contract in the README.

### Changed

- Badge, Alert, Divider, Progress and Empty State no longer emit the Tailwind
  utility classes they used before. Their template parameters, markup structure,
  accessibility attributes and `data-*` hooks are unchanged. Each component was
  compared against its previous Tailwind rendering in headless Chrome, with
  Tailwind present and with Tailwind removed. With Tailwind present the
  comparisons found no differences. With Tailwind removed, the Progress and
  Empty State comparisons ignore a border-style difference only where that side's
  computed border width is `0px` in both renderings (the style is then
  invisible). The Empty State comparison also ignores a `box-sizing` difference
  (`border-box` with Tailwind's preflight, `content-box` without it) on
  paragraphs directly in the component's body, only where all four paddings and
  all four border widths are `0px` in both renderings. Chrome only; no pixel
  diff or manual interaction testing.
- The Java style helpers of Badge, Alert, Divider and Progress now return Ashlar
  `ash-*` class strings instead of Tailwind utility strings. Their public method
  signatures are unchanged, as the migration diffs show.
- `AlertStyles.icon`, `title` and `body` keep their signatures and their
  `AlertVariant` parameter, but no longer vary by variant: each returns one
  fixed class (`ash-alert__icon`, `ash-alert__title`, `ash-alert__body`). The
  variant is now carried by the modifier class that `AlertStyles.container`
  returns (`ash-alert--info`, `--success`, `--warning`, `--error`), which sets
  the colours those elements read. Code that compared these return values per
  variant will see the same string for every variant.
- The README no longer says that Ashlar ships no CSS.

### Compatibility

- Consumer CSS or JavaScript that selects the removed utility classes on these
  five components stops matching. Select by `ash-*` class or `data-*` hook
  instead.
- Parity with Tailwind removed does not make the components Tailwind-independent.
  Empty State still renders its icon with the Icon component, which has not been
  migrated and still emits Tailwind classes.
- Tailwind CSS v4 is still required by the 56 components that have not been
  migrated, with the `@source` and `dark` variant setup in the README. Ashlar
  is not yet independent of Tailwind.
- Browser verification was done in Chrome only.
