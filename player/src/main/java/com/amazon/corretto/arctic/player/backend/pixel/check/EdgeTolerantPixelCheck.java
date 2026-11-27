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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.amazon.corretto.arctic.common.util.Pair;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * A pixel check that tolerates small movements of edges, such as glyphs rendered with a different font or window
 * borders drawn with slightly different rounding, while remaining strict on flat areas.
 *
 * Only pixels that failed the strict check are examined. Each of them is classified as follows:
 * <ul>
 *     <li>If the colors only deviate within the fuzzy tolerance, the pixel is fine.</li>
 *     <li>If there is no edge (in either image) within the configured radius, the pixel is in a flat area and the
 *     failure is not tolerated.</li>
 *     <li>If it is close to an edge, the failure is tolerated only if the color of the current pixel can be found
 *     within the radius in the recorded image, and the color of the recorded pixel can be found within the radius in
 *     the current image. A glyph that moves a pixel passes, while a glyph that disappears or changes color fails.
 *     A color is also found if it is a blend of two colors in that neighborhood, which is what anti-aliasing of a
 *     curve that is drawn slightly differently produces. The blend can go a configurable distance past either of the
 *     two colors, to allow for an outline that is drawn crisper or darker.</li>
 * </ul>
 * If a patch shift is configured, the failures that are still left are then grouped. A group of failures that sit
 * close to each other is a region of the screen that may have been drawn somewhere else than where it was recorded,
 * for example a window that the test places on its own, which does not move with the rest of the test. For each region
 * the offset, up to the configured number of pixels in each direction, is searched at which the recorded image
 * explains it, and its pixels are then judged against the recorded image at that offset with the rules above. A pixel
 * is explained only if its color is found in the recorded image at the offset, and the recorded color is found in the
 * current image at the opposite offset. Changed or missing content has no offset that explains it.
 *
 * The check passes if no more than the configured number of failures is left and the tolerated failures do not
 * exceed a ratio of the whole image.
 */
public final class EdgeTolerantPixelCheck implements PixelCheck {
    public static final Type NAME = Type.EDGE;
    public static final int PRIORITY = 45;

    private static final int BYTE_MASK = 0xFF;
    private static final int COMPONENTS = 3;
    private static final byte NOT_COMPUTED = 0;
    private static final byte NOT_EDGE = 1;
    private static final byte EDGE = 2;
    // Groups with fewer failures than this are never realigned, as a handful of pixels fits almost anywhere.
    private static final int MIN_REGION_PIXELS = 8;
    // The offset of a region is chosen looking at this many of its pixels, spread over the region.
    private static final int MAX_REGION_SAMPLES = 256;

    private final int tolerance;
    private final int gradient;
    private final int radius;
    private final float maxExcusedRatio;
    private final float blendOvershoot;
    private final int patchMaxShift;
    private final int maxUnexcused;
    private final HintMaskHelper hintMaskHelper;

