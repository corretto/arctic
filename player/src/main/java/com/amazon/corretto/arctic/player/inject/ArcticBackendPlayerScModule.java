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
package com.amazon.corretto.arctic.player.inject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.amazon.corretto.arctic.common.backend.ArcticImageSaver;
import com.amazon.corretto.arctic.common.backend.impl.JavaImageIoSaver;
import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.player.backend.ImageComparator;
import com.amazon.corretto.arctic.player.backend.impl.HashImageComparator;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.PixelImageComparator;
import com.amazon.corretto.arctic.player.backend.pixel.check.ClusterPixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.check.ConfidencePixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.check.DimensionPixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.check.EdgeTolerantPixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.check.FuzzyPixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.check.HashPixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.check.LoadRecordedPixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.check.StrictPixelCheck;
import com.google.inject.multibindings.Multibinder;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.inject.name.Names.named;

/**
 * Module to load screen check comparison related classes. This can mean a simple hash comparator or a complex pixel
 * comparator with different PixelChecks
 */
public final class ArcticBackendPlayerScModule extends ArcticModule {
    private static final Logger log = LoggerFactory.getLogger(ArcticBackendPlayerScModule.class);

    private static final Map<String, Consumer<ArcticBackendPlayerScModule>> COMPARATORS = Map.of(
            HashImageComparator.NAME, ArcticBackendPlayerScModule::configureHash,
            PixelImageComparator.NAME, ArcticBackendPlayerScModule::configurePixel
    );

    private static final Map<PixelCheck.Type, Class<? extends PixelCheck>> PIXEL_CHECKS = Map.of(
            HashPixelCheck.NAME, HashPixelCheck.class,
            LoadRecordedPixelCheck.NAME, LoadRecordedPixelCheck.class,
            DimensionPixelCheck.NAME, DimensionPixelCheck.class,
            ConfidencePixelCheck.NAME, ConfidencePixelCheck.class,
            StrictPixelCheck.NAME, StrictPixelCheck.class,
            ClusterPixelCheck.NAME, ClusterPixelCheck.class,
            FuzzyPixelCheck.NAME, FuzzyPixelCheck.class,
            EdgeTolerantPixelCheck.NAME, EdgeTolerantPixelCheck.class
    );

    private static final Map<PixelCheck.Type, Consumer<ArcticBackendPlayerScModule>> PIXEL_CHECK_CONFIGURATION = Map.of(
            ConfidencePixelCheck.NAME, ArcticBackendPlayerScModule::configureConfidence,
            FuzzyPixelCheck.NAME, ArcticBackendPlayerScModule::configureFuzzy,
            ClusterPixelCheck.NAME, ArcticBackendPlayerScModule::configureCluster,
            EdgeTolerantPixelCheck.NAME, ArcticBackendPlayerScModule::configureEdge
    );

    private static final int DEFAULT_MAX_VERTICAL_SHIFT = 8;
    private static final int DEFAULT_SHADOW_TOLERANCE = 3;

    private boolean fuzzyToleranceBound = false;

    /**
     * Creates a new instance of the module for a provided configuration.
     * @param config Configuration used to create the module.
     */
    public ArcticBackendPlayerScModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {
        check(InjectionKeys.BACKEND_SC_COMPARATOR, COMPARATORS.keySet());
        final String nameProvider = getConfig().getString(InjectionKeys.BACKEND_SC_COMPARATOR);
        if (!COMPARATORS.containsKey(nameProvider)) {
            fail(InjectionKeys.BACKEND_SC_COMPARATOR, COMPARATORS.keySet());
        }
        COMPARATORS.get(nameProvider).accept(this);
    }

    private void configureHash() {
        bind(ImageComparator.class).to(HashImageComparator.class);
    }

    private void configurePixel() {
        configureChecks();
        configureAlignment();
        configureScreenCheckSave();
        bind(ImageComparator.class).to(PixelImageComparator.class);
        bindFromConfig(Boolean.class, InjectionKeys.BACKEND_SC_PIXEL_HINT_FAST, "true for fast and ugly");
        bindFromConfig(Boolean.class, InjectionKeys.BACKEND_SC_PIXEL_HINT_MASK, "true to generate masks");
    }

