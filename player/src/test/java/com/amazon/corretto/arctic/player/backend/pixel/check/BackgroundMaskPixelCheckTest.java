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
import java.nio.file.Path;

import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.model.gui.ArcticFrame;
import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the strict and the edge check together on a recorded window that is played back over an unexpected background.
 */
class BackgroundMaskPixelCheckTest {
    private static final int SIZE = 60;
    private static final int WORKBENCH = 0xFFE6F0FA;
    private static final int WINDOW = 0xFFF4F4F4;
    private static final int BORDER = 0xFF404040;
    private static final int TEXT = 0xFF000000;
    private static final int RED = 0xFFFF0000;
    // The workbench color at 70% and at 85%.
    private static final int SHADOW_DARK = 0xFFA1A8AF;
    private static final int SHADOW_LIGHT = 0xFFC4CCD4;
    private static final int FOREIGN_A = 0xFF123456;
    private static final int FOREIGN_B = 0xFFA0502D;
    private static final int TOLERANCE = 8;
    private static final int GRADIENT = 24;
    private static final int RADIUS = 2;
    private static final float MAX_EXCUSED = 0.10f;
    private static final int MARGIN = 2;
    private static final Path ALTERNATIVE = Path.of("recorded.png");
    private static final int WINDOW_X = 10;
    private static final int WINDOW_Y = 10;
    private static final int WINDOW_SIZE = 40;
    private static final int COVER_START = 20;
    private static final int COVER_END = 40;
    private static final int STROKE_START = 8;
    private static final int STROKE_END = 28;

    private static StrictPixelCheck strict(final boolean ignoreBackground, final int margin) {
        return strict(ignoreBackground, margin, 0);
    }

    private static StrictPixelCheck strict(final boolean ignoreBackground, final int margin, final int boundary) {
        return strict(ignoreBackground, margin, boundary, 0f);
    }

    private static StrictPixelCheck strict(final boolean ignoreBackground, final int margin, final int boundary,
                                           final float shadowDarkness) {
        return new StrictPixelCheck(false, 0, ignoreBackground, 0, margin, shadowDarkness, 3, boundary, boundary,
                boundary, boundary, new HintMaskHelper(true, false));
    }

    private static EdgeTolerantPixelCheck edge() {
        return new EdgeTolerantPixelCheck(TOLERANCE, GRADIENT, RADIUS, MAX_EXCUSED, 0f, 0, 0,
                new HintMaskHelper(true, false));
    }

