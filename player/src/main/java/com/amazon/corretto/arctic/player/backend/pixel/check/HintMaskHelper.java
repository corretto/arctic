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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.amazon.corretto.arctic.common.util.Pair;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that will generate the hint/mask for a specific image based on its failures.
 *
 * A hint is a blue outline that can be overlaid on top of the image comparison. This makes it easy to locate those
 * pixels that are different between image. A mask is similar to a hint, but it applies a blue hue to the pixel of the
 * image that are considered equal, leaving the different pixels (and a small margin around) untouched. Masks make it
 * easier to identify the changes when whole areas are different, instead of isolated pixels. It is also possible to
 * combine multiple masks at the same time to see which pixels are detected differently between two different pixel
 * checks.
 *
 * For both, masks and hints, there are two ways to calculate them, a fast and a high detail mode. In a fast mode, a
 * rectangle that includes all the different pixels is used (with the mask applying to pixels outside the rectangle.
 * For the high detail version, the outlines and masks will fit the failed pixels closely, including curve and multiple
 * independent regions.
 */
public final class HintMaskHelper {
    private static final Logger log = LoggerFactory.getLogger(HintMaskHelper.class);
    // Specify a 25% opacity
    private static final int MASK_ALPHA_MASK = 0x40FFFFFF;
    private static final int HINT_COLOR = Color.BLUE.getRGB() & MASK_ALPHA_MASK;
    private static final int MARGIN = 3;
    private final boolean fastMode;
    private final boolean generateMasks;

    /**
     * Creates a new instance. Usually called by the dependency injection software.
     * @param fastMode If enabled, uses just a rectangle to represents the hint/mask instead of fitting to the failures.
     * @param generateMasks If true, masks are generated in addition to the hint images.
     */
    @Inject
    public HintMaskHelper(@Named(InjectionKeys.BACKEND_SC_PIXEL_HINT_FAST) final boolean fastMode,
                          @Named(InjectionKeys.BACKEND_SC_PIXEL_HINT_MASK) final boolean generateMasks) {
        this.fastMode = fastMode;
        this.generateMasks = generateMasks;
    }

    /**
     * Generates the hint/mask images for the given failures.
     * @param failures A list of all the failures.
     * @param w Width of the desired hint/mask image.
     * @param h Height of the desired hint/mask image.
     * @return A pair of images. The mask image is optional.
     */
    public Pair<BufferedImage, Optional<BufferedImage>> drawImages(final Set<Integer>[] failures,
                                                                   final int w, final int h) {
        if (fastMode) {
            return drawImagesFastMode(failures, w, h);
        } else {
            return drawImagesSlowMode(failures, w, h);
        }
    }

    private Pair<BufferedImage, Optional<BufferedImage>> drawImagesSlowMode(final Set<Integer>[] failures, final int w,
                                                                            final int h) {
        final BufferedImage hint = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage mask;
        final Graphics2D maskGraphics;

        final Graphics hintGraphics = hint.createGraphics();
        hintGraphics.setColor(Color.BLUE);

        if (generateMasks) {
            mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            maskGraphics = mask.createGraphics();
            maskGraphics.setColor(new Color(HINT_COLOR, true));
            maskGraphics.fillRect(0, 0, mask.getWidth(), mask.getHeight());
            maskGraphics.setBackground(new Color(0x00000000, true));
        } else {
            mask = null;
        }
        IntStream.range(0, failures.length)
                .filter(x -> failures[x] != null && !failures[x].isEmpty())
                .parallel()
                .forEach(x -> failures[x].forEach(y -> safeDrawRect(hint, hintGraphics, x - MARGIN, y - MARGIN,
                        MARGIN * 2, MARGIN * 2)));
        IntStream.range(0, failures.length)
                .filter(x -> failures[x] != null && !failures[x].isEmpty())
                .parallel()
                .forEach(x -> failures[x].forEach(y -> clearPixelAround(x, y, hint, mask, generateMasks)));
        return Pair.of(hint, Optional.ofNullable(mask));
    }

    private void safeDrawRect(final BufferedImage img, final Graphics hintGraphics, final int x, final int y,
                              final int w, final int h) {
        final int safeX = Math.max(0, x);
        final int safeY = Math.max(0, y);
        final int safeW = Math.min(img.getWidth() - safeX, w);
        final int safeH = Math.min(img.getHeight() - safeY, h);

        try {
            hintGraphics.drawRect(safeX, safeY, safeW, safeH);
        } catch (NullPointerException e) {
            log.debug("Rectangle {}:{}:{}:{} ({}:{}:{}:{}) skipped", x, y, w, h, safeX, safeY, safeW, safeH);
        }
    }

    private Pair<BufferedImage, Optional<BufferedImage>> drawImagesFastMode(final Set<Integer>[] failures, final int w,
                                                                            final int h) {
        final BufferedImage hint = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage mask;
        final Graphics2D maskGraphics;

        final Graphics hintGraphics = hint.createGraphics();
        hintGraphics.setColor(Color.BLUE);

        if (generateMasks) {
            mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            maskGraphics = mask.createGraphics();
            maskGraphics.setColor(new Color(HINT_COLOR, true));
            maskGraphics.fillRect(0, 0, mask.getWidth(), mask.getHeight());
            maskGraphics.setBackground(new Color(0x00000000, true));
        } else {
            maskGraphics = null;
            mask = null;
        }

        int minx = Integer.MAX_VALUE;
        int miny = Integer.MAX_VALUE;
        int maxx = 0;
        int maxy = 0;
        for (int x = 0; x < failures.length; x++) {
            if (failures[x] != null && !failures[x].isEmpty()) {
                minx = Math.min(minx, x);
                maxx = Math.max(maxx, x);
                for (Integer y : failures[x]) {
                    miny = Math.min(miny, y);
                    maxy = Math.max(maxy, y);
                }
            }
        }
        minx = Math.max(0, minx - MARGIN);
        miny = Math.max(0, miny - MARGIN);
        maxx = Math.min(hint.getWidth(), (maxx - minx) + MARGIN + 1);
        maxy = Math.min(hint.getWidth(), (maxy - miny) + MARGIN + 1);
        hintGraphics.setColor(Color.BLUE);
        safeDrawRect(hint, hintGraphics, minx, miny, maxx, maxy);
        if (generateMasks) {
            maskGraphics.clearRect(minx, miny, maxx, maxy);
        }
        return Pair.of(hint, Optional.ofNullable(mask));
    }

    private void clearPixelAround(final int x, final int y, final BufferedImage hint, final BufferedImage mask,
                          final boolean doMask) {
        for (int i = x - (MARGIN - 1); i < x + (MARGIN); i++) {
            for (int j = y - (MARGIN - 1); j < y + (MARGIN); j++) {
                if (i > 0 && i < hint.getWidth() && j > 0 && j < hint.getHeight()) {
                    hint.setRGB(i, j, 0);
                    if (doMask) {
                        mask.setRGB(i, j, 0);
                    }
                }
            }
        }
    }
}
