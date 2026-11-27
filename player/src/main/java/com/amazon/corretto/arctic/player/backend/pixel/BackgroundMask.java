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

/**
 * Identifies the pixels of a recorded image that are workbench background, that is, pixels where nothing but the
 * workbench was visible when the image was recorded, optionally including the drop shadows cast over it. The mask is
 * created dynamically from the recorded image and the color of the workbench, so no extra data needs to be stored
 * with the recording.
 *
 * Whatever is drawn on those pixels during playback, for example an accidental window or application behind the
 * test, is irrelevant for the comparison. The mask is grown by a margin, so the pixels right next to the background
 * are ignored as well. Those are the pixels that move when a window is shifted by a pixel or has its corners rounded
 * differently, and the ones that are blended with whatever was behind the window, such as anti-aliased corners and
 * shadows.
 */
public final class BackgroundMask {
    private static final int BYTE_MASK = 0xFF;
    private static final int COMPONENTS = 3;

    private final int width;
    private final boolean[] masked;
    private final int count;

    private BackgroundMask(final int width, final boolean[] masked) {
        this.width = width;
        this.masked = masked;
        int total = 0;
        for (final boolean it : masked) {
            if (it) {
                total++;
            }
        }
        this.count = total;
    }

    /**
     * Creates the mask for a recorded image.
     * @param recorded Image captured during the recording.
     * @param color Color of the workbench when the image was recorded. The alpha component is ignored.
     * @param tolerance How much each of the RGB components of a pixel can deviate from the color to still be
     *                  considered background.
     * @param margin How many pixels around the background are also masked.
     * @return A mask where background pixels, and the pixels around them, are set.
     */
    public static BackgroundMask create(final BufferedImage recorded, final int color, final int tolerance,
                                        final int margin) {
        return create(recorded, color, tolerance, margin, 0, 0, 0, 0);
    }

    /**
     * Creates the mask for a recorded image, additionally masking strips along the borders of the image. The strips
     * cover the pixels that are not workbench color but are not reliable either, such as the rounded corners of the
     * window that holds the workbench, where the desktop shows through.
     * @param recorded Image captured during the recording.
     * @param color Color of the workbench when the image was recorded. The alpha component is ignored. If null, no
     *              pixel is considered background, and only the border strips are masked.
     * @param tolerance How much each of the RGB components of a pixel can deviate from the color to still be
     *                  considered background.
     * @param margin How many pixels around the background are also masked.
     * @param top Height in pixels of the strip masked along the top border.
     * @param bottom Height in pixels of the strip masked along the bottom border.
     * @param left Width in pixels of the strip masked along the left border.
     * @param right Width in pixels of the strip masked along the right border.
     * @return A mask where background pixels, the pixels around them and the border strips are set.
     */
    public static BackgroundMask create(final BufferedImage recorded, final Integer color, final int tolerance,
                                        final int margin, final int top, final int bottom, final int left,
                                        final int right) {
        return create(recorded, color, tolerance, margin, top, bottom, left, right, 0f, 0);
    }

    /**
     * Creates the mask for a recorded image, like
     * {@link #create(BufferedImage, Integer, int, int, int, int, int, int)}, and also masks the drop shadows cast over
     * the background. A shadow is a semi transparent black over the workbench, so each of its pixels is the workbench
     * color scaled by a factor below 1. Only the shadow pixels that are connected to background pixels, directly or
     * through other shadow pixels, are masked. A dark area of the window itself is not connected to the background
     * through pixels of that kind, so it is not masked.
     * @param recorded Image captured during the recording.
     * @param color Color of the workbench when the image was recorded. The alpha component is ignored. If null, no
     *              pixel is considered background, and only the border strips are masked.
     * @param tolerance How much each of the RGB components of a pixel can deviate from the color to still be
     *                  considered background.
     * @param margin How many pixels around the background and the shadows are also masked.
     * @param top Height in pixels of the strip masked along the top border.
     * @param bottom Height in pixels of the strip masked along the bottom border.
     * @param left Width in pixels of the strip masked along the left border.
     * @param right Width in pixels of the strip masked along the right border.
     * @param shadowDarkness How dark a shadow can be, as a fraction of the workbench color that it removes. For
     *                       example 0.6 accepts colors from 40% to 100% of the workbench color. 0 disables the
     *                       detection of shadows.
     * @param shadowTolerance How much each of the RGB components of a pixel can deviate from the scaled workbench
     *                        color to still be considered shadow.
     * @return A mask where background pixels, shadows, the pixels around them and the border strips are set.
     */
    public static BackgroundMask create(final BufferedImage recorded, final Integer color, final int tolerance,
                                        final int margin, final int top, final int bottom, final int left,
                                        final int right, final float shadowDarkness, final int shadowTolerance) {
        final int w = recorded.getWidth();
        final int h = recorded.getHeight();
        boolean[] result = new boolean[w * h];
        if (color != null) {
            final int[] pixels = recorded.getRGB(0, 0, w, h, null, 0, w);
            for (int i = 0; i < pixels.length; i++) {
                result[i] = maxComponentDiff(pixels[i], color) <= tolerance;
            }
            if (shadowDarkness > 0) {
                addShadows(result, pixels, w, h, color, shadowDarkness, shadowTolerance);
            }
            if (margin > 0) {
                result = dilate(dilate(result, w, h, margin, true), w, h, margin, false);
            }
        }
        for (int y = 0; y < h; y++) {
            final boolean rowMasked = y < top || y >= h - bottom;
            for (int x = 0; x < w; x++) {
                if (rowMasked || x < left || x >= w - right) {
                    result[y * w + x] = true;
                }
            }
        }
        return new BackgroundMask(w, result);
    }

