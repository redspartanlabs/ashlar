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
- Tooltip and Popover are styled by Ashlar's own CSS (`components/tooltip.css`
  and `components/popover.css`) instead of Tailwind utility classes. Their
  template parameters and defaults, markup structure, ARIA, `data-*` hooks,
  placement fallbacks, 150ms show delay and 150ms transitions, viewport nudge
  and clipping behavior are unchanged. Each was compared against its previous
  Tailwind rendering in headless Chrome, in all 22 showcase configurations,
  with Tailwind present and removed: computed styles, motion, skin hooks, and
  pixels (exactly). They remain independent of Modal and Drawer's `overlay.js`;
  an open Tooltip or Popover inside a Modal or Drawer still owns the first
  Escape.
- `TooltipStyles.position()`, `TooltipStyles.arrow()` and
  `PopoverStyles.position()` keep their names and signatures and now return
  Ashlar classes (`ash-tooltip__bubble--top|right|bottom|left`,
  `ash-tooltip__arrow--top|right|bottom|left`,
  `ash-popover__surface--top|right|bottom|left`).
- The open and closed state is no longer expressed with classes. A closed
  Tooltip bubble or Popover surface carries the `hidden` attribute (it was the
  `hidden` class), and `data-state` (`closed` or `open`) on the bubble or the
  surface itself selects the visual state (the scripts used to toggle
  `opacity-*` and `scale-*` classes). Code that read those classes to tell
  whether a Tooltip or Popover is open should read `hidden`, `data-state`, or
  the trigger's `aria-expanded` (Popover) and `aria-describedby` (Tooltip).
- Dropdown Menu is styled by Ashlar's own CSS (`components/dropdown-menu.css`
  and `components/dropdown-menu-item.css`) instead of Tailwind utility classes.
  Its template parameters and defaults, markup structure, generated ids, ARIA,
  `data-*` hooks, placement (no viewport correction, as before) and 150ms
  transitions are unchanged; the trigger's label and decorative icon now sit in
  `ash-*` wrapper classes. It was compared against its previous Tailwind
  rendering in headless Chrome, in all 22 showcase configurations, with
  Tailwind present and removed: computed styles, motion, skin hooks, and pixels
  (exactly). It remains independent of Modal and Drawer's `overlay.js`; an open
  Dropdown Menu inside a Modal, a Drawer or a Popover still owns the first
  Escape. The trigger now owns its drop shadow in
  `--ash-dropdown-menu-trigger-shadow`, so each showcase skin flattens it with
  one rule on `.ash-dropdown-menu__trigger` (it used to reach Tailwind's
  `shadow-sm` class), as for Button and Toggle.
- The Dropdown Menu's open and closed state is no longer expressed with classes.
  A closed menu carries the `hidden` attribute (it was the `hidden` class),
  `data-state` (`closed` or `open`) on the menu selects its visual state, and
  `data-state` on the chevron turns it (the script used to toggle `opacity-*`,
  `scale-*` and `rotate-180` classes). Code that read those classes to tell
  whether a menu is open should read `hidden`, `data-state`, or the trigger's
  `aria-expanded`.

### Fixed

- Closing a Drawer under a Modal no longer unlocks the page while the Modal is
  open, and closing the Modal afterwards no longer leaves the page locked.
- Two stacked Modals closed by Escape no longer close together, and no longer
  can leave the page locked.
- A Drawer opened from a Modal no longer paints underneath it when it comes
  earlier in the document.
- A Tooltip whose pointer or keyboard focus comes back while it is still fading
  out is shown again, at once, from wherever its fade had got to. It used to
  stay hidden until the pointer left and came back.
- A Popover reopened and closed again in quick succession is hidden only when
  its second fade has finished. The first close's fallback timer used to hide
  it part-way through that fade.
- A click, Enter, Space or an arrow key on a Dropdown Menu's trigger while the
  menu is still fading out reopens it, from wherever its fade had got to. It
  used to be dropped for 150ms after every close, and a stale close no longer
  hides a menu that was reopened.
- Tab or Shift+Tab from the trigger of an open Dropdown Menu closes the menu,
  as Tab from one of its items already did. It used to stay open after focus
  had left it, where Escape no longer reached it; inside a Modal or Drawer that
  left Escape closing neither the menu nor the overlay.
- A second Escape pressed while a Dropdown Menu inside a Modal or Drawer is
  still fading out after the first one closes the overlay. It used to be
  consumed by the closing menu.

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