    private void configureChecks() {
        final List<String> checks = getConfig().getList(String.class, InjectionKeys.BACKEND_SC_PIXEL_CHECKS);
        final Multibinder<PixelCheck> checksMultiBinder =
                Multibinder.newSetBinder(binder(), PixelCheck.class, named(InjectionKeys.BACKEND_SC_PIXEL_CHECKS));

        checks.stream()
                .distinct()
                .filter(PixelCheck.Type::isType)
                .map(PixelCheck.Type::fromString)
                .filter(PIXEL_CHECKS::containsKey)
                .map(PIXEL_CHECKS::get)
                .forEach(it -> checksMultiBinder.addBinding().to(it));

        checks.stream()
                .filter(PixelCheck.Type::isType)
                .map(PixelCheck.Type::fromString)
                .filter(PIXEL_CHECKS::containsKey)
                .filter(PIXEL_CHECK_CONFIGURATION::containsKey)
                .map(PIXEL_CHECK_CONFIGURATION::get)
                .forEach(it -> it.accept(this));
        log.info("PixelChecks: {}", String.join(",", checks));

    }

    private void configureCluster() {
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_SC_PIXEL_CLUSTER_9,
                "number of pixels in 3x3");
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_SC_PIXEL_CLUSTER_25,
                "number of pixels in 5x5");
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_SC_PIXEL_CLUSTER_MAX_DRAW,
                "limit number of pixels to draw for clusters");
        bindFromConfig(Boolean.class, InjectionKeys.BACKEND_SC_PIXEL_CLUSTER_SOURCE_FUZZY,
                "whether to use fuzzy as source for cluster check");
    }

    private void configureEdge() {
        configureFuzzy();
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_SC_PIXEL_EDGE_GRADIENT,
                "minimum color change between neighbour pixels to be considered an edge");
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_SC_PIXEL_EDGE_RADIUS,
                "how many pixels around a failure to look for edges and matching colors");
        final float maxExcused = getConfig().getFloat(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_EXCUSED);
        if (maxExcused < 0 || maxExcused > 1.0) {
            fail(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_EXCUSED, Collections.singletonList("0 <= x <= 1"));
        }
        bind(Float.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_EXCUSED))
                .toInstance(maxExcused);
        // Optional, so configuration files written before this key existed keep working. Without it a blended
        // color has to be between the two colors it is a blend of.
        final float overshoot = getConfig().getFloat(InjectionKeys.BACKEND_SC_PIXEL_EDGE_BLEND_OVERSHOOT, 0f);
        if (overshoot < 0) {
            fail(InjectionKeys.BACKEND_SC_PIXEL_EDGE_BLEND_OVERSHOOT, "a number >= 0");
        }
        bind(Float.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_BLEND_OVERSHOOT))
                .toInstance(overshoot);
        // Also optional. Without them, no group of pixels is moved and no failing pixel is accepted.
        final int patchMaxShift = getConfig().getInt(InjectionKeys.BACKEND_SC_PIXEL_EDGE_PATCH_MAX_SHIFT, 0);
        if (patchMaxShift < 0) {
            fail(InjectionKeys.BACKEND_SC_PIXEL_EDGE_PATCH_MAX_SHIFT, "a number of pixels >= 0");
        }
        bind(Integer.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_PATCH_MAX_SHIFT))
                .toInstance(patchMaxShift);
        final int maxUnexcused = getConfig().getInt(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_UNEXCUSED, 0);
        if (maxUnexcused < 0) {
            fail(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_UNEXCUSED, "a number of pixels >= 0");
        }
        bind(Integer.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_UNEXCUSED))
                .toInstance(maxUnexcused);
    }

    private void configureFuzzy() {
        if (fuzzyToleranceBound) {
            return;
        }
        fuzzyToleranceBound = true;
        final int globalTolerance = getConfig().getInt(InjectionKeys.BACKEND_SC_PIXEL_FUZZY_TOLERANCE);
        if (globalTolerance < 0 || globalTolerance > FuzzyPixelCheck.MAX_TOLERANCE_VALUE) {
            fail(InjectionKeys.BACKEND_SC_PIXEL_CONFIDENCE_MIN, "a number between 0 and 255");
        }
        bind(Integer.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_FUZZY_TOLERANCE))
                .toInstance(globalTolerance);
    }

    private void configureConfidence() {
        final float globalConfidence = getConfig().getFloat(InjectionKeys.BACKEND_SC_PIXEL_CONFIDENCE_MIN);
        if (globalConfidence < 0 || globalConfidence > 1.0) {
            fail(InjectionKeys.BACKEND_SC_PIXEL_CONFIDENCE_MIN, Collections.singletonList("0 <= x <= 1"));
        }
        bind(Float.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_CONFIDENCE_MIN))
                .toInstance(globalConfidence);
        bindFromConfig(Boolean.class, InjectionKeys.BACKEND_SC_PIXEL_CHECK_SHADES, "whether to"
                + "check shaded areas");
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_SC_PIXEL_SHADE_MARGIN, "pixels around"
                + "the shaded areas not to check");
        configureBackground();
    }

    /**
     * Background keys are optional so configuration files written before the feature existed keep working. Without
     * them the background is not ignored.
     */
    private void configureBackground() {
        final int tolerance = getConfig().getInt(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_TOLERANCE, 0);
        if (tolerance < 0 || tolerance > FuzzyPixelCheck.MAX_TOLERANCE_VALUE) {
            fail(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_TOLERANCE, "a number between 0 and 255");
        }
        final int margin = getConfig().getInt(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_MARGIN, 2);
        if (margin < 0) {
            fail(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_MARGIN, "a number of pixels >= 0");
        }
        final float shadowDarkness = getConfig().getFloat(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_DARKNESS,
                0f);
        if (shadowDarkness < 0 || shadowDarkness > 1) {
            fail(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_DARKNESS, Collections.singletonList("0 <= x <= 1"));
        }
        final int shadowTolerance = getConfig().getInt(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_TOLERANCE,
                DEFAULT_SHADOW_TOLERANCE);
        if (shadowTolerance < 0 || shadowTolerance > FuzzyPixelCheck.MAX_TOLERANCE_VALUE) {
            fail(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_TOLERANCE, "a number between 0 and 255");
        }
        bind(Boolean.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_ENABLED))
                .toInstance(getConfig().getBoolean(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_ENABLED, false));
        bind(Float.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_DARKNESS))
                .toInstance(shadowDarkness);
        bind(Integer.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_TOLERANCE))
                .toInstance(shadowTolerance);
        bind(Integer.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_TOLERANCE))
                .toInstance(tolerance);
        bind(Integer.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_MARGIN))
                .toInstance(margin);
        configureBoundary(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_TOP);
        configureBoundary(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_BOTTOM);
        configureBoundary(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_LEFT);
        configureBoundary(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_RIGHT);
    }

    /**
     * Alignment keys are optional so configuration files written before the feature existed keep working. Without
     * them images are not aligned.
     */
    private void configureAlignment() {
        final int maxShift = getConfig().getInt(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_MAX_SHIFT,
                DEFAULT_MAX_VERTICAL_SHIFT);
        if (maxShift < 0) {
            fail(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_MAX_SHIFT, "a number of pixels >= 0");
        }
        bind(Boolean.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_ENABLED))
                .toInstance(getConfig().getBoolean(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_ENABLED, false));
        bind(Integer.class).annotatedWith(named(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_MAX_SHIFT))
                .toInstance(maxShift);
    }

    private void configureBoundary(final String key) {
        final int width = getConfig().getInt(key, 0);
        if (width < 0) {
            fail(key, "a number of pixels >= 0");
        }
        bind(Integer.class).annotatedWith(named(key)).toInstance(width);
    }

    private void configureScreenCheckSave() {
        bindFromConfig(Boolean.class, InjectionKeys.BACKEND_SC_PIXEL_SAVE, Arrays.asList(true, false));
        bindFromConfig(Boolean.class, InjectionKeys.BACKEND_SC_PIXEL_SAVE_CLEAR, Arrays.asList(true, false));
        bindFromConfig(String.class, InjectionKeys.BACKEND_SC_PIXEL_SAVE_FOLDER, "a valid output folder");
        check(InjectionKeys.BACKEND_SC_PIXEL_SAVE_FORMAT, "a valid java ImageIO format");
        check(InjectionKeys.BACKEND_SC_PIXEL_SAVE_EXTENSION, "a valid java ImageIO format extension");
        final String imageFormat = getConfig().getString(InjectionKeys.BACKEND_SC_PIXEL_SAVE_FORMAT);
        final String imageExtension = getConfig().getString(InjectionKeys.BACKEND_SC_PIXEL_SAVE_EXTENSION);
        bind(ArcticImageSaver.class).toInstance(new JavaImageIoSaver(imageFormat, imageExtension));
    }
}
