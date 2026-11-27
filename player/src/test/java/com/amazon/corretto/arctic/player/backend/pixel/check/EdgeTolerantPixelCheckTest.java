/*
 *   Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.amazon.corretto.arctic.player.backend.pixel.check;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeTolerantPixelCheckTest {
    private static final int SIZE = 40;
    private static final int STROKE_HEIGHT = 25;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    private static final int GRAY = 0xFF808080;
    private static final int DARK_GRAY = 0xFF606060;
    private static final int DARKER_GRAY = 0xFF404040;
    private static final int RED = 0xFFFF0000;
    private static final int GREEN = 0xFF00A000;
    private static final int TOLERANCE = 8;
    private static final int GRADIENT = 24;
    private static final int RADIUS = 2;
    private static final float MAX_EXCUSED = 0.10f;
    private static final float OVERSHOOT = 0.3f;
    private static final int PATCH_SHIFT = 4;
    // Half way and a quarter of the way from the mark (0x30) to the band (0xF0, 0xE8, 0xC0).
    private static final int BLEND_LIGHT = 0xFF908C78;
    private static final int BLEND_DARK = 0xFF605E54;
    private static final int PANEL_SIZE = 100;
    private static final int BAND_X = 10;
    private static final int BAND_Y = 10;
    private static final int BAND_WIDTH = 60;
    private static final int BAND_HEIGHT = 20;
    private static final int BAND_COLOR = 0xFFF0E8C0;
    private static final int BAND_TEXT = 0xFF303030;
    private static final int LIGHT_GRAY = 0xFFE0E0E0;
    private static final int SECOND_GROUP_Y = 70;
    private static final int STRAY_PIXEL = 95;

    private static EdgeTolerantPixelCheck check(final float maxExcused) {
        return check(maxExcused, 0f);
    }

    private static EdgeTolerantPixelCheck check(final float maxExcused, final float overshoot) {
        return check(maxExcused, overshoot, 0, 0);
    }

    private static EdgeTolerantPixelCheck check(final float maxExcused, final float overshoot, final int patchShift,
                                                final int maxUnexcused) {
        return new EdgeTolerantPixelCheck(TOLERANCE, GRADIENT, RADIUS, maxExcused, overshoot, patchShift,
                maxUnexcused, new HintMaskHelper(true, false));
    }

    private static EdgeTolerantPixelCheck patchCheck() {
        return check(MAX_EXCUSED, 0f, PATCH_SHIFT, 0);
    }

    // A panel with a colored band, like a title bar, that holds a few marks. The marks are drawn at the given offset,
    // and so is the band if movesBand is set.
    private static BufferedImage panel(final int offset, final boolean movesBand, final int markColor) {
        final BufferedImage img = new BufferedImage(PANEL_SIZE, PANEL_SIZE, BufferedImage.TYPE_INT_ARGB);
        fill(img, 0, 0, PANEL_SIZE, PANEL_SIZE, WHITE);
        fill(img, 0, 50, PANEL_SIZE, 10, LIGHT_GRAY);
        final int bandX = movesBand ? BAND_X + offset : BAND_X;
        fill(img, bandX, BAND_Y, BAND_WIDTH, BAND_HEIGHT, BAND_COLOR);
        for (int i = 0; i < 5; i++) {
            fill(img, BAND_X + offset + 5 + i * 10, BAND_Y + 5, 5, 10, markColor);
        }
        return img;
    }

    // Draws a one pixel ring around each mark of a panel whose marks are drawn at the given offset.
    private static void outline(final BufferedImage img, final int offset, final int color) {
        for (int i = 0; i < 5; i++) {
            final int x = BAND_X + offset + 5 + i * 10 - 1;
            final int y = BAND_Y + 5 - 1;
            fill(img, x, y, 7, 1, color);
            fill(img, x, y + 11, 7, 1, color);
            fill(img, x, y + 1, 1, 10, color);
            fill(img, x + 6, y + 1, 1, 10, color);
        }
    }

    private static BufferedImage image(final int color) {
        final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        fill(img, 0, 0, SIZE, SIZE, color);
        return img;
    }

    private static void fill(final BufferedImage img, final int x, final int y, final int w, final int h,
                             final int color) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                img.setRGB(i, j, color);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<Integer>[] strictFailures(final BufferedImage current, final BufferedImage saved) {
        final Set<Integer>[] failures = new Set[current.getWidth()];
        for (int x = 0; x < current.getWidth(); x++) {
            for (int y = 0; y < current.getHeight(); y++) {
                if (current.getRGB(x, y) != saved.getRGB(x, y)) {
                    if (failures[x] == null) {
                        failures[x] = new HashSet<>();
                    }
                    failures[x].add(y);
                }
            }
        }
        return failures;
    }

    private static boolean accepted(final EdgeTolerantPixelCheck check, final BufferedImage current,
                                    final BufferedImage saved) {
        return check.isAcceptable(current, saved, strictFailures(current, saved));
    }

    @Test
    void typeIsRegistered() {
        assertTrue(PixelCheck.Type.isType("edge"));
        assertEquals(PixelCheck.Type.EDGE, PixelCheck.Type.fromString("edge"));
        assertEquals(PixelCheck.Type.EDGE_HINT, PixelCheck.Type.EDGE.getHint());
    }

    @Test
    void identicalImagesPass() {
        assertTrue(accepted(check(MAX_EXCUSED), image(WHITE), image(WHITE)));
    }

    @Test
    void strokeShiftedByOnePixelPasses() {
        final BufferedImage saved = image(WHITE);
        fill(saved, 10, 5, 2, STROKE_HEIGHT, BLACK);
        final BufferedImage current = image(WHITE);
        fill(current, 11, 5, 2, STROKE_HEIGHT, BLACK);

        assertTrue(accepted(check(MAX_EXCUSED), current, saved));
    }

    @Test
    void strokeThicknessChangePasses() {
        final BufferedImage saved = image(WHITE);
        fill(saved, 10, 5, 2, STROKE_HEIGHT, BLACK);
        final BufferedImage current = image(WHITE);
        fill(current, 10, 5, 3, STROKE_HEIGHT, BLACK);

        assertTrue(accepted(check(MAX_EXCUSED), current, saved));
    }

    @Test
    void strokeShiftedBeyondRadiusFails() {
        final BufferedImage saved = image(WHITE);
        fill(saved, 5, 5, 2, STROKE_HEIGHT, BLACK);
        final BufferedImage current = image(WHITE);
        fill(current, 20, 5, 2, STROKE_HEIGHT, BLACK);

        assertFalse(accepted(check(MAX_EXCUSED), current, saved));
    }

    @Test
    void missingStrokeFails() {
        final BufferedImage saved = image(WHITE);
        fill(saved, 10, 5, 2, STROKE_HEIGHT, BLACK);

        assertFalse(accepted(check(MAX_EXCUSED), image(WHITE), saved));
    }

    @Test
    void antiAliasedEdgeColorPasses() {
        // The recording has a hard edge, the current image has one column that is a blend of both sides.
        final BufferedImage saved = image(WHITE);
        fill(saved, 0, 0, SIZE / 2, SIZE, BLACK);
        final BufferedImage current = image(WHITE);
        fill(current, 0, 0, SIZE / 2, SIZE, BLACK);
        fill(current, SIZE / 2, 0, 1, SIZE, GRAY);

        assertTrue(accepted(check(MAX_EXCUSED), current, saved));
    }

    @Test
    void edgeColorThatIsNotABlendOfTheNeighborhoodFails() {
        final BufferedImage saved = image(WHITE);
        fill(saved, 0, 0, SIZE / 2, SIZE, BLACK);
        final BufferedImage current = image(WHITE);
        fill(current, 0, 0, SIZE / 2, SIZE, BLACK);
        fill(current, SIZE / 2, 0, 1, SIZE, RED);

        assertFalse(accepted(check(MAX_EXCUSED), current, saved));
    }

    @Test
    void blendInAFlatAreaFails() {
        final BufferedImage saved = image(WHITE);
        fill(saved, 0, 0, SIZE, 4, BLACK);
        final BufferedImage current = image(WHITE);
        fill(current, 0, 0, SIZE, 4, BLACK);
        current.setRGB(SIZE / 2, SIZE / 2, GRAY);

        assertFalse(accepted(check(MAX_EXCUSED), current, saved));
    }

    // The recording has a dark gray area next to white. The current image has a column of the edge that is drawn
    // darker than the dark gray. On the line through dark gray and white that is 0.2 of the distance past dark gray.
    private static BufferedImage darkAreaWithEdgeColumn(final int edgeColor) {
        final BufferedImage img = image(WHITE);
        fill(img, 0, 0, SIZE / 2, SIZE, DARK_GRAY);
        fill(img, SIZE / 2, 0, 1, SIZE, edgeColor);
        return img;
    }

    private static BufferedImage darkAreaWithHardEdge() {
        final BufferedImage img = image(WHITE);
        fill(img, 0, 0, SIZE / 2, SIZE, DARK_GRAY);
        return img;
    }

    @Test
    void edgeDarkerThanBothNeighborsPassesWithOvershoot() {
        assertTrue(accepted(check(MAX_EXCUSED, OVERSHOOT), darkAreaWithEdgeColumn(DARKER_GRAY),
                darkAreaWithHardEdge()));
    }

    @Test
    void edgeDarkerThanBothNeighborsFailsWithoutOvershoot() {
        assertFalse(accepted(check(MAX_EXCUSED, 0f), darkAreaWithEdgeColumn(DARKER_GRAY), darkAreaWithHardEdge()));
    }

    @Test
    void edgeBeyondTheOvershootFails() {
        // Black is 0.6 of the distance past dark gray, more than the 0.3 allowed.
        assertFalse(accepted(check(MAX_EXCUSED, OVERSHOOT), darkAreaWithEdgeColumn(BLACK), darkAreaWithHardEdge()));
    }

    @Test
    void overshootDoesNotExcuseAFailureInAFlatArea() {
        final BufferedImage saved = image(DARK_GRAY);
        final BufferedImage current = image(DARK_GRAY);
        current.setRGB(SIZE / 2, SIZE / 2, DARKER_GRAY);

        assertFalse(accepted(check(MAX_EXCUSED, OVERSHOOT), current, saved));
    }

    @Test
    void extraStrokeFails() {
        final BufferedImage current = image(WHITE);
        fill(current, 10, 5, 2, STROKE_HEIGHT, BLACK);

        assertFalse(accepted(check(MAX_EXCUSED), current, image(WHITE)));
    }

    @Test
    void strokeColorChangeFails() {
        final BufferedImage saved = image(WHITE);
        fill(saved, 10, 5, 2, STROKE_HEIGHT, BLACK);
        final BufferedImage current = image(WHITE);
        fill(current, 10, 5, 2, STROKE_HEIGHT, RED);

        assertFalse(accepted(check(MAX_EXCUSED), current, saved));
    }

    @Test
    void flatAreaColorChangeFails() {
        final BufferedImage saved = image(WHITE);
        fill(saved, 5, 5, 30, 30, RED);
        final BufferedImage current = image(WHITE);
        fill(current, 5, 5, 30, 30, GREEN);

        assertFalse(accepted(check(MAX_EXCUSED), current, saved));
    }

    @Test
    void singleFailureInFlatAreaFails() {
        final BufferedImage saved = image(WHITE);
        final BufferedImage current = image(WHITE);
        current.setRGB(20, 20, BLACK);

        assertFalse(accepted(check(MAX_EXCUSED), current, saved));
    }

    @Test
    void failuresWithinFuzzyToleranceAreIgnored() {
        final BufferedImage saved = image(WHITE);
        final BufferedImage current = image(0xFFF8F8F8);

        assertTrue(accepted(check(0f), current, saved));
    }

    @Test
    void toleratedPixelsAboveCeilingFail() {
        final BufferedImage saved = image(WHITE);
        fill(saved, 10, 5, 2, STROKE_HEIGHT, BLACK);
        final BufferedImage current = image(WHITE);
        fill(current, 11, 5, 2, STROKE_HEIGHT, BLACK);

        assertFalse(accepted(check(0f), current, saved));
    }

    @Test
    void groupMovedFurtherThanTheRadiusPassesWithPatchShift() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, BAND_TEXT);

        assertFalse(accepted(check(MAX_EXCUSED), current, saved));
        assertTrue(accepted(patchCheck(), current, saved));
    }

    @Test
    void groupMovedInEitherDirectionPasses() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);

        assertTrue(accepted(patchCheck(), panel(-PATCH_SHIFT, false, BAND_TEXT), saved));
        assertTrue(accepted(patchCheck(), panel(PATCH_SHIFT - 1, false, BAND_TEXT), saved));
    }

    @Test
    void wholeBandMovedPasses() {
        final BufferedImage saved = panel(0, true, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, true, BAND_TEXT);

        assertTrue(accepted(patchCheck(), current, saved));
    }

    @Test
    void groupMovedBeyondPatchShiftAndRadiusFails() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        // The edge rules add the radius to the offset that was found, so that is how far a group can have moved.
        final BufferedImage current = panel(PATCH_SHIFT + RADIUS + 2, false, BAND_TEXT);

        assertFalse(accepted(patchCheck(), current, saved));
    }

    @Test
    void groupMovedByPatchShiftPlusRadiusPasses() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT + RADIUS, false, BAND_TEXT);

        assertTrue(accepted(patchCheck(), current, saved));
    }

    @Test
    void movedGroupWithDifferentAntialiasingPasses() {
        // Both images have an outline around each mark that is a blend of the mark and the band, but not the same one.
        // It is a large share of the box, so a fixed ratio of pixels that may differ would not allow for it.
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, BAND_TEXT);
        outline(saved, 0, BLEND_LIGHT);
        outline(current, PATCH_SHIFT, BLEND_DARK);

        assertTrue(accepted(patchCheck(), current, saved));
    }

    @Test
    void movedGroupWithAChangedOutlineColorFails() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, BAND_TEXT);
        outline(saved, 0, BLEND_LIGHT);
        outline(current, PATCH_SHIFT, RED);

        assertFalse(accepted(patchCheck(), current, saved));
    }

    @Test
    void movedGroupWithAFewPixelsChangedNeedsTheBudget() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, BAND_TEXT);
        final int markX = BAND_X + PATCH_SHIFT + 5;
        current.setRGB(markX + 1, BAND_Y + 7, RED);
        current.setRGB(markX + 2, BAND_Y + 7, RED);
        current.setRGB(markX + 3, BAND_Y + 7, RED);

        assertFalse(accepted(check(MAX_EXCUSED, 0f, PATCH_SHIFT, 0), current, saved));
        assertFalse(accepted(check(MAX_EXCUSED, 0f, PATCH_SHIFT, 2), current, saved));
        assertTrue(accepted(check(MAX_EXCUSED, 0f, PATCH_SHIFT, 3), current, saved));
    }

    @Test
    void groupThatIsMostlyUnexplainedByAnyOffsetFailsEvenWithBudget() {
        // Content that does not exist in the recorded image at any offset is not excused by a small budget either.
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, BAND_TEXT);
        fill(current, BAND_X + 5, BAND_Y + 16, 50, 3, RED);

        assertFalse(accepted(check(MAX_EXCUSED, 0f, PATCH_SHIFT, 10), current, saved));
    }

    @Test
    void groupWithChangedColorFailsWithPatchShift() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, RED);

        assertFalse(accepted(patchCheck(), current, saved));
    }

    @Test
    void groupWithMissingContentFailsWithPatchShift() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, BAND_TEXT);
        // One of the five marks is gone.
        fill(current, BAND_X + PATCH_SHIFT + 5 + 2 * 10, BAND_Y + 5, 5, 10, BAND_COLOR);

        assertFalse(accepted(patchCheck(), current, saved));
    }

    @Test
    void groupWithExtraContentFailsWithPatchShift() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, BAND_TEXT);
        fill(current, BAND_X + 5, BAND_Y + 16, 50, 3, BAND_TEXT);

        assertFalse(accepted(patchCheck(), current, saved));
    }

    @Test
    void patchShiftIsOffByDefault() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, BAND_TEXT);

        assertFalse(accepted(check(MAX_EXCUSED, 0f, 0, 0), current, saved));
    }

    @Test
    void patchDoesNotExcuseMoreThanMaxExcused() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, BAND_TEXT);

        assertFalse(accepted(check(0f, 0f, PATCH_SHIFT, 0), current, saved));
    }

    @Test
    void smallStrayGroupIsNotMatchedAsAPatch() {
        // A few pixels over a flat area fit at every offset, but they are too few to be a moved patch.
        final BufferedImage saved = image(WHITE);
        final BufferedImage current = image(WHITE);
        fill(current, 20, 20, 2, 2, BLACK);

        assertFalse(accepted(patchCheck(), current, saved));
    }

    @Test
    void strayContentOverAFlatAreaIsNotMatchedAsAPatch() {
        // Both images are the same flat color, except for an extra block in the current one. The block sits over a
        // flat area at every offset, so only the failing pixels themselves can tell that the content is new.
        final BufferedImage saved = image(WHITE);
        final BufferedImage current = image(WHITE);
        fill(current, 10, 10, 12, 12, BLACK);

        assertFalse(accepted(patchCheck(), current, saved));
    }

    @Test
    void separateGroupsAreMatchedIndependently() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, BAND_TEXT);
        // A second, far away group that moved by a different amount.
        fill(saved, 10, SECOND_GROUP_Y, 3, 20, BLACK);
        fill(current, 10 + 3, SECOND_GROUP_Y, 3, 20, BLACK);

        assertTrue(accepted(patchCheck(), current, saved));
        // And one that changed color is not hidden by the group that matched.
        fill(current, 10, SECOND_GROUP_Y, 3, 20, WHITE);
        fill(current, 10 + 3, SECOND_GROUP_Y, 3, 20, RED);
        assertFalse(accepted(patchCheck(), current, saved));
    }

    @Test
    void remainingFailuresUpToMaxUnexcusedPass() {
        final BufferedImage saved = image(WHITE);
        final BufferedImage current = image(WHITE);
        current.setRGB(20, 20, BLACK);
        current.setRGB(5, 30, BLACK);

        assertFalse(accepted(check(MAX_EXCUSED, 0f, 0, 1), current, saved));
        assertTrue(accepted(check(MAX_EXCUSED, 0f, 0, 2), current, saved));
    }

    @Test
    void maxUnexcusedDoesNotExcuseRealChanges() {
        final BufferedImage saved = image(WHITE);
        final BufferedImage current = image(WHITE);
        fill(current, 10, 10, 10, 10, BLACK);

        assertFalse(accepted(check(MAX_EXCUSED, 0f, 0, 2), current, saved));
    }

    @Test
    void maxUnexcusedAppliesAfterPatchesAreMatched() {
        final BufferedImage saved = panel(0, false, BAND_TEXT);
        final BufferedImage current = panel(PATCH_SHIFT, false, BAND_TEXT);
        current.setRGB(STRAY_PIXEL, STRAY_PIXEL, BLACK);

        assertFalse(accepted(check(MAX_EXCUSED, 0f, PATCH_SHIFT, 0), current, saved));
        assertTrue(accepted(check(MAX_EXCUSED, 0f, PATCH_SHIFT, 1), current, saved));
    }
}
