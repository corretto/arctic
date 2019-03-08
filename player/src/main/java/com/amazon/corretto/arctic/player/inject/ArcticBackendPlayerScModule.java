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
            FuzzyPixelCheck.NAME, FuzzyPixelCheck.class
    );

    private static final Map<PixelCheck.Type, Consumer<ArcticBackendPlayerScModule>> PIXEL_CHECK_CONFIGURATION = Map.of(
            ConfidencePixelCheck.NAME, ArcticBackendPlayerScModule::configureConfidence,
            FuzzyPixelCheck.NAME, ArcticBackendPlayerScModule::configureFuzzy,
            ClusterPixelCheck.NAME, ArcticBackendPlayerScModule::configureCluster
    );

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

    private void configureFuzzy() {
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