    // A window with a border, rounded corners and a text stroke, drawn over the given background.
    private static BufferedImage window(final int dx, final int dy, final int corner, final boolean foreign,
                                        final int strokeColor) {
        final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                img.setRGB(x, y, foreign ? ((x / 4 + y / 4) % 2 == 0 ? FOREIGN_A : FOREIGN_B) : WORKBENCH);
            }
        }
        final int left = WINDOW_X + dx;
        final int top = WINDOW_Y + dy;
        for (int x = left; x < left + WINDOW_SIZE; x++) {
            for (int y = top; y < top + WINDOW_SIZE; y++) {
                final boolean border = x == left || y == top || x == left + WINDOW_SIZE - 1
                        || y == top + WINDOW_SIZE - 1;
                img.setRGB(x, y, border ? BORDER : WINDOW);
            }
        }
        for (int i = 0; i < corner; i++) {
            for (int j = 0; j < corner - i; j++) {
                img.setRGB(left + i, top + j, foreign ? FOREIGN_A : WORKBENCH);
                img.setRGB(left + WINDOW_SIZE - 1 - i, top + j, foreign ? FOREIGN_A : WORKBENCH);
                img.setRGB(left + i, top + WINDOW_SIZE - 1 - j, foreign ? FOREIGN_A : WORKBENCH);
                img.setRGB(left + WINDOW_SIZE - 1 - i, top + WINDOW_SIZE - 1 - j, foreign ? FOREIGN_A : WORKBENCH);
            }
        }
        final int strokeX = left + 15;
        for (int x = strokeX; x < strokeX + 2; x++) {
            for (int y = top + STROKE_START; y < top + STROKE_END; y++) {
                img.setRGB(x, y, strokeColor);
            }
        }
        return img;
    }

    private static boolean passes(final StrictPixelCheck strict, final BufferedImage current,
                                  final BufferedImage saved) {
        final ScreenshotCheck currentCheck = new ScreenshotCheck();
        currentCheck.setImage(current);
        final ScreenshotCheck savedCheck = new ScreenshotCheck();
        savedCheck.setFilename(ALTERNATIVE);
        savedCheck.setSa(new ScreenArea(0, 0, SIZE, SIZE));
        savedCheck.setWorkbench(new ArcticFrame("wb", WORKBENCH, new ScreenArea(0, 0, SIZE, SIZE)));
        final PixelCheckResult result = new PixelCheckResult(currentCheck, savedCheck, new TestId("a", "b"), "default");
        result.putSavedImage(ALTERNATIVE, saved);

        return strict.doCheck(result, ALTERNATIVE) || edge().doCheck(result, ALTERNATIVE);
    }

    // Paints the outermost rows of the image, which are not workbench color, like the frame of a window.
    private static BufferedImage withTopStrip(final int rows, final int color) {
        final BufferedImage img = window(0, 0, 3, false, TEXT);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < SIZE; x++) {
                img.setRGB(x, y, color);
            }
        }
        return img;
    }

    // The recorded window has a band of shadow over the workbench next to it. During playback another application
    // shows through that band, which is darkened by the same shadow.
    private static BufferedImage withShadowBand(final boolean foreign) {
        final BufferedImage img = window(0, 0, 3, false, TEXT);
        for (int x = 0; x < SIZE; x++) {
            img.setRGB(x, 1, foreign ? FOREIGN_A : SHADOW_DARK);
            img.setRGB(x, 2, foreign ? FOREIGN_B : SHADOW_LIGHT);
        }
        return img;
    }

    @Test
    void shadowOverTheBackgroundIsIgnoredWhenEnabled() {
        final BufferedImage saved = withShadowBand(false);
        final BufferedImage current = withShadowBand(true);

        assertFalse(passes(strict(true, 0, 0, 0f), current, saved));
        assertTrue(passes(strict(true, 0, 0, 0.5f), current, saved));
    }

    @Test
    void shadowIsNotIgnoredWithoutTheBackgroundMask() {
        assertFalse(passes(strict(false, 0, 0, 0.5f), withShadowBand(true), withShadowBand(false)));
    }

    @Test
    void changeInsideTheWindowStillFailsWithShadowEnabled() {
        final BufferedImage saved = withShadowBand(false);
        final BufferedImage current = withShadowBand(true);
        for (int x = WINDOW_X + 5; x < WINDOW_X + 30; x++) {
            current.setRGB(x, WINDOW_Y + 30, RED);
        }

        assertFalse(passes(strict(true, 0, 0, 0.5f), current, saved));
    }

    @Test
    void boundaryIgnoresNonWorkbenchPixelsAlongTheBorder() {
        final BufferedImage saved = withTopStrip(4, FOREIGN_A);
        final BufferedImage current = withTopStrip(4, FOREIGN_B);

        assertFalse(passes(strict(true, 0, 0), current, saved));
        assertFalse(passes(strict(true, 0, 3), current, saved));
        assertTrue(passes(strict(true, 0, 4), current, saved));
    }

    @Test
    void boundaryDoesNotHideDifferencesInsideTheWindow() {
        final BufferedImage saved = window(0, 0, 3, false, TEXT);

        assertFalse(passes(strict(true, 0, 4), window(0, 0, 3, false, RED), saved));
    }

    @Test
    void foreignBackgroundFailsWithoutTheMask() {
        final BufferedImage saved = window(0, 0, 3, false, TEXT);
        final BufferedImage current = window(0, 0, 3, true, TEXT);

        assertFalse(passes(strict(false, MARGIN), current, saved));
    }

    @Test
    void foreignBackgroundPassesWithTheMask() {
        final BufferedImage saved = window(0, 0, 3, false, TEXT);
        final BufferedImage current = window(0, 0, 3, true, TEXT);

        assertTrue(passes(strict(true, MARGIN), current, saved));
    }

    @Test
    void shiftBeyondTheMarginFailsOverForeignBackground() {
        final BufferedImage saved = window(0, 0, 3, false, TEXT);

        assertFalse(passes(strict(true, 0), window(1, 0, 3, true, TEXT), saved));
        assertFalse(passes(strict(true, MARGIN), window(MARGIN + 1, 0, 3, true, TEXT), saved));
    }

    @Test
    void foreignBackgroundWithWindowShiftedOnePixelPasses() {
        final BufferedImage saved = window(0, 0, 3, false, TEXT);

        assertTrue(passes(strict(true, MARGIN), window(1, 0, 3, true, TEXT), saved));
        assertTrue(passes(strict(true, MARGIN), window(-1, 0, 3, true, TEXT), saved));
        assertTrue(passes(strict(true, MARGIN), window(0, 1, 3, true, TEXT), saved));
        assertTrue(passes(strict(true, MARGIN), window(0, -1, 3, true, TEXT), saved));
        assertTrue(passes(strict(true, MARGIN), window(1, 1, 3, true, TEXT), saved));
    }

    @Test
    void foreignBackgroundWithDifferentCornerRoundingPasses() {
        final BufferedImage saved = window(0, 0, 3, false, TEXT);

        assertTrue(passes(strict(true, MARGIN), window(0, 0, 2, true, TEXT), saved));
        assertTrue(passes(strict(true, MARGIN), window(0, 0, 4, true, TEXT), saved));
        assertTrue(passes(strict(true, MARGIN), window(1, 1, 4, true, TEXT), saved));
    }

    @Test
    void windowShiftedOnePixelOnTheSameBackgroundPassesWithoutTheMask() {
        final BufferedImage saved = window(0, 0, 3, false, TEXT);

        assertTrue(passes(strict(false, MARGIN), window(1, 0, 3, false, TEXT), saved));
        assertTrue(passes(strict(false, MARGIN), window(0, 0, 4, false, TEXT), saved));
    }

    @Test
    void changeInsideTheWindowStillFailsWithTheMask() {
        final BufferedImage saved = window(0, 0, 3, false, TEXT);
        final BufferedImage current = window(0, 0, 3, true, RED);

        assertFalse(passes(strict(true, MARGIN), current, saved));
    }

    @Test
    void missingTextStillFailsWithTheMask() {
        final BufferedImage saved = window(0, 0, 3, false, TEXT);
        final BufferedImage current = window(0, 0, 3, true, WINDOW);

        assertFalse(passes(strict(true, MARGIN), current, saved));
    }

    @Test
    void windowCoveredByAnotherWindowStillFailsWithTheMask() {
        final BufferedImage saved = window(0, 0, 3, false, TEXT);
        final BufferedImage current = window(0, 0, 3, true, TEXT);
        for (int x = COVER_START; x < COVER_END; x++) {
            for (int y = COVER_START; y < COVER_END; y++) {
                current.setRGB(x, y, FOREIGN_B);
            }
        }

        assertFalse(passes(strict(true, MARGIN), current, saved));
    }

    @Test
    void maskIsNotAppliedWhenTheWorkbenchColorWasNotRecorded() {
        final BufferedImage saved = window(0, 0, 3, false, TEXT);
        final BufferedImage current = window(0, 0, 3, true, TEXT);
        final ScreenshotCheck currentCheck = new ScreenshotCheck();
        currentCheck.setImage(current);
        final ScreenshotCheck savedCheck = new ScreenshotCheck();
        savedCheck.setFilename(ALTERNATIVE);
        savedCheck.setSa(new ScreenArea(0, 0, SIZE, SIZE));
        final PixelCheckResult result = new PixelCheckResult(currentCheck, savedCheck, new TestId("a", "b"), "default");
        result.putSavedImage(ALTERNATIVE, saved);

        assertFalse(strict(true, MARGIN).doCheck(result, ALTERNATIVE));
    }
}
