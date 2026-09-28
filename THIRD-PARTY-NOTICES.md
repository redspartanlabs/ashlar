# Third-Party Notices

This file records the provenance and licensing of third-party material
redistributed as part of this component library. It covers icon artwork only.

Ashlar itself is licensed under the Apache License 2.0; see `LICENSE`. This
file covers only third-party material redistributed inside the library.

## Feather Icons

92 of the 100 icons in `com.redspartanlabs.ashlar.icon.Icon` are reproduced
verbatim from Feather Icons.

- **Project:** Feather Icons — https://github.com/feathericons/feather
- **Version pinned:** 4.29.2
- **Source of truth:** `dist/icons.json` from the `feather-icons@4.29.2` npm release
- **License:** MIT

Artwork is used unmodified. This project applies its own *presentation*
contract in `ashlar/src/main/jte/ashlar/components/icon.jte` — `stroke-width="1.5"` rather
than Feather's default `2`, plus `fill="none"`, `stroke="currentColor"` and
round caps/joins on a `0 0 24 24` viewBox. Those are rendering attributes
applied by the component wrapper; the underlying path geometry is untouched.

Do not "tidy" the geometry (rounding decimals, adjusting radii, re-encoding
paths). Doing so silently breaks the ability to mechanically verify this set
against the upstream release.

### MIT License

