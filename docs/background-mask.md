# Background mask

The background mask makes the `pixel` image comparator ignore every part of a recorded screenshot that was empty
workbench. It is used when playback runs on a machine where something unrelated shows up behind the test window, such
as a stray application, a desktop widget or a notification. Without the mask, those pixels fail the `strict` check and
the whole comparison fails immediately.

It is disabled by default.

## How it works

Arctic already stores the color of the workbench with every screenshot check, and the recorded image shows exactly
where that color was visible. The mask is created from those two things on each comparison, so nothing extra is stored
with the recordings, and existing recordings work unchanged.

1. Every pixel of the **recorded** image whose RGB components are within `background.tolerance` of the workbench color
   is background.
2. If `background.shadow.darkness` is set, the drop shadows over the workbench are added. See
   [Drop shadows](#drop-shadows).
3. The background is grown by `background.margin` pixels in every direction.
4. Masked pixels are skipped by the `strict` check, in the same place where shades are skipped.

All checks that start from the strict result (`confidence`, `edge`, `fuzzy`, `cluster`) inherit the mask without any
change. The mask is built from the recorded image only, so what is on the screen during playback cannot influence it.

### Why there is a margin

Pixels at the border of the test window are the ones that are unreliable when the background is different:

- A window that is positioned one pixel differently moves its whole border.
- Rounded corners are drawn with different shapes, and are anti-aliased against whatever is behind them.
- Drop shadows and borders are blended with the background.

These pixels belong to the window, not to the background, so masking only the exact background color would still fail
on them. The margin covers them. A window shifted by up to `background.margin` pixels, or with corner rounding that
differs by up to `background.margin` pixels, passes over any background.

### Relation to the edge check

The two features cover different failures and work together:

| Situation | Handled by |
|---|---|
| Something unrelated shows behind the window | Background mask. |
| The window or its text is shifted by a pixel or two, over the same background | `edge` check. |
| The window is shifted by a pixel or two, over a different background | Mask margin, plus `edge` for what is left inside the window. |
| Corner rounding differs, over the same background | `edge` check. |
| Corner rounding differs, over a different background | Mask margin. |

Set `background.margin` to at least `edge.radius`. With the defaults (both 2) the two tolerate the same amount of
movement.

The mask does not need the `edge` check, and the `edge` check does not need the mask.

## Drop shadows

Windows cast a soft shadow over whatever is behind them. Over the workbench, a shadow is a semi transparent black, so
the pixel is the workbench color made darker. It is no longer within `background.tolerance` of the workbench color, so
the mask above treats it as part of the window. During playback, a different application behind the test window is
darkened by the same shadow, in a different color, and the comparison fails along the shadow of every window. The
`margin` cannot cover it, because a shadow reaches 20 pixels or more from the window.

`background.shadow.darkness` adds those pixels to the mask. A recorded pixel is shadow when:

- it is the workbench color multiplied by a factor between `1 - darkness` and `1`, with each RGB component within
  `background.shadow.tolerance` of the scaled color, and
- it is connected, through its eight neighbours, to a background pixel or to another shadow pixel.

The second condition is what keeps the window itself checked. A dark area of the window that happens to have a similar
color is not next to the background, so it is not masked. Pixels such as a neutral gray are not shadows either, because
they are not the workbench color scaled.

A `darkness` of `0.6` is a good starting point. It accepts shadows that darken the workbench by up to 60%. The
tolerance of `3` is enough for the rounding of the scaled colors.

## Boundary strips

The mask above only recognizes the workbench color. The workbench is a regular, decorated window, so the screenshot
also contains the frame of that window, and on macOS its rounded corners. Those corner pixels are not workbench
color: they show whatever desktop was behind the window when recording, so the mask does not cover them, and a margin
large enough to reach them would also hide most of the title bar.

The boundary properties mask a strip of fixed width along each border of the image, independently for the top, bottom,
left and right. They do not look at the colors of the recorded image.

Strips are rectangles, so they cover the whole side, not only the corner. Use the smallest value that works, and leave
a side at `0` when it is not needed. In a macOS capture measured at 800x800, the left and right edges were clean and
only the four corners, about 22 px deep, showed the desktop. Covering those corners with strips needs about 23 on the
top and bottom, which also hides 23 rows of the title bar. A narrower strip, such as 9, covers only the outer part of
the corners.

Boundary strips only apply while `background.enabled` is `true`. They also apply to recordings that have no stored
workbench color, where nothing else is masked.

## Configuration

```properties
arctic.player.backend.sc.pixel.background.enabled = true
arctic.player.backend.sc.pixel.background.tolerance = 0
arctic.player.backend.sc.pixel.background.margin = 2
arctic.player.backend.sc.pixel.background.shadow.darkness = 0
arctic.player.backend.sc.pixel.background.shadow.tolerance = 3
arctic.player.backend.sc.pixel.background.boundary.top = 0
arctic.player.backend.sc.pixel.background.boundary.bottom = 0
arctic.player.backend.sc.pixel.background.boundary.left = 0
arctic.player.backend.sc.pixel.background.boundary.right = 0
```

| Property | Default | Meaning |
|---|---|---|
| `arctic.player.backend.sc.pixel.background.enabled` | `false` | Turns the mask on. |
| `arctic.player.backend.sc.pixel.background.tolerance` | `0` | How much each RGB component of a recorded pixel can deviate from the workbench color to be background. Raise it only if the recorded background is not a flat color, for example because of color compression. |
| `arctic.player.backend.sc.pixel.background.margin` | `2` | Extra pixels around the background that are also ignored. |
| `arctic.player.backend.sc.pixel.background.shadow.darkness` | `0` | How much darker than the workbench color a drop shadow can be, as a fraction (0 to 1), to be ignored as well. `0` disables it. |
| `arctic.player.backend.sc.pixel.background.shadow.tolerance` | `3` | How much each RGB component of a pixel can deviate from the darkened workbench color to be shadow. |
| `arctic.player.backend.sc.pixel.background.boundary.top` | `0` | Rows ignored along the top border of the image. |
| `arctic.player.backend.sc.pixel.background.boundary.bottom` | `0` | Rows ignored along the bottom border of the image. |
| `arctic.player.backend.sc.pixel.background.boundary.left` | `0` | Columns ignored along the left border of the image. |
| `arctic.player.backend.sc.pixel.background.boundary.right` | `0` | Columns ignored along the right border of the image. |

All keys are optional. A `player.properties` written before this feature existed keeps working, with the mask
disabled. To get the documented keys, add them by hand, or run the player with `--writeConfig`, which overwrites
`player.properties` with the defaults and discards your edits.

The mask applies to the `strict` check, so the `strict` check must be in `arctic.player.backend.sc.pixel.checks` for
it to take effect, which is the case in the default configuration.

## What passes and what fails

| Difference between playback and recording | Result |
|---|---|
| Different content where the recording showed workbench | Pass |
| Window shifted by up to `margin` pixels, over a different background | Pass |
| Corner rounding differing by up to `margin` pixels, over a different background | Pass |
| Window shifted by more than `margin` pixels, over a different background | Fail |
| Different text or color inside the window | Fail |
| Another window covering part of the test window | Fail |
| Different pixels over the drop shadow of a window, with `shadow.darkness` set | Pass |
| Different pixels inside a `boundary` strip | Pass |
| Recording without a stored workbench color | Workbench mask not applied, `boundary` strips still are |

## Review GUI and diff output

When the mask is active, the `strict` check shows a `background` property with the number of ignored pixels. The strict
image, hints and masks do not mark ignored pixels as failures. The other properties are unchanged.

## Limitations

- **Content drawn in the workbench color is ignored.** If the application under test uses the same color as the
  workbench inside the window, those areas are not checked. The default workbench color is `0xE6F0FA`, which is rarely
  used by applications. If you see a conflict, change `arctic.common.arctic.gui.wb.default.color` and record again.
- **Windows that are not opaque are not supported.** If the test window shows the desktop through it, the recorded
  image contains the desktop, which is not masked, and the playback will differ if the desktop differs.
- **The window must be mostly surrounded by workbench.** A test window that fills the whole workbench has little to
  mask. The mask then changes little, which is correct.
- **A window that is moved further than the margin fails.** This is the same behavior as without the mask.
- **Shadows only count when they are connected to the background.** A shadow that is cut off from the background by
  other content, for example one that only shows between two windows, is not ignored.
- **A larger margin hides more.** Each extra pixel of margin ignores one more pixel of the window border. Keep it as
  small as the observed shifts allow.
- **The mask is computed on every comparison.** The cost is small and linear in the size of the image, and only paid
  when `background.enabled` is true.

## Implementation

| File | Purpose |
|---|---|
| `player/.../backend/pixel/BackgroundMask.java` | Creates the mask from the recorded image, the workbench color, the shadows and the boundary strips. |
| `player/.../backend/pixel/check/StrictPixelCheck.java` | Skips masked pixels, in `doCheck` and `doDiff`. |
| `player/.../backend/pixel/PixelCheckResult.java` | Carries the recorded workbench color to the checks. |
| `player/.../model/PixelCheckFailure.java`, `ArcticDiffImages.java` | Carry the workbench color to the review GUI, so the diff matches the playback result. |
| `player/.../inject/ArcticBackendPlayerScModule.java` | Binds the configuration, with defaults for missing keys. |
| `player/.../inject/InjectionKeys.java` | Configuration keys. |
| `player/src/main/resources/player.properties` | Default values. |

Tests are in `BackgroundMaskTest` (the mask itself), `BackgroundMaskPixelCheckTest` (strict and edge checks on
synthetic windows over a foreign background, including shifts and corner rounding) and
`ArcticBackendPlayerScModuleTest` (configuration).

Run them with:

```bash
./gradlew :player:test
```