    /**
     * Creates a new instance of the check. Usually called by the dependency injection software.
     * @param tolerance How much each of the RGB components can deviate for two colors to be considered the same.
     * @param gradient Minimum difference between two neighbour pixels, in any component, to consider an edge.
     * @param radius How many pixels around a failure we look at to find edges and matching colors.
     * @param maxExcusedRatio Maximum ratio of pixels in the image that can be tolerated by this check.
     * @param blendOvershoot How far past either of the two colors a blend can be, as a fraction of the distance
     *                       between them. 0 means the blend has to be between the two colors.
     * @param patchMaxShift How many pixels, in each direction, a group of failures can be moved to find its content in
     *                      the recorded image. 0 disables the search.
     * @param maxUnexcused How many failures can be left after everything else, for example the isolated pixels of a
     *                     window corner that is drawn differently.
     * @param hintMaskHelper Used to generate the hint and mask images.
     */
    @Inject
    public EdgeTolerantPixelCheck(@Named(InjectionKeys.BACKEND_SC_PIXEL_FUZZY_TOLERANCE) final int tolerance,
                                  @Named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_GRADIENT) final int gradient,
                                  @Named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_RADIUS) final int radius,
                                  @Named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_EXCUSED) final float maxExcusedRatio,
                                  @Named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_BLEND_OVERSHOOT)
                                  final float blendOvershoot,
                                  @Named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_PATCH_MAX_SHIFT)
                                  final int patchMaxShift,
                                  @Named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_UNEXCUSED) final int maxUnexcused,
                                  final HintMaskHelper hintMaskHelper) {
        this.tolerance = tolerance;
        this.gradient = gradient;
        this.radius = radius;
        this.maxExcusedRatio = maxExcusedRatio;
        this.blendOvershoot = blendOvershoot;
        this.patchMaxShift = patchMaxShift;
        this.maxUnexcused = maxUnexcused;
        this.hintMaskHelper = hintMaskHelper;
    }

    @Override
    public boolean isSufficient() {
        return true;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public Type getType() {
        return NAME;
    }

    @Override
    public List<Type> getDependencyChecks() {
        return List.of(Type.RECORDED, Type.DIMENSION, Type.STRICT);
    }

    @Override
    public boolean doCheck(final PixelCheckResult result, final Path alternative) {
        final PixelCheckSummary strict = result.getStrictSummary();
        if (strict.getTotalFailedPixels() == 0) {
            return true;
        }
        return isAcceptable(result.getAlignedImage(alternative), result.getSavedImage(alternative),
                strict.getFailedPixels());
    }

    @Override
    public void doDiff(final Path alternative, final ArcticDiffImages diffImages) {
        final BufferedImage curr = diffImages.getAlignedImage(alternative);
        final BufferedImage saved = diffImages.getImages(alternative).get(Type.RECORDED);
        final BufferedImage strictImage = diffImages.getImages(alternative).get(Type.STRICT);
        final Set<Integer>[] failures = diffImages.getStrictSummary(alternative).getFailedPixels();

        final Analysis analysis = analyze(curr, saved, failures, false);
        final BufferedImage edge = new BufferedImage(curr.getWidth(), curr.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final Set<Integer>[] unexcused = analysis.unexcused.getFailedPixels();
        for (int x = 0; x < unexcused.length; x++) {
            if (unexcused[x] != null) {
                for (final int y : unexcused[x]) {
                    edge.setRGB(x, y, strictImage.getRGB(x, y));
                }
            }
        }

        final Pair<BufferedImage, Optional<BufferedImage>> images = hintMaskHelper.drawImages(unexcused,
                curr.getWidth(), curr.getHeight());
        diffImages.getImages(alternative).put(Type.EDGE, edge);
        diffImages.getHints(alternative).put(Type.EDGE_HINT, images.getLeft());
        images.getRight().ifPresent(it -> diffImages.getHints(alternative).put(Type.EDGE_MASK, it));

        diffImages.addProperty(alternative, NAME, "passed", String.valueOf(analysis.isAcceptable(maxExcused(curr))));
        diffImages.addProperty(alternative, NAME, "failed", analysis.unexcused.getTotalFailedPixels(), "px");
        diffImages.addProperty(alternative, NAME, "maxUnexcused", maxUnexcused, "px");
        diffImages.addProperty(alternative, NAME, "excused", analysis.excused, "px");
        diffImages.addProperty(alternative, NAME, "patched", analysis.patched, "px");
        diffImages.addProperty(alternative, NAME, "maxExcused", maxExcused(curr), "px");
        diffImages.addProperty(alternative, NAME, "ratio", analysis.unexcused.getConfidence());
    }

    boolean isAcceptable(final BufferedImage current, final BufferedImage saved, final Set<Integer>[] failures) {
        return analyze(current, saved, failures, true).isAcceptable(maxExcused(current));
    }

    private long maxExcused(final BufferedImage image) {
        return (long) (maxExcusedRatio * image.getWidth() * image.getHeight());
    }

    private Analysis analyze(final BufferedImage current, final BufferedImage saved, final Set<Integer>[] failures,
                             final boolean stopOnFailure) {
        final PixelCheckSummary unexcused = new PixelCheckSummary(current.getWidth(), current.getHeight());
        final EdgeMap currentEdges = new EdgeMap(current, gradient);
        final EdgeMap savedEdges = new EdgeMap(saved, gradient);
        final long maxExcused = maxExcused(current);
        final Analysis analysis = new Analysis(unexcused, maxUnexcused);
        // Failures that the patch search can still excuse later, which bounds how many we can afford to collect.
        final long recoverable = patchMaxShift > 0 ? maxExcused : 0;

        for (int x = 0; x < failures.length; x++) {
            if (failures[x] == null) {
                continue;
            }
            for (final int y : failures[x]) {
                final int curRgb = current.getRGB(x, y);
                final int savedRgb = saved.getRGB(x, y);
                if (colorsMatch(curRgb, savedRgb)) {
                    continue;
                }
                if (isNearEdge(x, y, currentEdges, savedEdges) && hasNearOrBlend(saved, x, y, curRgb)
                        && hasNearOrBlend(current, x, y, savedRgb)) {
                    analysis.excused++;
                } else {
                    unexcused.addFailure(x, y);
                }
                if (stopOnFailure && (analysis.excused > maxExcused
                        || analysis.unexcused.getTotalFailedPixels() > maxUnexcused + recoverable)) {
                    return analysis;
                }
            }
        }
        if (patchMaxShift > 0 && analysis.unexcused.getTotalFailedPixels() > maxUnexcused) {
            realignRegions(current, saved, currentEdges, savedEdges, analysis, maxExcused);
        }
        return analysis;
    }

    // Groups the failures that are left. Each group is a region of the screen that could have been drawn somewhere else
    // than where it was recorded, for example a window that the test places on its own. The region is compared again
    // with the recorded image moved by one offset, using the same rules as for the rest of the image.
    private void realignRegions(final BufferedImage current, final BufferedImage saved, final EdgeMap currentEdges,
                                final EdgeMap savedEdges, final Analysis analysis, final long maxExcused) {
        final int width = current.getWidth();
        final int height = current.getHeight();
        final boolean[] pending = new boolean[width * height];
        final Set<Integer>[] failed = analysis.unexcused.getFailedPixels();
        for (int x = 0; x < failed.length; x++) {
            if (failed[x] != null) {
                for (final int y : failed[x]) {
                    pending[y * width + x] = true;
                }
            }
        }

        final PixelCheckSummary remaining = new PixelCheckSummary(width, height);
        final Deque<Integer> stack = new ArrayDeque<>();
        int budget = maxUnexcused;
        for (int x = 0; x < failed.length; x++) {
            if (failed[x] == null) {
                continue;
            }
            for (final int y : failed[x]) {
                if (!pending[y * width + x]) {
                    continue;
                }
                final List<Integer> group = new ArrayList<>();
                pending[y * width + x] = false;
                stack.push(y * width + x);
                while (!stack.isEmpty()) {
                    final int index = stack.pop();
                    group.add(index);
                    final int px = index % width;
                    final int py = index / width;
                    for (int j = Math.max(0, py - patchMaxShift); j <= Math.min(height - 1, py + patchMaxShift); j++) {
                        for (int i = Math.max(0, px - patchMaxShift); i <= Math.min(width - 1, px + patchMaxShift);
                             i++) {
                            if (pending[j * width + i]) {
                                pending[j * width + i] = false;
                                stack.push(j * width + i);
                            }
                        }
                    }
                }
                final int[] offset = group.size() >= MIN_REGION_PIXELS && analysis.excused + group.size() <= maxExcused
                        ? findOffset(current, saved, currentEdges, savedEdges, group) : null;
                final List<Integer> explained = new ArrayList<>();
                final List<Integer> unexplained = new ArrayList<>();
                for (final int index : group) {
                    if (offset != null && isMoved(current, saved, currentEdges, savedEdges, index % width,
                            index / width, offset[0], offset[1])) {
                        explained.add(index);
                    } else {
                        unexplained.add(index);
                    }
                }
                // A region is only moved as a whole. If what the offset does not explain does not fit in what is
                // allowed to be left over, the offset is not the reason for the difference, and nothing is excused.
                if (offset != null && unexplained.size() <= budget) {
                    budget -= unexplained.size();
                    analysis.excused += explained.size();
                    analysis.patched += explained.size();
                    unexplained.forEach(it -> remaining.addFailure(it % width, it / width));
                } else {
                    group.forEach(it -> remaining.addFailure(it % width, it / width));
                }
            }
        }
        analysis.unexcused = remaining;
    }

    // Finds the offset at which the recorded image has the content of the region, or null if there is none. Only a
    // sample of the pixels is used to choose, so a large region does not cost more than a small one.
    private int[] findOffset(final BufferedImage current, final BufferedImage saved, final EdgeMap currentEdges,
                             final EdgeMap savedEdges, final List<Integer> group) {
        final int width = current.getWidth();
        final int step = Math.max(1, group.size() / MAX_REGION_SAMPLES);
        final List<Integer> samples = new ArrayList<>();
        for (int i = 0; i < group.size(); i += step) {
            samples.add(group.get(i));
        }
        int bestFailures = samples.size();
        int[] best = null;
        // Offsets are tried from the closest to zero outwards, so the smallest one wins a tie.
        for (int distance = 1; distance <= patchMaxShift; distance++) {
            for (int dy = -distance; dy <= distance; dy++) {
                for (int dx = -distance; dx <= distance; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != distance) {
                        continue;
                    }
                    int failures = 0;
                    for (final int index : samples) {
                        if (!isMoved(current, saved, currentEdges, savedEdges, index % width, index / width, dx, dy)
                                && ++failures >= bestFailures) {
                            break;
                        }
                    }
                    if (failures < bestFailures) {
                        bestFailures = failures;
                        best = new int[] {dx, dy};
                    }
                }
            }
        }
        return best;
    }

    // Whether the pixel is explained by the content having moved by the offset. The current pixel has to be found in
    // the recorded image at the offset, and the recorded pixel has to be found in the current image at the opposite
    // offset. Without the second half, a changed or missing part of the screen would be explained by any offset that
    // happens to land on the same flat color in the recorded image.
    private boolean isMoved(final BufferedImage current, final BufferedImage saved, final EdgeMap currentEdges,
                            final EdgeMap savedEdges, final int x, final int y, final int dx, final int dy) {
        final int sx = x + dx;
        final int sy = y + dy;
        return isTolerated(current, saved, currentEdges, savedEdges, x, y, dx, dy)
                && sx >= 0 && sy >= 0 && sx < saved.getWidth() && sy < saved.getHeight()
                && isTolerated(saved, current, savedEdges, currentEdges, sx, sy, -dx, -dy);
    }

    // Whether the pixel of the current image is tolerated against the recorded image moved by the offset.
    private boolean isTolerated(final BufferedImage current, final BufferedImage saved, final EdgeMap currentEdges,
                                final EdgeMap savedEdges, final int x, final int y, final int dx, final int dy) {
        final int sx = x + dx;
        final int sy = y + dy;
        if (sx < 0 || sy < 0 || sx >= saved.getWidth() || sy >= saved.getHeight()) {
            return false;
        }
        final int curRgb = current.getRGB(x, y);
        final int savedRgb = saved.getRGB(sx, sy);
        return colorsMatch(curRgb, savedRgb)
                || isNearEdge(x, y, currentEdges, savedEdges, dx, dy) && hasNearOrBlend(saved, sx, sy, curRgb)
                && hasNearOrBlend(current, x, y, savedRgb);
    }

    private boolean isNearEdge(final int x, final int y, final EdgeMap first, final EdgeMap second) {
        return isNearEdge(x, y, first, second, 0, 0);
    }

    // Whether there is an edge within the radius of the pixel in the first image, or within the radius of the pixel
    // that is the offset away in the second.
    private boolean isNearEdge(final int x, final int y, final EdgeMap first, final EdgeMap second, final int dx,
                               final int dy) {
        final int maxX = Math.min(first.width - 1, x + radius);
        final int maxY = Math.min(first.height - 1, y + radius);
        for (int i = Math.max(0, x - radius); i <= maxX; i++) {
            for (int j = Math.max(0, y - radius); j <= maxY; j++) {
                if (first.isEdge(i, j)) {
                    return true;
                }
                final int si = i + dx;
                final int sj = j + dy;
                if (si >= 0 && sj >= 0 && si < second.width && sj < second.height && second.isEdge(si, sj)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Whether the color is found within the radius, or is a blend of two colors found within the radius.
    private boolean hasNearOrBlend(final BufferedImage image, final int x, final int y, final int rgb) {
        final int minX = Math.max(0, x - radius);
        final int minY = Math.max(0, y - radius);
        final int maxX = Math.min(image.getWidth() - 1, x + radius);
        final int maxY = Math.min(image.getHeight() - 1, y + radius);
        final int[] colors = new int[(maxX - minX + 1) * (maxY - minY + 1)];
        int count = 0;
        for (int i = minX; i <= maxX; i++) {
            for (int j = minY; j <= maxY; j++) {
                final int color = image.getRGB(i, j);
                if (colorsMatch(color, rgb)) {
                    return true;
                }
                colors[count++] = color;
            }
        }
        for (int first = 0; first < count; first++) {
            for (int second = first + 1; second < count; second++) {
                if (isBlend(colors[first], colors[second], rgb)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Whether the color is, within the tolerance, on the straight line in RGB space between the two colors. The line
    // is extended by the overshoot past each of them.
    private boolean isBlend(final int start, final int end, final int color) {
        long dot = 0;
        long length = 0;
        for (int i = 0; i < COMPONENTS; i++) {
            final int span = component(end, i) - component(start, i);
            dot += (long) (component(color, i) - component(start, i)) * span;
            length += (long) span * span;
        }
        if (length == 0) {
            return false;
        }
        final double fraction = Math.max(-blendOvershoot, Math.min(1 + blendOvershoot, (double) dot / length));
        for (int i = 0; i < COMPONENTS; i++) {
            final double expected = component(start, i) + fraction * (component(end, i) - component(start, i));
            if (Math.abs(component(color, i) - expected) > tolerance) {
                return false;
            }
        }
        return true;
    }

    private static int component(final int px, final int index) {
        return px >>> index * Byte.SIZE & BYTE_MASK;
    }

    private boolean colorsMatch(final int px1, final int px2) {
        return maxComponentDiff(px1, px2) <= tolerance;
    }

    private static int maxComponentDiff(final int px1, final int px2) {
        int max = 0;
        for (int i = 0; i < COMPONENTS; i++) {
            final int diff = Math.abs(component(px1, i) - component(px2, i));
            max = Math.max(max, diff);
        }
        return max;
    }

    /**
     * Result of classifying the strict failures.
     */
    private static final class Analysis {
        private final int maxUnexcused;
        private PixelCheckSummary unexcused;
        private int excused = 0;
        private int patched = 0;

        private Analysis(final PixelCheckSummary unexcused, final int maxUnexcused) {
            this.unexcused = unexcused;
            this.maxUnexcused = maxUnexcused;
        }

        private boolean isAcceptable(final long maxExcused) {
            return unexcused.getTotalFailedPixels() <= maxUnexcused && excused <= maxExcused;
        }
    }

    /**
     * Lazily computed map of the pixels of an image that are next to a sharp color change.
     */
    private static final class EdgeMap {
        private final BufferedImage image;
        private final int threshold;
        private final int width;
        private final int height;
        private final byte[] cache;

        private EdgeMap(final BufferedImage image, final int threshold) {
            this.image = image;
            this.threshold = threshold;
            this.width = image.getWidth();
            this.height = image.getHeight();
            this.cache = new byte[width * height];
        }

        private boolean isEdge(final int x, final int y) {
            final int index = y * width + x;
            if (cache[index] == NOT_COMPUTED) {
                cache[index] = compute(x, y) ? EDGE : NOT_EDGE;
            }
            return cache[index] == EDGE;
        }

        private boolean compute(final int x, final int y) {
            final int rgb = image.getRGB(x, y);
            return (x + 1 < width && maxComponentDiff(rgb, image.getRGB(x + 1, y)) > threshold)
                    || (y + 1 < height && maxComponentDiff(rgb, image.getRGB(x, y + 1)) > threshold);
        }
    }
}