```
The MIT License (MIT)

Copyright (c) 2013-2023 Cole Bemis

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Original project artwork

8 icons are original artwork created for this project, for concepts Feather
4.29.2 has no equivalent of. They are listed in the table below and carry no
third-party license obligation.

Basis for the "original" classification: each was drawn for this repository,
and none has an exact geometry match in any of the icon sets checked — Feather
4.29.2 (287 icons), Lucide 0.544.0 (1,636), Heroicons 2.2.0 24px-outline, or
Tabler 3.34.0 outline. Comparison normalised paths to absolute coordinates so
it is invariant to element splitting, subpath order, relative-vs-absolute
encoding and number formatting.

Two notes on scope of that claim: absence of an exact geometric match is not
proof of independent creation, and the four sets above are not the whole
universe of icon libraries. The classification reflects authorship in this
repository, corroborated by the comparison.

## Sets checked but not used

- **Lucide 0.544.0** (ISC, with MIT portions held by Cole Bemis) — no Lucide
  artwork is used. Lucide is a Feather fork, so a number of icons here are
  byte-identical to their Lucide counterparts; that is shared Feather ancestry,
  not use of Lucide. `GRID` previously carried an added corner radius that made
  it match Lucide's `layout-grid` exactly rather than Feather's `grid`; it has
  been restored to canonical Feather `grid`, so no ISC obligation arises.
- **Heroicons 2.2.0** (MIT) and **Tabler 3.34.0** (MIT) — checked for
  contamination; no matches found. Not used.

## Per-icon provenance

Every entry in the `Icon` enum, its source, and the upstream icon id it comes
from. Update this table whenever an icon is added or its artwork changes.

| Icon | Source | Version | Upstream id | Status |
| --- | --- | --- | --- | --- |
| `CALENDAR` | Feather Icons | 4.29.2 | `calendar` | Canonical (verbatim) |
| `CHEVRON_LEFT` | Feather Icons | 4.29.2 | `chevron-left` | Canonical (verbatim) |
| `CHEVRON_RIGHT` | Feather Icons | 4.29.2 | `chevron-right` | Canonical (verbatim) |
| `CHEVRON_DOWN` | Feather Icons | 4.29.2 | `chevron-down` | Canonical (verbatim) |
| `UPLOAD` | Feather Icons | 4.29.2 | `upload` | Canonical (verbatim) |
| `SEARCH` | Feather Icons | 4.29.2 | `search` | Canonical (verbatim) |
| `EDIT` | Feather Icons | 4.29.2 | `edit-3` | Canonical (verbatim) |
| `DELETE` | Feather Icons | 4.29.2 | `trash-2` | Canonical (verbatim) |
| `SAVE` | Feather Icons | 4.29.2 | `save` | Canonical (verbatim) |
| `PLUS` | Feather Icons | 4.29.2 | `plus` | Canonical (verbatim) |
| `CHECK` | Feather Icons | 4.29.2 | `check` | Canonical (verbatim) |
| `CLOSE` | Feather Icons | 4.29.2 | `x` | Canonical (verbatim) |
| `EYE` | Feather Icons | 4.29.2 | `eye` | Canonical (verbatim) |
| `FILTER` | Feather Icons | 4.29.2 | `filter` | Canonical (verbatim) |
| `USER` | Feather Icons | 4.29.2 | `user` | Canonical (verbatim) |
| `SETTINGS` | Feather Icons | 4.29.2 | `settings` | Canonical (verbatim) |
| `SHIELD` | Feather Icons | 4.29.2 | `shield` | Canonical (verbatim) |
| `SUN` | Feather Icons | 4.29.2 | `sun` | Canonical (verbatim) |
| `MOON` | Feather Icons | 4.29.2 | `moon` | Canonical (verbatim) |
| `CHECK_CIRCLE` | Feather Icons | 4.29.2 | `check-circle` | Canonical (verbatim) |
| `INFO_CIRCLE` | Feather Icons | 4.29.2 | `info` | Canonical (verbatim) |
| `WARNING_TRIANGLE` | Feather Icons | 4.29.2 | `alert-triangle` | Canonical (verbatim) |
| `X_CIRCLE` | Feather Icons | 4.29.2 | `x-circle` | Canonical (verbatim) |
| `BELL` | Feather Icons | 4.29.2 | `bell` | Canonical (verbatim) |
| `CHEVRON_UP` | Feather Icons | 4.29.2 | `chevron-up` | Canonical (verbatim) |
| `ARROW_LEFT` | Feather Icons | 4.29.2 | `arrow-left` | Canonical (verbatim) |
| `ARROW_RIGHT` | Feather Icons | 4.29.2 | `arrow-right` | Canonical (verbatim) |
| `ARROW_UP` | Feather Icons | 4.29.2 | `arrow-up` | Canonical (verbatim) |
| `ARROW_DOWN` | Feather Icons | 4.29.2 | `arrow-down` | Canonical (verbatim) |
| `MENU` | Feather Icons | 4.29.2 | `menu` | Canonical (verbatim) |
| `MORE_HORIZONTAL` | Feather Icons | 4.29.2 | `more-horizontal` | Canonical (verbatim) |
| `MORE_VERTICAL` | Feather Icons | 4.29.2 | `more-vertical` | Canonical (verbatim) |
| `EXTERNAL_LINK` | Feather Icons | 4.29.2 | `external-link` | Canonical (verbatim) |
| `HOME` | Feather Icons | 4.29.2 | `home` | Canonical (verbatim) |
| `DOWNLOAD` | Feather Icons | 4.29.2 | `download` | Canonical (verbatim) |
| `COPY` | Feather Icons | 4.29.2 | `copy` | Canonical (verbatim) |
| `DUPLICATE` | Original project artwork | — | — | Original (no match in Feather/Lucide/Heroicons/Tabler) |
| `REFRESH` | Feather Icons | 4.29.2 | `refresh-cw` | Canonical (verbatim) |
| `PRINT` | Feather Icons | 4.29.2 | `printer` | Canonical (verbatim) |
| `SHARE` | Feather Icons | 4.29.2 | `share-2` | Canonical (verbatim) |
| `SEND` | Feather Icons | 4.29.2 | `send` | Canonical (verbatim) |
| `ARCHIVE` | Feather Icons | 4.29.2 | `archive` | Canonical (verbatim) |
| `LOCK` | Feather Icons | 4.29.2 | `lock` | Canonical (verbatim) |
| `UNLOCK` | Feather Icons | 4.29.2 | `unlock` | Canonical (verbatim) |
| `PIN` | Original project artwork | — | — | Original (no match in Feather/Lucide/Heroicons/Tabler) |
| `LINK` | Feather Icons | 4.29.2 | `link` | Canonical (verbatim) |
| `FILE` | Feather Icons | 4.29.2 | `file` | Canonical (verbatim) |
| `FOLDER` | Feather Icons | 4.29.2 | `folder` | Canonical (verbatim) |
| `FOLDER_OPEN` | Original project artwork | — | — | Original (no match in Feather/Lucide/Heroicons/Tabler) |
| `FILE_TEXT` | Feather Icons | 4.29.2 | `file-text` | Canonical (verbatim) |
| `CLIPBOARD` | Feather Icons | 4.29.2 | `clipboard` | Canonical (verbatim) |
| `PAPERCLIP` | Feather Icons | 4.29.2 | `paperclip` | Canonical (verbatim) |
| `IMAGE` | Feather Icons | 4.29.2 | `image` | Canonical (verbatim) |
| `BOOK` | Feather Icons | 4.29.2 | `book` | Canonical (verbatim) |
| `MAIL` | Feather Icons | 4.29.2 | `mail` | Canonical (verbatim) |
| `PHONE` | Feather Icons | 4.29.2 | `phone` | Canonical (verbatim) |
| `MESSAGE` | Feather Icons | 4.29.2 | `message-circle` | Canonical (verbatim) |
| `INBOX` | Feather Icons | 4.29.2 | `inbox` | Canonical (verbatim) |
| `VIDEO` | Feather Icons | 4.29.2 | `video` | Canonical (verbatim) |
| `USERS` | Feather Icons | 4.29.2 | `users` | Canonical (verbatim) |
| `USER_PLUS` | Feather Icons | 4.29.2 | `user-plus` | Canonical (verbatim) |
| `USER_CHECK` | Feather Icons | 4.29.2 | `user-check` | Canonical (verbatim) |
| `USER_X` | Feather Icons | 4.29.2 | `user-x` | Canonical (verbatim) |
| `BUILDING` | Original project artwork | — | — | Original (no match in Feather/Lucide/Heroicons/Tabler) |
| `BRIEFCASE` | Feather Icons | 4.29.2 | `briefcase` | Canonical (verbatim) |
| `ID_BADGE` | Original project artwork | — | — | Original (no match in Feather/Lucide/Heroicons/Tabler) |
| `CHART_BAR` | Feather Icons | 4.29.2 | `bar-chart-2` | Canonical (verbatim) |
| `CHART_LINE` | Original project artwork | — | — | Original (no match in Feather/Lucide/Heroicons/Tabler) |
| `CHART_PIE` | Feather Icons | 4.29.2 | `pie-chart` | Canonical (verbatim) |
| `TRENDING_UP` | Feather Icons | 4.29.2 | `trending-up` | Canonical (verbatim) |
| `TRENDING_DOWN` | Feather Icons | 4.29.2 | `trending-down` | Canonical (verbatim) |
| `DATABASE` | Feather Icons | 4.29.2 | `database` | Canonical (verbatim) |
| `SERVER` | Feather Icons | 4.29.2 | `server` | Canonical (verbatim) |
| `ACTIVITY` | Feather Icons | 4.29.2 | `activity` | Canonical (verbatim) |
| `SHOPPING_CART` | Feather Icons | 4.29.2 | `shopping-cart` | Canonical (verbatim) |
| `CREDIT_CARD` | Feather Icons | 4.29.2 | `credit-card` | Canonical (verbatim) |
| `DOLLAR_SIGN` | Feather Icons | 4.29.2 | `dollar-sign` | Canonical (verbatim) |
| `TAG` | Feather Icons | 4.29.2 | `tag` | Canonical (verbatim) |
| `RECEIPT` | Original project artwork | — | — | Original (no match in Feather/Lucide/Heroicons/Tabler) |
| `WALLET` | Original project artwork | — | — | Original (no match in Feather/Lucide/Heroicons/Tabler) |
| `HELP_CIRCLE` | Feather Icons | 4.29.2 | `help-circle` | Canonical (verbatim) |
| `ALERT_CIRCLE` | Feather Icons | 4.29.2 | `alert-circle` | Canonical (verbatim) |
| `STAR` | Feather Icons | 4.29.2 | `star` | Canonical (verbatim) |
| `FLAG` | Feather Icons | 4.29.2 | `flag` | Canonical (verbatim) |
| `THUMBS_UP` | Feather Icons | 4.29.2 | `thumbs-up` | Canonical (verbatim) |
| `THUMBS_DOWN` | Feather Icons | 4.29.2 | `thumbs-down` | Canonical (verbatim) |
| `GRID` | Feather Icons | 4.29.2 | `grid` | Canonical (verbatim) |
| `LIST` | Feather Icons | 4.29.2 | `list` | Canonical (verbatim) |
| `LAYOUT` | Feather Icons | 4.29.2 | `layout` | Canonical (verbatim) |
| `COLUMNS` | Feather Icons | 4.29.2 | `columns` | Canonical (verbatim) |
| `MAXIMIZE` | Feather Icons | 4.29.2 | `maximize` | Canonical (verbatim) |
| `MINIMIZE` | Feather Icons | 4.29.2 | `minimize` | Canonical (verbatim) |
| `SLIDERS` | Feather Icons | 4.29.2 | `sliders` | Canonical (verbatim) |
| `CLOCK` | Feather Icons | 4.29.2 | `clock` | Canonical (verbatim) |
| `KEY` | Feather Icons | 4.29.2 | `key` | Canonical (verbatim) |
| `LOG_OUT` | Feather Icons | 4.29.2 | `log-out` | Canonical (verbatim) |
| `LOG_IN` | Feather Icons | 4.29.2 | `log-in` | Canonical (verbatim) |
| `GLOBE` | Feather Icons | 4.29.2 | `globe` | Canonical (verbatim) |
| `MAP_PIN` | Feather Icons | 4.29.2 | `map-pin` | Canonical (verbatim) |
| `CLOUD` | Feather Icons | 4.29.2 | `cloud` | Canonical (verbatim) |

