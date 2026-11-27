# Edge tolerant pixel check

`EdgeTolerantPixelCheck` is a pixel check for the `pixel` image comparator. It accepts screenshots that differ only
because text or borders were drawn slightly differently, for example with another font, a different font size, or
different window corner rounding. It stays strict in flat areas, so a wrong color inside a colored shape still fails.

It is disabled by default.

## Why it exists

The existing checks handle small differences, but not moved edges:

| Check | Limitation for text and border changes |
|---|---|
| `strict` | Any different pixel fails. |
| `fuzzy` | Tolerates a small deviation per RGB component (`fuzzy.tolerance`, default 8). A black glyph pixel against a light background differs by about 200, so it still fails. |
| `cluster` | Tolerates isolated failing pixels. Glyph strokes are dense, so their failures form clusters and fail. |
| `confidence` | A gate on the ratio of strictly matching pixels. It cannot accept an image on its own. |

Raising `fuzzy.tolerance` far enough to cover a shifted glyph would also hide real color errors in flat regions. This
check applies a different rule depending on where the failing pixel is.

## How it works

The check examines only the pixels that failed the `strict` check. Each failing pixel is classified as follows:

1. **Within the fuzzy tolerance.** If every RGB component differs by at most `fuzzy.tolerance`, the pixel is ignored.
2. **Flat area.** If there is no edge within `edge.radius` pixels of the pixel in either image, the failure is not
   tolerated.
3. **Near an edge.** The failure is tolerated only if both conditions hold:
   - the color of the current pixel exists within `edge.radius` pixels of the same position in the recorded image,
     and
   - the color of the recorded pixel exists within `edge.radius` pixels of the same position in the current image.

   Colors are compared using `fuzzy.tolerance`. A color also counts as existing in the neighborhood when it is a blend
   of two colors found there: it lies, within `fuzzy.tolerance` per RGB component, on the straight line in RGB space
   between them. This is what anti-aliasing produces. A window corner that is rounded slightly differently, for
   example, has a hard-edged yellow border in one image and a border blended with the gray window in the other. The
   exact yellow does not exist nearby in the second image, but a mix of yellow and gray does.

   The line can be extended past either of the two colors by `edge.blend.overshoot`, as a fraction of the distance
   between them. An outline that is drawn crisper or darker than anything in the other image is on that line but
   beyond its end. With `0.6`, a color up to 60% of the distance past either end is accepted. With `0` only colors
   between the two are.

A pixel is an edge pixel when its color differs from its right or lower neighbor by more than `edge.gradient` in at
least one RGB component. The edge map is computed lazily, only for pixels near a failure.

4. **Moved region.** This step only runs if `edge.patch.maxShift` is above 0. It looks at the failures that steps 1 to 3
   did not tolerate. Failures that are within `edge.patch.maxShift` pixels of each other are grouped into a region.
   For each region of 8 pixels or more:
   - The offset `(dx, dy)` is searched, with each component at most `edge.patch.maxShift`, from the smallest
     outwards, at which the recorded image explains most of the region. It is chosen looking at up to 256 pixels
     spread over the region.
   - Each failing pixel of the region is then judged again, with the same rules as steps 1 to 3, against the recorded
     pixel that is the offset away. A pixel is explained only if its color is found in the recorded image around the
     offset position, and the recorded color is found in the current image around the opposite position.

   This is for a part of the screen that is drawn somewhere else than where it was recorded, and that does not move
   with the rest. It is the same situation the vertical alignment handles for the whole image, for a region that is
   not part of the test window, such as a dialog the test places by itself. As the rules of steps 1 to 3 still apply
   at the offset, a pixel can be off by `edge.radius` on top of the offset, so a region can be as far away as
   `edge.patch.maxShift + edge.radius`. Antialiasing, different outlines and shadows are handled by those rules and
   need no separate limit.

   A region is excused only if what the offset does not explain fits in what is left of `edge.maxUnexcused`. If it
   does not, the offset is not the reason for the difference, and none of the region is excused. Content that
   changed color, is missing or is extra has no offset that explains it, and it stays as failures. Pixels excused this
   way count towards `edge.maxExcused`, and a region that would take the tolerated pixels past it is not searched. The
   step is skipped when the failures left after steps 1 to 3 already fit in `edge.maxUnexcused`.

The check passes when both of these hold:

- no more than `edge.maxUnexcused` failing pixels are left, and
- the number of tolerated pixels is at most `edge.maxExcused` times the number of pixels in the image.

### What passes and what fails

