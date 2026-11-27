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
import java.util.stream.IntStream;

import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import com.amazon.corretto.arctic.common.util.Pair;
import com.amazon.corretto.arctic.player.backend.pixel.BackgroundMask;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pixel check that compares every pixel and expects them to be exactly the same. In practice, this comparator relies
 * on the results of {@link ConfidencePixelCheck}. This PixelCheck is useful though to generate the diff image between
 * current and recorded image.
 */
public final class StrictPixelCheck implements PixelCheck {
    public static final Type NAME = Type.STRICT;

    private static final Logger log = LoggerFactory.getLogger(LoadRecordedPixelCheck.class);
    private static final int PRIORITY = 30;
    private static final int ALPHA_MASK = 0xFF000000;
    private final boolean checkShades;
    private final int shadeMargin;
    private final boolean ignoreBackground;
    private final int backgroundTolerance;
    private final int backgroundMargin;
    private final float shadowDarkness;
    private final int shadowTolerance;
    private final int boundaryTop;
    private final int boundaryBottom;
    private final int boundaryLeft;
    private final int boundaryRight;
    private final HintMaskHelper hintMaskHelper;

    /**
     * Creates a new instance. Usually called by the dependency injector software.
     * @param checkShades Whether to include the areas that are covered by shades,
     * @param shadeMargin Add an extra in pixels to the area that defines a shade. This solves some issues in pixels
     *                    that make the shade border in some systems.
     * @param ignoreBackground Whether to ignore the pixels that were workbench background in the recorded image.
     * @param backgroundTolerance How much each RGB component of a recorded pixel can deviate from the workbench color
     *                            to be considered background.
     * @param backgroundMargin Extra pixels around the background that are also ignored.
     * @param shadowDarkness How dark a drop shadow over the background can be, as a fraction of the workbench color,
     *                       to be ignored as well. 0 disables it.
     * @param shadowTolerance How much each RGB component of a pixel can deviate from the darkened workbench color to
     *                        be considered shadow.
     * @param boundaryTop Pixels ignored along the top border of the image.
     * @param boundaryBottom Pixels ignored along the bottom border of the image.
     * @param boundaryLeft Pixels ignored along the left border of the image.
     * @param boundaryRight Pixels ignored along the right border of the image.
     * @param hintMaskHelper Used to generate the hint/mask of the image.
     */
    @Inject
    public StrictPixelCheck(@Named(InjectionKeys.BACKEND_SC_PIXEL_CHECK_SHADES) final boolean checkShades,
                            @Named(InjectionKeys.BACKEND_SC_PIXEL_SHADE_MARGIN) final int shadeMargin,
                            @Named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_ENABLED) final boolean ignoreBackground,
                            @Named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_TOLERANCE) final int backgroundTolerance,
                            @Named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_MARGIN) final int backgroundMargin,
                            @Named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_DARKNESS)
                            final float shadowDarkness,
                            @Named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_TOLERANCE)
                            final int shadowTolerance,
                            @Named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_TOP) final int boundaryTop,
                            @Named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_BOTTOM) final int boundaryBottom,
                            @Named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_LEFT) final int boundaryLeft,
                            @Named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_RIGHT) final int boundaryRight,
                            final HintMaskHelper hintMaskHelper) {
        this.checkShades = checkShades;
        this.shadeMargin = shadeMargin;
        this.ignoreBackground = ignoreBackground;
        this.backgroundTolerance = backgroundTolerance;
        this.backgroundMargin = backgroundMargin;
        this.shadowDarkness = shadowDarkness;
        this.shadowTolerance = shadowTolerance;
        this.boundaryTop = boundaryTop;
        this.boundaryBottom = boundaryBottom;
        this.boundaryLeft = boundaryLeft;
        this.boundaryRight = boundaryRight;
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
    public boolean doCheck(final PixelCheckResult result, final Path alternative) {
        log.trace("doCheck");
        final BufferedImage saved = result.getSavedImage(alternative);
        final PixelCheckSummary summary = compareImages(result.getAlignedImage(alternative), saved,
                result.getShades(), createMask(saved, result.getWorkbenchColor()), null);
        result.setStrictSummary(summary);
        return summary.getTotalFailedPixels() == 0;
    }

    @Override
    public void doDiff(final Path alternative, final ArcticDiffImages diffImages) {
        final BufferedImage current = diffImages.getAlignedImage(alternative);
        final BufferedImage saved = diffImages.getImages(alternative).get(Type.RECORDED);
        final BufferedImage strict = new BufferedImage(current.getWidth(), current.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        final BackgroundMask mask = createMask(saved, diffImages.getWorkbenchColor());
        final PixelCheckSummary summary = compareImages(current, saved, diffImages.getShades(), mask, strict);
        diffImages.setStrictSummary(alternative, summary);

        Pair<BufferedImage, Optional<BufferedImage>> images = hintMaskHelper.drawImages(summary.getFailedPixels(),
                strict.getWidth(), strict.getHeight());

        diffImages.getImages(alternative).put(Type.STRICT, strict);
        diffImages.getHints(alternative).put(Type.HINT, images.getLeft());
        images.getRight().ifPresent(it -> diffImages.getHints(alternative).put(Type.MASK, it));
        boolean passed = summary.getTotalFailedPixels() == 0;
        diffImages.addProperty(alternative, NAME, "passed", String.valueOf(passed));
        if (mask != null) {
            diffImages.addProperty(alternative, NAME, "background", mask.getCount(), "px");
        }
        diffImages.addProperty(alternative, NAME, "failed", summary.getTotalFailedPixels(), "px");
        diffImages.addProperty(alternative, NAME, "ratio", summary.getConfidence());
    }

    @Override
    public List<Type> getDependencyChecks() {
        return List.of(Type.RECORDED, Type.DIMENSION);
    }


    private BackgroundMask createMask(final BufferedImage saved, final Integer workbenchColor) {
        final boolean hasBoundary = boundaryTop > 0 || boundaryBottom > 0 || boundaryLeft > 0 || boundaryRight > 0;
        if (!ignoreBackground || workbenchColor == null && !hasBoundary) {
            return null;
        }
        return BackgroundMask.create(saved, workbenchColor, backgroundTolerance, backgroundMargin, boundaryTop,
                boundaryBottom, boundaryLeft, boundaryRight, shadowDarkness, shadowTolerance);
    }

    private PixelCheckSummary compareImages(final BufferedImage current, final BufferedImage saved,
                                            final List<ScreenArea> shades, final BackgroundMask mask,
                                            final BufferedImage diff) {
        final PixelCheckSummary summary = new PixelCheckSummary(current.getWidth(), current.getHeight());
        IntStream.range(0, current.getWidth()).parallel().forEach(x -> {
            for (int y = 0; y < current.getHeight(); y++) {
                if ((checkShades || !isShaded(x, y, shades))
                        && (mask == null || !mask.isMasked(x, y))
                        && current.getRGB(x, y) != saved.getRGB(x, y)) {
                    summary.addFailure(x, y);
                    if (diff != null) {
                        diff.setRGB(x, y, getPixelDiff(current.getRGB(x, y), saved.getRGB(x, y)));
                    }
                }
            }
        });
        return summary;
    }

    private boolean isShaded(final int x, final int y, final List<ScreenArea> shades) {
        return shades.stream().anyMatch(it -> x >= it.getX() - shadeMargin
                && x < it.getX() + it.getW() + shadeMargin
                && y >= it.getY() - shadeMargin
                && y < it.getY() + it.getH() + shadeMargin);
    }

    private static final int NEUTRAL_COMPONENT = 0x80;
    private static final int NEUTRAL_COLOR = 0x808080;
    private static final int BYTE_MASK = 0xFF;
    int getPixelDiff(final int px1, final int px2) {
        int pixelDiff = 0;
        for (int i = 0; i < 3; i++) {
            final int comp1 = px1 >>> i * 8 & BYTE_MASK;
            final int comp2 = px2 >>> i * 8 & BYTE_MASK;
            final int componentDiff = NEUTRAL_COMPONENT + ((comp1 >> 1) - (comp2 >> 1));
            pixelDiff |= componentDiff << i * 8;
        }
        if (pixelDiff != NEUTRAL_COLOR) {
            pixelDiff |= ALPHA_MASK;
        }
        return pixelDiff;
    }
}
