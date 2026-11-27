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
import java.util.ArrayList;
import java.util.List;

import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Operating systems sometimes place the test window a few pixels higher or lower than where it was when the test was
 * recorded, which makes every pixel below the window top differ. This class finds that vertical displacement and
 * produces a copy of the playback image that is moved, as a whole, to line up with the recorded one.
 *
 * <p>It does not look for the test window itself, as a shade or an unrelated window can cover any part of it. Instead
 * it takes the horizontal edges of the recorded image, away from the shades, and tests how many of them are found in
 * the playback image when it is moved up or down. Shifts are tried outwards from zero. The shift under which most edges
 * match is used, as long as it is clearly better than not shifting at all. Edges that belong to windows that are not
 * part of the test match under no shift, so they do not change the result.
 */
public final class VerticalAligner {
    /** Minimum color change between vertical neighbours for a pixel to be a horizontal edge. */
    static final int GRADIENT = 24;
    /** How much each color component can differ between an edge in both images to still count as a match. */
    static final int TOLERANCE = 8;
    /** Pixels around the shades that are not used to find edges, as shades cast shadows. */
    static final int SHADE_PADDING = 4;
    /** If there are fewer edges than this, there is not enough information to move the image. */
    static final int MIN_SAMPLES = 16;
    /** A shift is only used if it matches at least this many more edges than not shifting. */
    static final int MIN_GAIN = 16;
    /** A shift is only used if it matches at least this fraction of the edges more than not shifting. */
    static final double MIN_GAIN_RATIO = 0.01;

    private static final int BYTE_MASK = 0xFF;
    private static final int BITS_PER_BYTE = 8;
    private static final int COMPONENTS = 3;

    private final boolean enabled;
    private final int maxShift;

    /**
     * Creates a new instance. Usually called by the dependency injection software.
     * @param enabled Whether to align images. If false, {@link #align} returns the images unchanged.
     * @param maxShift Maximum number of pixels, up or down, the playback image can be moved.
     */
    @Inject
    public VerticalAligner(@Named(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_ENABLED) final boolean enabled,
                           @Named(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_MAX_SHIFT) final int maxShift) {
        this.enabled = enabled;
        this.maxShift = maxShift;
    }

    /**
     * Whether images are aligned.
     * @return True if {@link #align} can move the image.
     */
    public boolean isEnabled() {
        return enabled && maxShift > 0;
    }

    /**
     * Aligns the image captured during playback with the recorded one.
     * @param recorded Image that was captured during the recording.
     * @param current Image that was captured during playback.
     * @param shades Areas, relative to the image, that are covered by shades. Edges around them are not used.
     * @return The alignment. If no shift is needed or possible, it holds the current image unchanged.
     */
    public Alignment align(final BufferedImage recorded, final BufferedImage current, final List<ScreenArea> shades) {
        if (!isEnabled() || recorded == null || current == null
                || recorded.getWidth() != current.getWidth() || recorded.getHeight() != current.getHeight()) {
            return new Alignment(0, current);
        }
        final int w = recorded.getWidth();
        final int h = recorded.getHeight();
        final int[] rec = recorded.getRGB(0, 0, w, h, null, 0, w);
        final int[] cur = current.getRGB(0, 0, w, h, null, 0, w);
        final int[] samples = collectEdges(rec, w, h, shades);
        final int shift = findShift(rec, cur, w, h, samples);
        if (shift == 0) {
            return new Alignment(0, current);
        }
        return new Alignment(shift, shift(cur, w, h, shift));
    }

    /**
     * Finds how many rows the content of the playback image is below (positive) or above (negative) the same content
     * in the recorded image.
     * @param recorded Image that was captured during the recording.
     * @param current Image that was captured during playback.
     * @param shades Areas, relative to the image, that are covered by shades.
     * @return The shift, or 0 if the images are already aligned or there is no clear candidate.
     */
    int findShift(final BufferedImage recorded, final BufferedImage current, final List<ScreenArea> shades) {
        final int w = recorded.getWidth();
        final int h = recorded.getHeight();
        final int[] rec = recorded.getRGB(0, 0, w, h, null, 0, w);
        final int[] cur = current.getRGB(0, 0, w, h, null, 0, w);
        return findShift(rec, cur, w, h, collectEdges(rec, w, h, shades));
    }

