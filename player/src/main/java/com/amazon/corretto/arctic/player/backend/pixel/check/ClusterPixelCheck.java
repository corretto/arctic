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
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.util.Pair;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A PixelCheck that will allow failures as long as they are not in a cluster. This means isolated pixels that are
 * different are considered ok. This is based on the fact that a human will not detect (or consider relevant) a single
 * pixel or two pixel if the surrounding pixels still match.
 *
 * This PixelCheck looks around each failure, scanning a 3x3 area and a 5x5 one. How many pixels need to be on each area
 * so it is consider a failure is configurable.
 *
 * When generating a diff image, there is a bailout once enough clusters are detected, defined by a constructor
 * parameter.
 */
public final class ClusterPixelCheck implements PixelCheck {
    public static final Type NAME = Type.CLUSTER;
    public static final int PRIORITY = 60;


    private static final Logger log = LoggerFactory.getLogger(ClusterPixelCheck.class);

    private final int maxCluster9;
    private final int maxCluster25;
    private final int maxDrawn;
    private final boolean fuzzySource;
    private final HintMaskHelper hintMaskHelper;

    /**
     * Creates a new instance. Usually called by the dependency injector.
     * @param maxCluster9 How many failures can we accept in a 3x3 area around the failure we are checking.
     * @param maxCluster25 How many failures can we accept in a 5x5 area around the failure we are checking.
     * @param maxDrawn How many clusters we draw in the diff image before giving up.
     * @param fuzzySource whether to use the fuzzy check as source instead of the strict one.
     * @param hintMaskHelper Helper class to generate the hints/masks
     */
    @Inject
    public ClusterPixelCheck(@Named(InjectionKeys.BACKEND_SC_PIXEL_CLUSTER_9) final int maxCluster9,
                             @Named(InjectionKeys.BACKEND_SC_PIXEL_CLUSTER_25) final int maxCluster25,
                             @Named(InjectionKeys.BACKEND_SC_PIXEL_CLUSTER_MAX_DRAW) final int maxDrawn,
                             @Named(InjectionKeys.BACKEND_SC_PIXEL_CLUSTER_SOURCE_FUZZY) final boolean fuzzySource,
                             final HintMaskHelper hintMaskHelper) {
        this.maxCluster9 = maxCluster9;
        this.maxCluster25 = maxCluster25;
        this.maxDrawn = maxDrawn;
        this.fuzzySource = fuzzySource;
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
        PixelCheck.Type source = fuzzySource ? Type.FUZZY : Type.STRICT;
        return List.of(Type.RECORDED, Type.DIMENSION, source);
    }

    @Override
    public boolean doCheck(final PixelCheckResult result, final Path alternative) {
        log.trace("doCheck");
        PixelCheckSummary summary = fuzzySource ? result.getFuzzySummary() : result.getStrictSummary();
        return (summary.getTotalFailedPixels() == 0 || !hasClusters(summary.getFailedPixels()));
    }

    @Override
    public void doDiff(final Path alternative, final ArcticDiffImages diffImages) {
        final BufferedImage strict = diffImages.getImages(alternative).get(Type.STRICT);
        final BufferedImage cluster = new BufferedImage(strict.getWidth(), strict.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        PixelCheckSummary sourceSummary = fuzzySource
                ? diffImages.getFuzzySummary(alternative)
                : diffImages.getStrictSummary(alternative);
        final PixelCheckSummary summary = getClusters(sourceSummary.getFailedPixels(), cluster, strict);

        Pair<BufferedImage, Optional<BufferedImage>> images = hintMaskHelper.drawImages(summary.getFailedPixels(),
                strict.getWidth(), strict.getHeight());

        diffImages.getImages(alternative).put(Type.CLUSTER, cluster);
        diffImages.getHints(alternative).put(Type.CLUSTER_HINT, images.getLeft());
        images.getRight().ifPresent(it -> diffImages.getHints(alternative).put(Type.CLUSTER_MASK, it));

        boolean passed = (summary.getTotalFailedPixels() == 0);
        diffImages.addProperty(alternative, NAME, "passed", String.valueOf(passed));
        diffImages.addProperty(alternative, NAME, "failed", summary.getTotalFailedPixels(), "px");
        diffImages.addProperty(alternative, NAME, "ratio", summary.getConfidence());
    }

    boolean hasClusters(final Set<Integer>[] failedPixels) {
        for (int i = 0; i < failedPixels.length; i++) {
            if (failedPixels[i] != null) {
                final int x = i;
                final boolean foundCluster = failedPixels[i].stream()
                        .anyMatch(it -> isCluster(x, it, failedPixels));
                if (foundCluster) {
                    return true;
                }
            }
        }
        return false;
    }

    PixelCheckSummary getClusters(final Set<Integer>[] failedPixels, final BufferedImage clusterImage,
                               final BufferedImage strictImage) {
        PixelCheckSummary summary = new PixelCheckSummary(clusterImage.getWidth(), clusterImage.getHeight());
        @SuppressWarnings("unchecked")
        Set<Integer>[] clusterFailures = new Set[failedPixels.length];
        IntStream.range(0, failedPixels.length)
                .filter(x -> failedPixels[x] != null && !failedPixels[x].isEmpty())
                .parallel()
                .forEach(x -> failedPixels[x].stream()
                        .filter(y -> isCluster(x, y, failedPixels))
                        .forEach(y -> {
                            summary.addFailure(x, y);
                            clusterImage.setRGB(x, y, strictImage.getRGB(x, y));
                        })
                );
        return summary;
    }

    boolean isCluster(final int x, final int y, final Set<Integer>[] failedPixels) {
        return isCluster(x, y, failedPixels, 1, maxCluster9) || isCluster(x, y, failedPixels, 2, maxCluster25);
    }

    boolean isCluster(final int x, final int y, final Set<Integer>[] failures, final int mode, final int maxCount) {
        int count = 0;
        for (int i = x - mode; i <= x + mode; i++) {
            if (i >= 0 && i < failures.length && failures[i] != null && !failures[i].isEmpty()) {
                for (int j = y - mode; j <= y + mode; j++) {
                    if (failures[i].contains(j)) {
                        if (++count > maxCount) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