    /**
     * Whether a pixel of the recorded image is ignored by the mask.
     * @param x x coordinate of the pixel.
     * @param y y coordinate of the pixel.
     * @return True if the pixel is background, or within the margin of it.
     */
    public boolean isMasked(final int x, final int y) {
        return masked[y * width + x];
    }

    /**
     * Number of pixels that are masked.
     * @return Number of pixels that are masked.
     */
    public int getCount() {
        return count;
    }

    // Sets the pixels that are a shadow over the workbench, and are connected to the pixels that are already set. The
    // connection goes through the eight neighbours, so a shadow that follows a rounded corner is followed all the way.
    private static void addShadows(final boolean[] masked, final int[] pixels, final int w, final int h,
                                   final int color, final float darkness, final int tolerance) {
        final int[] queue = new int[masked.length];
        int tail = 0;
        for (int i = 0; i < masked.length; i++) {
            if (masked[i]) {
                queue[tail++] = i;
            }
        }
        int head = 0;
        while (head < tail) {
            final int index = queue[head++];
            final int x = index % w;
            final int y = index / w;
            for (int j = Math.max(0, y - 1); j <= Math.min(h - 1, y + 1); j++) {
                for (int i = Math.max(0, x - 1); i <= Math.min(w - 1, x + 1); i++) {
                    final int neighbour = j * w + i;
                    if (!masked[neighbour] && isShadow(pixels[neighbour], color, darkness, tolerance)) {
                        masked[neighbour] = true;
                        queue[tail++] = neighbour;
                    }
                }
            }
        }
    }

    // A shadow is the color scaled by a factor between 1 - darkness and 1. The factor is the one that fits the pixel
    // best, and then each component has to be close to the scaled one.
    private static boolean isShadow(final int px, final int color, final float darkness, final int tolerance) {
        long dot = 0;
        long length = 0;
        for (int i = 0; i < COMPONENTS; i++) {
            final int base = component(color, i);
            dot += (long) component(px, i) * base;
            length += (long) base * base;
        }
        if (length == 0) {
            return false;
        }
        final double factor = (double) dot / length;
        if (factor < 1 - darkness || factor > 1) {
            return false;
        }
        for (int i = 0; i < COMPONENTS; i++) {
            if (Math.abs(component(px, i) - factor * component(color, i)) > tolerance) {
                return false;
            }
        }
        return true;
    }

    private static int component(final int px, final int index) {
        return px >>> index * Byte.SIZE & BYTE_MASK;
    }

    // Grows the set pixels by a margin along one axis. Two passes, one per axis, grow a square around each pixel.
    private static boolean[] dilate(final boolean[] src, final int w, final int h, final int margin,
                                    final boolean alongX) {
        final boolean[] out = new boolean[src.length];
        final int lines = alongX ? h : w;
        final int length = alongX ? w : h;
        final int step = alongX ? 1 : w;
        final int lineStep = alongX ? w : 1;
        final int far = length + margin + 1;
        for (int line = 0; line < lines; line++) {
            final int base = line * lineStep;
            int last = -far;
            for (int i = 0; i < length; i++) {
                if (src[base + i * step]) {
                    last = i;
                }
                if (i - last <= margin) {
                    out[base + i * step] = true;
                }
            }
            int next = far * 2;
            for (int i = length - 1; i >= 0; i--) {
                if (src[base + i * step]) {
                    next = i;
                }
                if (next - i <= margin) {
                    out[base + i * step] = true;
                }
            }
        }
        return out;
    }

    private static int maxComponentDiff(final int px1, final int px2) {
        int max = 0;
        for (int i = 0; i < COMPONENTS; i++) {
            final int diff = Math.abs(component(px1, i) - component(px2, i));
            max = Math.max(max, diff);
        }
        return max;
    }
}