    private int findShift(final int[] rec, final int[] cur, final int w, final int h, final int[] samples) {
        if (samples.length < MIN_SAMPLES) {
            return 0;
        }
        int best = 0;
        int bestCount = countMatches(rec, cur, w, h, samples, 0);
        final int baseCount = bestCount;
        for (int distance = 1; distance <= maxShift; distance++) {
            for (final int shift : new int[] {distance, -distance}) {
                final int count = countMatches(rec, cur, w, h, samples, shift);
                if (count > bestCount) {
                    bestCount = count;
                    best = shift;
                }
            }
        }
        final int requiredGain = Math.max(MIN_GAIN, (int) Math.ceil(MIN_GAIN_RATIO * samples.length));
        return bestCount - baseCount >= requiredGain ? best : 0;
    }

    // Creates an image where every row comes from the row that is shift rows below. The whole image is moved as one,
    // so rows are never made of pieces of two different shifts. Parts of the screen that do not move with the test
    // window, like the title bar of the workbench or a shade, end up displaced, so they have to be excluded from the
    // comparison (see the background boundary and the shades). The rows that would come from outside of the image
    // keep their original content.
    static BufferedImage shift(final int[] src, final int w, final int h, final int shift) {
        final int[] out = new int[src.length];
        for (int y = 0; y < h; y++) {
            final int from = y + shift >= 0 && y + shift < h ? y + shift : y;
            System.arraycopy(src, from * w, out, y * w, w);
        }
        final BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, w, h, out, 0, w);
        return result;
    }

    private int[] collectEdges(final int[] rec, final int w, final int h, final List<ScreenArea> shades) {
        final int exclusion = SHADE_PADDING + maxShift;
        final List<ScreenArea> excluded = new ArrayList<>();
        for (final ScreenArea s : shades) {
            excluded.add(new ScreenArea(s.getX() - exclusion, s.getY() - exclusion, s.getW() + 2 * exclusion,
                    s.getH() + 2 * exclusion));
        }
        final int[] found = new int[w * h];
        int count = 0;
        for (int y = 1; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int i = y * w + x;
                if (maxDiff(rec[i], rec[i - w]) > GRADIENT && !covered(x, y, excluded)) {
                    found[count++] = i;
                }
            }
        }
        final int[] samples = new int[count];
        System.arraycopy(found, 0, samples, 0, count);
        return samples;
    }

    private static boolean covered(final int x, final int y, final List<ScreenArea> areas) {
        for (final ScreenArea a : areas) {
            if (x >= a.getX() && x < a.getX() + a.getW() && y >= a.getY() && y < a.getY() + a.getH()) {
                return true;
            }
        }
        return false;
    }

    private static int countMatches(final int[] rec, final int[] cur, final int w, final int h, final int[] samples,
                                    final int shift) {
        int count = 0;
        for (final int i : samples) {
            if (matches(rec, cur, w, h, i, shift)) {
                count++;
            }
        }
        return count;
    }

    // Whether the edge between pixel i and the one above it in the recorded image is found shift rows away.
    private static boolean matches(final int[] rec, final int[] cur, final int w, final int h, final int i,
                                   final int shift) {
        final int shifted = i / w + shift;
        if (shifted < 1 || shifted >= h) {
            return false;
        }
        final int j = i + shift * w;
        return maxDiff(rec[i], cur[j]) <= TOLERANCE && maxDiff(rec[i - w], cur[j - w]) <= TOLERANCE;
    }

    private static int maxDiff(final int a, final int b) {
        int max = 0;
        for (int c = 0; c < COMPONENTS; c++) {
            final int shiftBits = c * BITS_PER_BYTE;
            max = Math.max(max, Math.abs(((a >>> shiftBits) & BYTE_MASK) - ((b >>> shiftBits) & BYTE_MASK)));
        }
        return max;
    }

    /**
     * Result of aligning the playback image to the recorded one.
     */
    public static final class Alignment {
        private final int shift;
        private final BufferedImage image;

        Alignment(final int shift, final BufferedImage image) {
            this.shift = shift;
            this.image = image;
        }

        /**
         * Rows the content of the playback image is below (positive) or above (negative) the recorded one.
         * @return The detected shift in pixels. 0 if the image was not moved.
         */
        public int getShift() {
            return shift;
        }

        /**
         * The playback image moved to line up with the recorded one. This is the same instance that was passed in if
         * the shift is 0.
         * @return The aligned image.
         */
        public BufferedImage getImage() {
            return image;
        }
    }
}