| Difference between the images | Result |
|---|---|
| A stroke shifted by 1 px (within the radius) | Pass |
| A stroke that is thicker or thinner | Pass |
| A stroke shifted by more than the radius | Fail |
| A stroke or glyph that is missing in one image | Fail, because the other image has no matching color nearby |
| An edge pixel that is a blend of the two colors next to it | Pass |
| A stroke that changed color, such as black to red | Fail, as red is not on the line between black and white |
| A different color inside a flat shape | Fail |
| A single stray pixel in a flat area | Fail |
| A region of content moved further than `edge.radius`, as one block, by at most `edge.patch.maxShift` | Pass, if `edge.patch.maxShift` is set |
| The same region, with an outline that is antialiased differently | Pass |
| The same region, with one part missing, recolored or added to | Fail |
| A region moved further than `edge.patch.maxShift` plus `edge.radius` | Fail |
| Up to `edge.maxUnexcused` stray failing pixels | Pass |
| More tolerated pixels than `edge.maxExcused` allows | Fail |

Requiring a match in both directions is what separates a moved glyph from a missing or recolored one. It does not
separate two different glyphs of the same size in the same place. See the limitations.

## Position in the check chain

Checks run in order of priority, not in the order they appear in the `checks` property.

| Property | Value |
|---|---|
| Type | `EDGE` (`edge`) |
| Sufficient | Yes. A pass marks the image as passed. |
| Priority | 45, so it runs after `confidence` (40) and before `fuzzy` (50) and `cluster` (60). |
| Dependencies | `recorded`, `dimension`, `strict` |

Consequences of the chain rules:

- A failing `confidence` check marks the whole comparison as failed before `edge` runs. If screenshots differ too much
  overall, `edge` cannot rescue them. Lower `confidence.min` if text-heavy windows fall below it.
- A failing `edge` check does not fail the comparison. It leaves the status undecided, so `fuzzy` and `cluster` still
  get their turn.
- The result status is shared across all recorded alternatives. The first alternative that reaches a decisive PASSED
  or FAILED stops further checks for the others. A non-sufficient check, such as `confidence`, that fails on an earlier
  alternative therefore also decides the comparison for the later ones.

## Configuration

Add `edge` to the list of checks in `player.properties`:

```properties
arctic.player.backend.sc.pixel.checks = hash, recorded, dimension, strict, confidence, edge, fuzzy, cluster
```

`edge` also works without `fuzzy` in the list. It reads `fuzzy.tolerance` either way.

| Property | Default | Meaning |
|---|---|---|
| `arctic.player.backend.sc.pixel.edge.gradient` | `24` | Minimum difference in any RGB component between neighbouring pixels to count as an edge. |
| `arctic.player.backend.sc.pixel.edge.radius` | `2` | How many pixels around a failure to look for edges and matching colors. This is how far text or borders may move. |
| `arctic.player.backend.sc.pixel.edge.maxExcused` | `0.10` | Maximum ratio (0 to 1) of the image that may be tolerated. Above it, the check fails. |
| `arctic.player.backend.sc.pixel.edge.blend.overshoot` | `0` | How far past either of two blended colors a color may be, as a fraction of the distance between them. Optional. Must not be negative. |
| `arctic.player.backend.sc.pixel.edge.patch.maxShift` | `0` | How many pixels, in each direction, a region of failures can be moved to find its content in the recorded image. `0` disables the search. Optional. Must not be negative. |
| `arctic.player.backend.sc.pixel.edge.maxUnexcused` | `0` | How many failing pixels can be left over. Optional. Must not be negative. |
| `arctic.player.backend.sc.pixel.fuzzy.tolerance` | `8` | Shared with the `fuzzy` check. Defines when two colors are the same. |

`edge.gradient`, `edge.radius` and `edge.maxExcused` must be present in the configuration when `edge` is enabled.
`edge.blend.overshoot`, `edge.patch.maxShift` and `edge.maxUnexcused` are optional, so a `player.properties` written
before they existed keeps working and behaves as `0`, which is also the value in the default `player.properties`.

### Tuning

- **Text moves further than expected, and the check fails.** Increase `edge.radius`. A larger radius also makes it
  easier for unrelated differences to be excused.
- **A flat region next to text is being excused.** Lower `edge.radius`, or raise `edge.gradient` so that faint
  boundaries are not treated as edges.
- **Large windows with a lot of text fail on the ratio.** Raise `edge.maxExcused`.
- **A curve or outline drawn darker or crisper than the recording still fails.** Raise `edge.blend.overshoot`. Try
  values from `0.5` upwards. In one macOS comparison with `edge.radius` 1, `1.0` and `1.4` left the same number of
  pixels, and `0.5` and `0` left more. Lower it, or set it to `0`, if differences in shade are being excused that
  should not be.
- **A window the test places by itself, or a title bar, is drawn some pixels away, and the check fails.** First see
  whether `edge.radius` already covers the move, since the edge rules also excuse a rigid shift up to the radius. If
  the move is larger, set `edge.patch.maxShift` so that `edge.patch.maxShift + edge.radius` is at least the distance.
  Look at the vertical alignment too: it moves the whole image, so a region that does not move with the test window
  ends up further away than in the raw capture, and the distance has to include that. Keep the value small: a layout
  bug that moves a component by a few pixels is excused too, and the review shows the number of such pixels as
  `patched`.
