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
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import com.amazon.corretto.arctic.common.util.Pair;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
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
    private final HintMaskHelper hintMaskHelper;

    /**
     * Creates a new instance. Usually called by the dependency injector software.
     * @param checkShades Whether to include the areas that are covered by shades,
     * @param shadeMargin Add an extra in pixels to the area that defines a shade. This solves some issues in pixels
     *                    that make the shade border in some systems.
     * @param hintMaskHelper Used to generate the hint/mask of the image.
     */
    @Inject
    public StrictPixelCheck(@Named(InjectionKeys.BACKEND_SC_PIXEL_CHECK_SHADES) final boolean checkShades,
                            @Named(InjectionKeys.BACKEND_SC_PIXEL_SHADE_MARGIN) final int shadeMargin,
                            final HintMaskHelper hintMaskHelper) {
        this.checkShades = checkShades;
        this.shadeMargin = shadeMargin;
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
        final PixelCheckSummary summary = compareImages(result.getCurrentImage(),
                result.getSavedImage(alternative), result.getShades(), null);
        result.setStrictSummary(summary);
        return summary.getTotalFailedPixels() == 0;
    }

    @Override
    public void doDiff(final Path alternative, final ArcticDiffImages diffImages) {
        final BufferedImage current = diffImages.getCurrentImage();
        final BufferedImage saved = diffImages.getImages(alternative).get(Type.RECORDED);
        final BufferedImage strict = new BufferedImage(current.getWidth(), current.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        final PixelCheckSummary summary = compareImages(current, saved, diffImages.getShades(), strict);
        diffImages.setStrictSummary(alternative, summary);

        Pair<BufferedImage, Optional<BufferedImage>> images = hintMaskHelper.drawImages(summary.getFailedPixels(),
                strict.getWidth(), strict.getHeight());

        diffImages.getImages(alternative).put(Type.STRICT, strict);
        diffImages.getHints(alternative).put(Type.HINT, images.getLeft());
        images.getRight().ifPresent(it -> diffImages.getHints(alternative).put(Type.MASK, it));
        boolean passed = summary.getTotalFailedPixels() == 0;
        diffImages.addProperty(alternative, NAME, "passed", String.valueOf(passed));
        diffImages.addProperty(alternative, NAME, "failed", summary.getTotalFailedPixels(), "px");
        diffImages.addProperty(alternative, NAME, "ratio", summary.getConfidence());
    }

    @Override
    public List<Type> getDependencyChecks() {
        return List.of(Type.RECORDED, Type.DIMENSION);
    }


    private PixelCheckSummary compareImages(final BufferedImage current, final BufferedImage saved,
                                            final List<ScreenArea> shades, final BufferedImage diff) {
        final PixelCheckSummary summary = new PixelCheckSummary(current.getWidth(), current.getHeight());
        IntStream.range(0, current.getWidth()).parallel().forEach(x -> {
            for (int y = 0; y < current.getHeight(); y++) {
                if ((checkShades || !isShaded(x, y, shades))
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
