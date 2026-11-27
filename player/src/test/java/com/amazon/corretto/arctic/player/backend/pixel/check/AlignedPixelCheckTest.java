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
import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import com.amazon.corretto.arctic.common.repository.TestLoadRepository;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;
import com.amazon.corretto.arctic.player.backend.pixel.VerticalAligner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs the recorded check and the strict check over a test window that is displaced vertically during playback.
 */
class AlignedPixelCheckTest {
    private static final int SIZE = 100;
    private static final int WORKBENCH = 0xFFE6F0FA;
    private static final int WINDOW = 0xFFC0C0C0;
    private static final int BORDER = 0xFF202020;
    private static final int PANEL = 0xFFFFFFFF;
    private static final int RED = 0xFFFF0000;
    private static final int TOP = 30;
    private static final Path ALTERNATIVE = Path.of("recorded.png");

    private static void fill(final BufferedImage img, final int x0, final int y0, final int w, final int h,
                             final int color) {
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                img.setRGB(x, y, color);
            }
        }
    }

    private static BufferedImage screen(final int displacement, final int panelColor) {
        final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        fill(img, 0, 0, SIZE, SIZE, WORKBENCH);
        final int top = TOP + displacement;
        fill(img, 10, top, 80, 60, BORDER);
        fill(img, 11, top + 1, 78, 58, WINDOW);
        fill(img, 20, top + 10, 40, 10, panelColor);
        fill(img, 20, top + 30, 30, 15, panelColor);
        return img;
    }

    private static PixelCheckResult result(final BufferedImage current) {
        final ScreenshotCheck currentCheck = new ScreenshotCheck();
        currentCheck.setImage(current);
        final ScreenshotCheck savedCheck = new ScreenshotCheck();
        savedCheck.setFilename(ALTERNATIVE);
        savedCheck.setSa(new ScreenArea(0, 0, SIZE, SIZE));
        return new PixelCheckResult(currentCheck, savedCheck, new TestId("a", "b"), "default");
    }

    private static LoadRecordedPixelCheck loader(final BufferedImage recorded, final boolean align) {
        final TestLoadRepository repository = mock(TestLoadRepository.class);
        when(repository.loadImage("default", ALTERNATIVE)).thenReturn(recorded);
        return new LoadRecordedPixelCheck(repository, new VerticalAligner(align, 8));
    }

    private static StrictPixelCheck strict() {
        return new StrictPixelCheck(false, 0, false, 0, 0, 0f, 0, 0, 0, 0, 0, new HintMaskHelper(true, false));
    }

    @Test
    void displacedWindowFailsStrictWithoutAlignment() {
        final PixelCheckResult result = result(screen(2, PANEL));

        assertTrue(loader(screen(0, PANEL), false).doCheck(result, ALTERNATIVE));

        assertFalse(strict().doCheck(result, ALTERNATIVE));
        assertEquals(0, result.getAlignmentShift(ALTERNATIVE));
    }

    @Test
    void displacedWindowPassesStrictWithAlignment() {
        final PixelCheckResult result = result(screen(2, PANEL));

        assertTrue(loader(screen(0, PANEL), true).doCheck(result, ALTERNATIVE));

        assertEquals(2, result.getAlignmentShift(ALTERNATIVE));
        assertTrue(strict().doCheck(result, ALTERNATIVE));
    }

    @Test
    void realDifferenceStillFailsAfterAlignment() {
        final PixelCheckResult result = result(screen(2, RED));

        assertTrue(loader(screen(0, PANEL), true).doCheck(result, ALTERNATIVE));

        assertFalse(strict().doCheck(result, ALTERNATIVE));
    }

    @Test
    void originalImageIsKeptUntouched() {
        final BufferedImage current = screen(2, PANEL);
        final PixelCheckResult result = result(current);

        loader(screen(0, PANEL), true).doCheck(result, ALTERNATIVE);

        assertSame(current, result.getCurrentImage());
        assertEquals(PANEL, result.getCurrentImage().getRGB(25, TOP + 2 + 12));
    }

    @Test
    void alignedImageIsTheCurrentImageWhenNothingMoved() {
        final BufferedImage current = screen(0, PANEL);
        final PixelCheckResult result = result(current);

        loader(screen(0, PANEL), true).doCheck(result, ALTERNATIVE);

        assertSame(current, result.getAlignedImage(ALTERNATIVE));
        assertEquals(0, result.getAlignmentShift(ALTERNATIVE));
    }
}