- **A few isolated pixels are left.** These are typically single pixels at a window corner or along an outline that
  the platform draws differently. Set `edge.maxUnexcused` to a small number of pixels. The review shows the left over
  pixels as `failed`, so pick a value just above what a known-good run shows. It is an absolute number, so keep it far
  below the number of pixels in any real change, such as a missing glyph. It is also what a moved region may leave
  behind: a region whose offset leaves more than this unexplained is not excused at all.
- **A real regression passes.** Lower `edge.maxExcused`. This is the main safeguard against a large region being
  waved through.

## Review GUI and diff output

The check adds an `edge` image and `edge.hint` and `edge.mask` overlays to the failure review GUI. The `edge` image
shows only the failing pixels that were not tolerated. The hint and mask overlays outline the same pixels. These are
generated on demand, as for the other checks.

The review shows these properties for the `edge` check:

| Property | Meaning |
|---|---|
| `passed` | Whether the check passed for this alternative. |
| `failed` | Pixels that were not tolerated. |
| `maxUnexcused` | Not tolerated pixels allowed by `edge.maxUnexcused`. |
| `excused` | Pixels that were tolerated, including the patched ones. |
| `patched` | The part of `excused` that was tolerated because a region of failures matched at a moved position. |
| `maxExcused` | Tolerated pixels allowed by `edge.maxExcused`. |
| `ratio` | Share of image pixels that are not unexcused failures. 1 means there are none. |

## Limitations

- **The threshold values are starting points.** They were only tried on a few macOS comparisons of Swing dialogs, not
  on a wide range of screens. Tune them on a few known-same and known-different pairs from your environment.
- **It compares by position.** It accepts shifts up to `edge.radius` pixels, and with `edge.patch.maxShift` a region
  that moves as a whole by up to that many more. Text that rewraps or controls that move by different amounts fail,
  which is usually the desired behavior.
- **The region search does not recognize window decorations.** It has no idea what a title bar or a dialog is. It
  excuses any region that moved rigidly, so it works the same for any platform and operating system version.
- **Regions are made of failures that are close to each other.** Two things that moved by different amounts, but that
  touch, are one region and have one offset. The part that does not fit that offset has to fit in `edge.maxUnexcused`,
  or neither is excused.
- **Pixels drawn with a new color are not excused by moving.** If the platform draws, for example, a shadow line under
  a title bar that the recording does not have, those pixels have no match in the recording. Only `edge.maxUnexcused`
  can accept them, and a line across a whole window can exceed any reasonable value.
- **Edge boundaries of colored shapes are within the tolerated zone.** The interior of a shape is checked strictly. The
  band within `edge.radius` of its boundary is checked loosely. Keep the radius small if boundary accuracy matters.
- **A small element next to text can be hidden.** A thin colored line close to text may be excused if the same color
  exists nearby in the other image. The bidirectional match and `edge.maxExcused` reduce this but do not remove it.
- **Small text changes can pass.** The check compares colors, not shapes. Every dark pixel of one glyph has a dark pixel
  within `edge.radius` in a similar glyph of the same size and position, so a changed digit, such as `50` against `55`,
  can be excused. Do not rely on this check for text whose content matters. Record an alternative image for screens
  that show values that differ between platforms, so that `strict` matches them exactly. A smaller `edge.radius` or
  `edge.blend.overshoot` of 0 narrows this, but also narrows the shifts the check tolerates.
- **Blends widen what is excused.** With the blend rule, a pixel near an edge can also be excused when its color is
  between two colors nearby, or up to `edge.blend.overshoot` past them. A genuinely different color that happens to
  lie on that line, such as a mid-gray next to black and white, is excused too. The check is still limited to pixels
  within `edge.radius` of an edge, and by `edge.maxExcused`.
- **Environment still matters.** Resolution, UI theme and color profile affect any pixel comparison. This check does
  not compensate for a color profile difference. A different desktop background behind the test window can be handled
  with the [background mask](background-mask.md).

## Implementation

| File | Purpose |
|---|---|
| `player/.../backend/pixel/check/EdgeTolerantPixelCheck.java` | The check. |
| `player/.../backend/pixel/PixelCheck.java` | Adds the `EDGE`, `EDGE_HINT` and `EDGE_MASK` types. |
| `player/.../inject/ArcticBackendPlayerScModule.java` | Registers the check and binds its configuration. |
| `player/.../inject/InjectionKeys.java` | Configuration keys. |
| `player/src/main/resources/player.properties` | Default values. |

Tests are in `EdgeTolerantPixelCheckTest` (behavior on synthetic images) and `ArcticBackendPlayerScModuleTest`
(dependency injection with `edge` enabled).

Run them with:

```bash
./gradlew :player:test
```
