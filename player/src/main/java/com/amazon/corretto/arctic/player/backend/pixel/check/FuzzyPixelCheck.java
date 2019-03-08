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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.util.Pair;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;

/**
 * A pixel check that considers fuzziness. For each pixel that does not match in both images, the pixel is split into
 * the RGB component. For each component, we check whether they deviate in more than the fuzziness value. This means
 * at fuzziness 255, every single image will be considered equal.
 */
public final class FuzzyPixelCheck implements PixelCheck {
    public static final int MAX_TOLERANCE_VALUE = 255;
    public static final Type NAME = Type.FUZZY;
    public static final int PRIORITY = 50;

    private static final int ALPHA_MASK = 0xFF000000;

    private final int tolerance;
    private final HintMaskHelper hintMaskHelper;

    /**
     * Creates a new instance of the check. Usually called by the dependency injection software.
     * @param tolerance How much each of the component can deviate.
     * @param hintMaskHelper Used to generate the hint and mask images.
     */
    @Inject
    public FuzzyPixelCheck(@Named(InjectionKeys.BACKEND_SC_PIXEL_FUZZY_TOLERANCE) final int tolerance,
                           final HintMaskHelper hintMaskHelper) {
        this.tolerance = tolerance;
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
        return List.of(Type.RECORDED, Type.DIMENSION, Type.CONFIDENCE);
    }

    @Override
    public boolean doCheck(final PixelCheckResult result, final Path alternative) {
        if (result.getStrictSummary().getTotalFailedPixels() == 0) {
            result.setFuzzySummary(result.getStrictSummary());
            return true;
        }
        final PixelCheckSummary summary = fuzzyCheck(result.getStrictSummary().getFailedPixels(),
                result.getCurrentImage(), result.getSavedImage(alternative));
        log.trace("FuzzyCheck: {}", summary.getTotalFailedPixels() == 0);
        result.setFuzzySummary(summary);
        return (summary.getTotalFailedPixels() == 0);
    }

