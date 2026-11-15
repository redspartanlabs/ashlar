# Table pixel instability evidence (issue #34)

This folder is the evidence behind the production noise envelopes in
`TablePixelNoiseEnvelopes.PRODUCTION`. `TablePixelRetryTest` reads it on every
run and fails if the envelope table and this evidence disagree, or if the
evidence and the preserved pictures disagree.

An envelope changes only through a reviewed change backed by new measured
evidence added here. Never widen it to let a failing run pass.

## Contents

| File | What it is |
| --- | --- |
| `evidence.tsv` | One row per observed differing pixel (or one row per observation when no pixel is known), for the four cases with observed instability: `tb6`, `tb8`, `tb19` and `tb20`. |
| `images/` | The pictures the rows were read from: three full legacy/migrated picture pairs from TablePixelTest, and eleven enlargements from the repeat and replay studies. |
| `derive-evidence.py` | The script that produced `evidence.tsv` and `images/` from the downloaded CI artifacts. It uses only the Python standard library. |

## Sources

All observations come from GitHub Actions runs on Linux (`ubuntu-latest`,
headless Chrome). Each comes from the JUnit XML report in the named artifact:

| Run | Artifact | Report | Context |
| --- | --- | --- | --- |
| 43386499851 | `showcase-shard-4` | TablePixelTest with diagnostics | PR #32 at `bf75e12` |
| 43547695812 | `showcase-shard-4` | TablePixelTest with diagnostics; the repeat study | PR #33 |
| 43547695812 | `showcase-shard-1` | the replay study | PR #33 |
| 43569091894 | `showcase-shard-1` | the retry study | PR #35 |

These CI artifacts expire on GitHub around 2026-11-27/28. This folder keeps what
the envelopes rest on after that date.

To regenerate, download those artifacts into one folder per artifact named
`<run-id>-s<shard>` (each with its `junit/` folder), then run:

```
python -I derive-evidence.py <artifacts-root> <output-folder>
```

The script reproduces `evidence.tsv` and `images/` byte for byte from the same
artifacts.

## Columns of `evidence.tsv`

- `observation`: one observed difference between two pictures of the same case
  (`obs-001` and onward). One observation has as many rows as known differing
  pixels.
- `run`, `artifact`, `report`: where it was observed.
- `mode`: the parity run, `with-tailwind` or `tailwind-free`. Every row behind
  an envelope is `with-tailwind`. The one `tailwind-free` observation
  (`obs-009`) was tolerated by the policy, so that run has no envelope.
- `configuration`, `field`, `case`: the theme and color scheme, the field id, and
  the case key from `TableBrowser.CASES`.
- `kind`:
  - `rejected`: a TablePixelTest (or retry study) comparison of legacy and
    migrated that the policy rejected.
  - `tolerated`: a TablePixelTest comparison that the policy accepted. These rows
    are kept for context only. They are not part of any envelope.
  - `alternate in L`, `alternate in Mm`, `alternate in Mp`: the study captured
    the *same* page repeatedly and saw two different pictures. `L` is the legacy
    rendering. `Mm` and `Mp` are the migrated rendering with master's built
    stylesheet and with PR #32's built stylesheet (see `TablePixelStudy` in
    PR #33).
- `differing`, `max_channel`, `bounds`: the report's own measurement of the
  whole observation.
- `complete`: `yes` when every differing pixel of the observation is listed.
- `x`, `y`, `color_a`, `color_b`, `channel`: one differing pixel in picture
  coordinates (the field's box plus 6 px around it). It shows its two RGB colors
  and their largest channel difference. Unknown values are `-`.
- `pixel_source`: how the pixel is known:
  - `full-picture`: decoded from the full legacy and migrated pictures, which are
    preserved in `images/`.
  - `text+enlargement`: read from a preserved enlargement, and it matches the
    pixel list in the report text.
  - `text`: only from the report text, which lists every differing pixel when
    there are at most 32. No picture was printed.
  - `none`: the report gave counts only (`differing`, `max_channel` and
    `bounds`), so no pixel is known.
- `image`: the preserved file or files the row was read from, or `-`.

## Method

### Full pictures

For TablePixelTest observations that printed both pictures, the script decodes
both PNGs. Each pixel whose RGBA values differ becomes one row. Where the report
text also lists a pixel, the script requires the text and the picture to agree.

### Enlargements

The studies printed an enlargement instead of the full pictures. An enlargement
has three panels side by side, separated by 8 px gutters:

1. the first picture
2. the second picture
3. the difference, with every differing pixel painted magenta (`#FF00FF`)

Each panel covers the observation's bounding box plus a 6 px margin: from
`x0 = max(0, minX - 6)`, `y0 = max(0, minY - 6)` down to `y1 = maxY + 6`. Every
source pixel appears as a `scale` x `scale` block, where
`scale = height / (y1 - y0 + 1)` (integer division). The panel width is
`(width - 16) / 3`.

The script reads every magenta pixel of the third panel and maps it back to
source coordinates:

```
x = x0 + (px - 2 * (panel + 8)) / scale
y = y0 + py / scale
```

It reads the pixel's two colors at the center of its block in the first and
second panels. When the report text also lists the pixel, the script requires
the text and the enlargement to agree.

Observations that showed the same pair of pictures share one enlargement. The
rows record each observation's own direction (which picture showed which color).

## Verification

`TablePixelRetryTest` checks all of the following:

- **The envelopes against the evidence.** The production table has exactly one
  envelope for each configuration and field with a non-tolerated observation.
  Each envelope's coordinates and color pairs are exactly those observed. Each
  channel limit is the observed channel difference. Each maximum pixel count is
  the largest `differing` of any observation.
- **The count-only observations against the envelopes.** Every `none` row lies
  within its envelope's pixel count and largest channel limit.
- **The evidence against the full pictures.** For the three `full-picture`
  observations, decoding the preserved PNGs gives exactly the listed pixels and
  colors, and nothing else.
- **The evidence against the enlargements.** For the 35 enlargement
  observations, the magenta pixels of the preserved enlargement are exactly the
  listed coordinates. The colors at each one, in the first two panels, are
  exactly the listed pair.

## Limitations

- **Enlargement observations are reconstructed.** 35 observations come from
  enlargements, not from the original full-size pictures, which the studies
  never printed. Their coordinates and colors are read back through the mapping
  above. They agree with the report text wherever the text lists a pixel, and
  the test re-checks them against the preserved enlargements. But no full-size
  original exists to verify them against.
- **Some observations have no pixels.** Three observations (`obs-013`, `obs-014`
  and `obs-035`, from the retry study) have counts only. They are checked
  against the envelopes' counts and limits, not pixel by pixel.
- **Tolerated observations are text only.** Their rows come from the report text
  alone. They are not part of any envelope.
- **Windows observations are not here.** Local Windows runs also showed
  instability during the investigation, but their reports were not kept. They
  are not part of this evidence, and no envelope rests on them.
- **The scope is one environment.** The evidence covers the Linux runner images
  and Chrome versions of the runs above. A rendering change in a newer image or
  browser that moves, recolors or adds pixels lies outside the envelopes by
  design. It then fails at once, as an ordinary rejection.
