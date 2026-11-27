# Vertical alignment

Some systems place the test window a few pixels higher or lower than it was when the test was recorded. This happens,
for example, when playback runs on another macOS version or display setup than the recording, as the menu bar and the
window decorations differ in height. Every pixel below the top of the test window then differs from the recording,
and the strict, fuzzy, cluster and edge checks fail even though nothing is wrong.

Vertical alignment finds that displacement and moves the playback image up or down to line up with the recording
before the pixel checks run. It is off by default.

## How it works

The test window can be covered by shades, its size changes between tests, and unrelated windows can appear around it.
For that reason the aligner does not look for the test window. It uses the recorded image itself:

1. It takes the horizontal edges of the recorded image: every pixel whose color differs from the pixel above it by more
   than 24 in any RGB component. Pixels in or around a shade are skipped, as a shade can hide any part of the window
   and casts a shadow.
2. For each candidate shift it counts how many of those edges are also found in the playback image, with the same
   colors above and below the edge, within 8 per RGB component. It tries `0`, then `+1` and `-1`, then `+2` and `-2`,
   and so on, up to `align.vertical.maxShift`.
3. It uses the shift that matches the most edges, but only if that is clearly better than not shifting: at least 16
   edges and at least 1% of all edges more than at shift `0`. Otherwise the image is not moved.
4. The whole playback image is then moved by that shift: every row is taken from the row that many rows away. Rows that
   would come from outside the image keep their content.

Edges that belong to something that is not part of the test, like a window that is only there during playback, match
under no shift, so they do not change the decision. Something that is at the same place in both images matches at shift
`0`, so it does not push towards a shift either.

## What uses the aligned image

| Where | Image used |
|---|---|
| Strict, fuzzy, cluster and edge checks | Aligned |
| Review: the `current` image and every diff and hint | Aligned, so hints land on the pixels they describe |
| Hash check | Original |
| Dimension and confidence checks | Original dimensions, aligned failures |
| Image saved to the failures folder, and the one saved as the new recording | Original |

The review reports the detected shift for each alternative as the `alignShift` property of the `recorded` check, in
pixels. A positive value means the test window was that many pixels lower than in the recording.

## Configuration

```properties
arctic.player.backend.sc.pixel.align.vertical.enabled = false
arctic.player.backend.sc.pixel.align.vertical.maxShift = 8
```

| Key | Default | Description |
|---|---|---|
| `arctic.player.backend.sc.pixel.align.vertical.enabled` | `false` | Turns the alignment on. |
| `arctic.player.backend.sc.pixel.align.vertical.maxShift` | `8` | Largest shift, up or down, in pixels. `0` never moves the image. Must not be negative. |

Both keys are optional. A `player.properties` written before this feature keeps working and does not align images.

## Notes and limits

- Only vertical displacement is handled. A horizontal shift is not detected.
- The whole image is moved as one. If two windows in the test are displaced by different amounts, only the displacement
  that matches the most edges is corrected. A window that the test places by itself, such as a dialog positioned
  relative to the screen, can end up displaced by a different amount. The `edge` check can absorb that with
  `edge.patch.maxShift`, see [`edge-pixel-check.md`](edge-pixel-check.md).
- Parts of the screen that do not move with the test window, such as the title bar of the workbench and the shades, end
  up displaced by the shift. They need to be excluded from the comparison, with `background.boundary.top` for the
  workbench title bar and with `shadeMargin` for the shades (shades are skipped when `checkShades` is `false`). Moving
  them back row by row was tried, but rows that cross both moving and stationary content ended up mixed from two
  shifts, which created differences of their own.
- Alignment compensates for the position of the window. It does not change colors, so differences like a window title
  bar tinted by the desktop wallpaper still fail. The `background` mask and the `boundary` strips described in
  [`background-mask.md`](background-mask.md) are independent and can be used together with this feature.
- The recording must contain enough edges. An image that is almost flat is not moved.
- The aligner needs the recorded and the playback image to have the same dimensions. The dimension check still fails
  when they do not.

## Code

| File | Purpose |
|---|---|
| `player/.../backend/pixel/VerticalAligner.java` | Finds the shift and creates the aligned image. |
| `player/.../backend/pixel/check/LoadRecordedPixelCheck.java` | Aligns the images when the recorded image is loaded, both during playback and when generating the review. |
| `player/.../backend/pixel/PixelCheckResult.java` | Holds the aligned image and the shift for each alternative. |
| `player/.../model/ArcticDiffImages.java` | Holds the aligned image for the review. |