    @Override
    public void doDiff(final Path alternative, final ArcticDiffImages diffImages) {
        final BufferedImage curr = diffImages.getCurrentImage();
        final BufferedImage recorded = diffImages.getImages(alternative).get(Type.RECORDED);
        final BufferedImage fuzzy = new BufferedImage(curr.getWidth(), curr.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final Set<Integer>[] failures = diffImages.getStrictSummary(alternative).getFailedPixels();
        PixelCheck.PixelCheckSummary summary = new PixelCheckSummary(curr.getWidth(), curr.getHeight());

        int maxPixelDifference = 0;
        int maxComponentDifference = 0;
        int nonEqualPixels = 0;
        int totalDeviation = 0;
        int failedPixelTotalDeviation = 0;

        for (int x = 0; x < failures.length; x++) {
            if (failures[x] != null && failures[x].size() != 0) {
                for (final int y : failures[x]) {
                    final PixelResult pixelDifference = getPixelResult(curr.getRGB(x, y), recorded.getRGB(x, y));
                    maxPixelDifference = Math.max(maxPixelDifference, pixelDifference.globalDiff);
                    maxComponentDifference = Math.max(maxComponentDifference, pixelDifference.componentDiff);
                    totalDeviation += pixelDifference.globalDiff;
                    nonEqualPixels++;
                    if ((pixelDifference.pixelColor & ALPHA_MASK) != 0) {
                        fuzzy.setRGB(x, y, pixelDifference.pixelColor);
                        summary.addFailure(x, y);
                        failedPixelTotalDeviation += pixelDifference.globalDiff;
                    }
                }
            }
        }
        int totalPixels = fuzzy.getHeight() * fuzzy.getWidth();
        float avgDeviation = (totalDeviation / (float) totalPixels) / 3;
        float avgNonEqDeviation = nonEqualPixels != 0 ? (totalDeviation / (float) (nonEqualPixels)) / 3 : 0;
        float avgFailDeviation = summary.getTotalFailedPixels() != 0
                ? (failedPixelTotalDeviation / (float) summary.getTotalFailedPixels()) / 3
                : 0;

        diffImages.setFuzzySummary(alternative, summary);
        Pair<BufferedImage, Optional<BufferedImage>> images = hintMaskHelper.drawImages(summary.getFailedPixels(),
                fuzzy.getWidth(), fuzzy.getHeight());
        diffImages.getImages(alternative).put(Type.FUZZY, fuzzy);

        diffImages.getHints(alternative).put(Type.FUZZY_HINT, images.getLeft());
        images.getRight().ifPresent(it -> diffImages.getHints(alternative).put(Type.FUZZY_MASK, it));
        boolean passed = summary.getTotalFailedPixels() == 0;
        diffImages.addProperty(alternative, NAME, "passed", String.valueOf(passed));
        diffImages.addProperty(alternative, NAME, "failed", summary.getTotalFailedPixels(), "px");
        diffImages.addProperty(alternative, NAME, "ratio", summary.getConfidence());
        diffImages.addProperty(alternative, NAME, "maxComponentDiff", maxComponentDifference);
        diffImages.addProperty(alternative, NAME, "maxCombinedDiff", maxPixelDifference);
        diffImages.addProperty(alternative, NAME, "totalDeviation", totalDeviation);
        diffImages.addProperty(alternative, NAME, "failedTotalDeviation", failedPixelTotalDeviation);
        diffImages.addProperty(alternative, NAME, "averageDeviation", avgDeviation);
        diffImages.addProperty(alternative, NAME, "averageNonEqualDeviation", avgNonEqDeviation);
        diffImages.addProperty(alternative, NAME, "averageFailDeviation", avgFailDeviation);
    }

    PixelCheck.PixelCheckSummary fuzzyCheck(final Set<Integer>[] failedPixels, final BufferedImage current,
                                            final BufferedImage saved) {
        PixelCheck.PixelCheckSummary summary = new PixelCheckSummary(current.getWidth(), current.getHeight());
        int maxPixelDifference = 0;
        int maxComponentDifference = 0;

        for (int x = 0; x < saved.getWidth(); x++) {
            if (failedPixels[x] != null) {
                for (final int y : failedPixels[x]) {
                    final PixelResult pixelDifference = getPixelResult(current.getRGB(x, y), saved.getRGB(x, y));
                    maxPixelDifference = Math.max(maxPixelDifference, pixelDifference.globalDiff);
                    maxComponentDifference = Math.max(maxComponentDifference, pixelDifference.componentDiff);
                    if ((pixelDifference.pixelColor & ALPHA_MASK) != 0) {
                        summary.addFailure(x, y);
                    }
                }
            }
        }
        log.trace("Max pixel diff: {}", maxPixelDifference);
        log.trace("Max component diff: {}", maxComponentDifference);
        return summary;
    }

    private static final int NEUTRAL_COMPONENT = 0x80;
    private static final int NEUTRAL_COLOR = 0x808080;
    private static final int BYTE_MASK = 0xFF;
    PixelResult getPixelResult(final int px1, final int px2) {
        final PixelResult result = new PixelResult();
        result.pixelColor = 0;
        for (int i = 0; i < 3; i++) {
            final int comp1 = px1 >>> i * 8 & BYTE_MASK;
            final int comp2 = px2 >>> i * 8 & BYTE_MASK;
            final int componentDiff = comp1 - comp2;
            result.globalDiff += Math.abs(componentDiff);
            result.componentDiff = Math.max(result.componentDiff, Math.abs(componentDiff));
            final int componentColor = NEUTRAL_COMPONENT + ((tolerance >= Math.abs(componentDiff) ? 0 : componentDiff
                    - (componentDiff > 0 ? tolerance : -tolerance)) >> 1);
            result.pixelColor |= componentColor << i * 8;
        }
        if (result.pixelColor != NEUTRAL_COLOR) {
            result.pixelColor |= ALPHA_MASK;
        }
        return result;
    }

    private static class PixelResult {
        private int pixelColor = 0;
        private int globalDiff = 0;
        private int componentDiff = 0;
    }
}
