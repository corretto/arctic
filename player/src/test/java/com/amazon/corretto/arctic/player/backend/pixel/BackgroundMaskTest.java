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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundMaskTest {
    private static final int SIZE = 10;
    private static final int BACKGROUND = 0xFFE6F0FA;
    private static final int OTHER = 0xFF202020;
    private static final int BACKGROUND_NO_ALPHA = 0x00E6F0FA;
    private static final int BACKGROUND_OFF_BY_TWO = 0xFFE4F0FB;

    private static BufferedImage image(final int color) {
        final BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                img.setRGB(x, y, color);
            }
        }
        return img;
    }

    @Test
    void onlyPixelsWithTheWorkbenchColorAreBackground() {
        final BufferedImage img = image(OTHER);
        img.setRGB(5, 5, BACKGROUND);

        final BackgroundMask mask = BackgroundMask.create(img, BACKGROUND, 0, 0);

        assertEquals(1, mask.getCount());
        assertTrue(mask.isMasked(5, 5));
        assertFalse(mask.isMasked(4, 5));
        assertFalse(mask.isMasked(5, 6));
    }

    @Test
    void alphaOfTheColorIsIgnored() {
        final BufferedImage img = image(OTHER);
        img.setRGB(5, 5, BACKGROUND);

        assertTrue(BackgroundMask.create(img, BACKGROUND_NO_ALPHA, 0, 0).isMasked(5, 5));
    }

    @Test
    void toleranceAcceptsSmallDeviations() {
        final BufferedImage img = image(OTHER);
        img.setRGB(5, 5, BACKGROUND_OFF_BY_TWO);

        assertFalse(BackgroundMask.create(img, BACKGROUND, 0, 0).isMasked(5, 5));
        assertTrue(BackgroundMask.create(img, BACKGROUND, 2, 0).isMasked(5, 5));
    }

    @Test
    void marginGrowsTheMaskInAllDirections() {
        final BufferedImage img = image(OTHER);
        img.setRGB(5, 5, BACKGROUND);

        final BackgroundMask mask = BackgroundMask.create(img, BACKGROUND, 0, 1);

        assertEquals(9, mask.getCount());
        for (int x = 4; x <= 6; x++) {
            for (int y = 4; y <= 6; y++) {
                assertTrue(mask.isMasked(x, y), x + "," + y);
            }
        }
        assertFalse(mask.isMasked(3, 5));
        assertFalse(mask.isMasked(7, 5));
        assertFalse(mask.isMasked(5, 3));
        assertFalse(mask.isMasked(5, 7));
    }

    @Test
    void marginIsClippedAtTheImageBorder() {
        final BufferedImage img = image(OTHER);
        img.setRGB(0, 0, BACKGROUND);

        assertEquals(4, BackgroundMask.create(img, BACKGROUND, 0, 1).getCount());
    }

    @Test
    void imageWithoutBackgroundHasEmptyMask() {
        assertEquals(0, BackgroundMask.create(image(OTHER), BACKGROUND, 0, 2).getCount());
    }

    @Test
    void imageThatIsAllBackgroundIsFullyMasked() {
        assertEquals(SIZE * SIZE, BackgroundMask.create(image(BACKGROUND), BACKGROUND, 0, 2).getCount());
    }

    // Darkens the workbench color to the given fraction, like a shadow over it.
    private static int shadow(final double factor) {
        final int r = (int) Math.round(0xE6 * factor);
        final int g = (int) Math.round(0xF0 * factor);
        final int b = (int) Math.round(0xFA * factor);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    // Workbench background with a band of shadow that gets darker, like the one around a window, and a dark window
    // area that is not connected to the background.
    private static BufferedImage withShadow() {
        final BufferedImage img = image(BACKGROUND);
        for (int x = 0; x < SIZE; x++) {
            img.setRGB(x, 0, shadow(0.8));
            img.setRGB(x, 1, shadow(0.6));
        }
        return img;
    }

    @Test
    void shadowIsNotMaskedUnlessEnabled() {
        final BackgroundMask mask = BackgroundMask.create(withShadow(), BACKGROUND, 0, 0, 0, 0, 0, 0, 0f, 3);

        assertFalse(mask.isMasked(5, 0));
        assertFalse(mask.isMasked(5, 1));
    }

    @Test
    void shadowConnectedToTheBackgroundIsMasked() {
        final BackgroundMask mask = BackgroundMask.create(withShadow(), BACKGROUND, 0, 0, 0, 0, 0, 0, 0.5f, 3);

        assertEquals(SIZE * SIZE, mask.getCount());
    }

    @Test
    void shadowDarkerThanTheLimitIsNotMasked() {
        final BufferedImage img = withShadow();
        for (int x = 0; x < SIZE; x++) {
            img.setRGB(x, 1, shadow(0.4));
        }

        final BackgroundMask mask = BackgroundMask.create(img, BACKGROUND, 0, 0, 0, 0, 0, 0, 0.3f, 3);

        assertFalse(mask.isMasked(5, 0));
        assertFalse(mask.isMasked(5, 1));
    }

    @Test
    void shadowChainIsFollowedThroughOtherShadowPixels() {
        // Only the last row touches the background, the others are reached through the pixels in between.
        final BufferedImage img = image(BACKGROUND);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < SIZE; x++) {
                img.setRGB(x, y, shadow(0.5 + 0.1 * y));
            }
        }

        assertEquals(SIZE * SIZE, BackgroundMask.create(img, BACKGROUND, 0, 0, 0, 0, 0, 0, 0.6f, 3).getCount());
    }

    @Test
    void darkAreaNotConnectedToTheBackgroundIsNotMasked() {
        final BufferedImage img = image(OTHER);
        img.setRGB(5, 5, shadow(0.8));
        img.setRGB(0, 0, BACKGROUND);

        final BackgroundMask mask = BackgroundMask.create(img, BACKGROUND, 0, 0, 0, 0, 0, 0, 0.5f, 3);

        assertTrue(mask.isMasked(0, 0));
        assertFalse(mask.isMasked(5, 5));
    }

    @Test
    void darkGrayThatIsNotAShadowOfTheWorkbenchIsNotMasked() {
        // Touches the background, but a neutral gray is not the workbench color scaled.
        final BufferedImage img = image(BACKGROUND);
        img.setRGB(5, 5, 0xFF808080);

        assertFalse(BackgroundMask.create(img, BACKGROUND, 0, 0, 0, 0, 0, 0, 0.6f, 3).isMasked(5, 5));
    }

    @Test
    void shadowToleranceAcceptsSmallDeviations() {
        final BufferedImage img = image(BACKGROUND);
        img.setRGB(5, 5, shadow(0.7) + 3);

        assertFalse(BackgroundMask.create(img, BACKGROUND, 0, 0, 0, 0, 0, 0, 0.5f, 0).isMasked(5, 5));
        assertTrue(BackgroundMask.create(img, BACKGROUND, 0, 0, 0, 0, 0, 0, 0.5f, 3).isMasked(5, 5));
    }

    @Test
    void shadowIsGrownByTheMargin() {
        final BufferedImage img = image(OTHER);
        img.setRGB(5, 5, BACKGROUND);
        img.setRGB(6, 5, shadow(0.7));

        final BackgroundMask mask = BackgroundMask.create(img, BACKGROUND, 0, 1, 0, 0, 0, 0, 0.5f, 3);

        assertTrue(mask.isMasked(7, 5));
        assertFalse(mask.isMasked(8, 5));
    }

    @Test
    void boundariesMaskEachSideIndependently() {
        final BackgroundMask mask = BackgroundMask.create(image(OTHER), BACKGROUND, 0, 0, 1, 2, 3, 4);

        assertTrue(mask.isMasked(5, 0));
        assertFalse(mask.isMasked(5, 1));
        assertTrue(mask.isMasked(5, SIZE - 2));
        assertFalse(mask.isMasked(5, SIZE - 3));
        assertTrue(mask.isMasked(2, 5));
        assertFalse(mask.isMasked(3, 5));
        assertTrue(mask.isMasked(SIZE - 4, 5));
        assertFalse(mask.isMasked(SIZE - 5, 5));
    }

    @Test
    void boundariesCountMatchesTheStrips() {
        // 10x10 image, rows 0 and 9 fully masked, columns 0 and 1 masked in the 8 rows left.
        final BackgroundMask mask = BackgroundMask.create(image(OTHER), BACKGROUND, 0, 0, 1, 1, 2, 0);

        assertEquals(2 * SIZE + 2 * (SIZE - 2), mask.getCount());
    }

    @Test
    void boundariesLargerThanTheImageMaskEverything() {
        assertEquals(SIZE * SIZE, BackgroundMask.create(image(OTHER), BACKGROUND, 0, 0, SIZE * 2, 0, 0, 0).getCount());
    }

    @Test
    void boundariesWorkWithoutWorkbenchColor() {
        final BackgroundMask mask = BackgroundMask.create(image(BACKGROUND), null, 0, 2, 1, 0, 0, 0);

        assertEquals(SIZE, mask.getCount());
        assertTrue(mask.isMasked(0, 0));
        assertFalse(mask.isMasked(0, 1));
    }
}
