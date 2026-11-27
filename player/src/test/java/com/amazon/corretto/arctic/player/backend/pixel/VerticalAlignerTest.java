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

package com.amazon.corretto.arctic.player.backend.pixel;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the detection of vertical displacement of the test window using synthetic images.
 */
class VerticalAlignerTest {
    private static final int W = 120;
    private static final int H = 120;
    private static final int WORKBENCH = 0xFFE6F0FA;
    private static final int TITLE = 0xFFEBE7EB;
    private static final int WINDOW = 0xFFC0C0C0;
    private static final int PANEL = 0xFFFFFFFF;
    private static final int BORDER = 0xFF202020;
    private static final int SHADE = 0xFFF2FFE6;
    private static final int TITLE_ROWS = 8;
    private static final int WINDOW_LEFT = 10;
    private static final int WINDOW_WIDTH = 100;
    private static final int WINDOW_TOP = 40;
    private static final int WINDOW_HEIGHT = 70;
    private static final int MAX_SHIFT = 8;

    private static VerticalAligner aligner() {
        return new VerticalAligner(true, MAX_SHIFT);
    }

    private static void fill(final BufferedImage img, final int x0, final int y0, final int w, final int h,
                             final int color) {
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                if (x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight()) {
                    img.setRGB(x, y, color);
                }
            }
        }
    }

    // A workbench title bar that never moves, and a test window that starts at WINDOW_TOP plus the given displacement.
    private static BufferedImage screen(final int displacement) {
        final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        fill(img, 0, 0, W, H, WORKBENCH);
        fill(img, 0, 0, W, TITLE_ROWS, TITLE);
        final int top = WINDOW_TOP + displacement;
        fill(img, WINDOW_LEFT, top, WINDOW_WIDTH, WINDOW_HEIGHT, BORDER);
        fill(img, WINDOW_LEFT + 1, top + 1, WINDOW_WIDTH - 2, WINDOW_HEIGHT - 2, WINDOW);
        fill(img, WINDOW_LEFT + 10, top + 10, 60, 12, PANEL);
        fill(img, WINDOW_LEFT + 10, top + 30, 40, 20, PANEL);
        return img;
    }

    // Number of pixels that differ, ignoring the rows above fromRow. The title bar does not move with the test window,
    // so it ends up displaced after the whole image is moved.
    private static int mismatches(final BufferedImage a, final BufferedImage b, final int fromRow) {
        int count = 0;
        for (int y = fromRow; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void identicalImagesAreNotMoved() {
        final BufferedImage recorded = screen(0);
        final BufferedImage current = screen(0);

        final VerticalAligner.Alignment alignment = aligner().align(recorded, current, Collections.emptyList());

        assertEquals(0, alignment.getShift());
        assertSame(current, alignment.getImage());
    }

    @Test
    void windowLowerThanRecordedIsMovedUp() {
        final BufferedImage recorded = screen(0);
        final BufferedImage current = screen(2);

        final VerticalAligner.Alignment alignment = aligner().align(recorded, current, Collections.emptyList());

        assertEquals(2, alignment.getShift());
        assertEquals(0, mismatches(recorded, alignment.getImage(), TITLE_ROWS + MAX_SHIFT));
    }

    @Test
    void windowHigherThanRecordedIsMovedDown() {
        final BufferedImage recorded = screen(0);
        final BufferedImage current = screen(-3);

        final VerticalAligner.Alignment alignment = aligner().align(recorded, current, Collections.emptyList());

        assertEquals(-3, alignment.getShift());
        assertEquals(0, mismatches(recorded, alignment.getImage(), TITLE_ROWS + MAX_SHIFT));
    }

    @Test
    void wholeImageIsMovedByTheSameAmount() {
        final BufferedImage recorded = screen(0);
        final BufferedImage current = screen(2);

        final BufferedImage aligned = aligner().align(recorded, current, Collections.emptyList()).getImage();

        // Every row comes from two rows below, including the ones that hold the title bar, which never moved.
        for (int y = 0; y < H - 2; y++) {
            assertEquals(current.getRGB(5, y + 2), aligned.getRGB(5, y));
        }
        // Rows that would come from outside the image keep their content.
        for (int y = H - 2; y < H; y++) {
            assertEquals(current.getRGB(5, y), aligned.getRGB(5, y));
        }
    }

    @Test
    void shiftLargerThanTheLimitIsIgnored() {
        final BufferedImage recorded = screen(0);
        final BufferedImage current = screen(MAX_SHIFT + 2);

        final VerticalAligner.Alignment alignment = aligner().align(recorded, current, Collections.emptyList());

        assertEquals(0, alignment.getShift());
        assertSame(current, alignment.getImage());
    }

    @Test
    void shiftAtTheLimitIsUsed() {
        final BufferedImage recorded = screen(0);

        assertEquals(MAX_SHIFT, aligner().align(recorded, screen(MAX_SHIFT), Collections.emptyList()).getShift());
        assertEquals(-MAX_SHIFT, aligner().align(recorded, screen(-MAX_SHIFT), Collections.emptyList()).getShift());
    }

    @Test
    void disabledAlignerNeverMoves() {
        final BufferedImage current = screen(2);

        final VerticalAligner.Alignment alignment = new VerticalAligner(false, MAX_SHIFT)
                .align(screen(0), current, Collections.emptyList());

        assertEquals(0, alignment.getShift());
        assertSame(current, alignment.getImage());
    }

    @Test
    void zeroMaxShiftNeverMoves() {
        final VerticalAligner aligner = new VerticalAligner(true, 0);

        assertEquals(0, aligner.align(screen(0), screen(2), Collections.emptyList()).getShift());
        assertTrue(!aligner.isEnabled());
    }

    @Test
    void imagesOfDifferentSizeAreNotMoved() {
        final BufferedImage current = new BufferedImage(W + 1, H, BufferedImage.TYPE_INT_ARGB);

        final VerticalAligner.Alignment alignment = aligner().align(screen(0), current, Collections.emptyList());

        assertEquals(0, alignment.getShift());
        assertSame(current, alignment.getImage());
    }

    @Test
    void imageWithoutEdgesIsNotMoved() {
        final BufferedImage recorded = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        fill(recorded, 0, 0, W, H, WORKBENCH);

        assertEquals(0, aligner().align(recorded, screen(2), Collections.emptyList()).getShift());
    }

    @Test
    void shadeCoveringTheTopOfTheWindowDoesNotPreventAlignment() {
        // A shade, drawn in the same place in both images, covers the whole top of the test window.
        final ScreenArea shade = new ScreenArea(0, WINDOW_TOP - 10, W, 30);
        final BufferedImage recorded = screen(0);
        final BufferedImage current = screen(2);
        fill(recorded, shade.getX(), shade.getY(), shade.getW(), shade.getH(), SHADE);
        fill(current, shade.getX(), shade.getY(), shade.getW(), shade.getH(), SHADE);

        final VerticalAligner.Alignment alignment = aligner().align(recorded, current, List.of(shade));

        assertEquals(2, alignment.getShift());
    }

    @Test
    void windowThatDoesNotMoveWithTheTestIsNotUsedToDecide() {
        // An unrelated window that is at the same place in both images, next to the test window that moved.
        final BufferedImage recorded = screen(0);
        final BufferedImage current = screen(3);
        for (final BufferedImage img : List.of(recorded, current)) {
            fill(img, 0, 100, W, 20, BORDER);
            fill(img, 4, 104, W - 8, 12, PANEL);
        }

        final VerticalAligner.Alignment alignment = aligner().align(recorded, current, Collections.emptyList());

        assertEquals(3, alignment.getShift());
    }

    @Test
    void backgroundWindowOnlyInPlaybackDoesNotChangeTheShift() {
        final BufferedImage recorded = screen(0);
        final BufferedImage current = screen(2);
        // Something that was not there during the recording, over the area the shifted window does not cover.
        fill(current, 0, 14, 10, 20, BORDER);
        fill(current, 111, 14, 9, 20, BORDER);

        assertEquals(2, aligner().align(recorded, current, Collections.emptyList()).getShift());
    }

    @Test
    void smallDifferenceInColorsStillMatches() {
        final BufferedImage recorded = screen(0);
        final BufferedImage current = screen(2);
        // Playback colors that deviate slightly, like window chrome that is tinted by the wallpaper.
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                final int rgb = current.getRGB(x, y);
                current.setRGB(x, y, (rgb & 0xFF000000) | ((rgb & 0xFF0000) - 0x030000 & 0xFF0000)
                        | (rgb & 0x00FFFF));
            }
        }

        assertEquals(2, aligner().align(recorded, current, Collections.emptyList()).getShift());
    }

    @Test
    void findShiftTriesZeroFirst() {
        assertEquals(0, aligner().findShift(screen(0), screen(0), Collections.emptyList()));
    }
}
