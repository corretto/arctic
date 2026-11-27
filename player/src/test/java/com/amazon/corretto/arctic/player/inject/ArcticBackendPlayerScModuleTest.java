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

import java.util.Set;

import com.amazon.corretto.arctic.common.repository.TestLoadRepository;
import com.amazon.corretto.arctic.common.repository.TestRepository;
import com.amazon.corretto.arctic.common.repository.TestSaveRepository;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.VerticalAligner;
import com.amazon.corretto.arctic.player.backend.pixel.check.EdgeTolerantPixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.check.StrictPixelCheck;
import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.junit.jupiter.api.Test;

import static com.google.inject.name.Names.named;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ArcticBackendPlayerScModuleTest {

    private static Injector injector(final String checks) {
        return Guice.createInjector(new ArcticBackendPlayerScModule(config(checks)), new AbstractModule() {
            @Override
            protected void configure() {
                bind(TestRepository.class).toInstance(mock(TestRepository.class));
                bind(TestLoadRepository.class).toInstance(mock(TestLoadRepository.class));
                bind(TestSaveRepository.class).toInstance(mock(TestSaveRepository.class));
            }
        });
    }

    private static Configuration config(final String checks) {
        final BaseConfiguration config = new BaseConfiguration();
        config.setListDelimiterHandler(new DefaultListDelimiterHandler(','));
        config.setProperty(InjectionKeys.BACKEND_SC_COMPARATOR, "pixel");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_CHECKS, checks);
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_SAVE, "false");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_SAVE_CLEAR, "false");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_SAVE_FOLDER, "failures");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_SAVE_FORMAT, "png");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_SAVE_EXTENSION, ".png");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_HINT_FAST, "false");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_HINT_MASK, "false");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_CONFIDENCE_MIN, "0.65");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_CHECK_SHADES, "false");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_SHADE_MARGIN, "3");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_FUZZY_TOLERANCE, "8");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_EDGE_GRADIENT, "24");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_EDGE_RADIUS, "2");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_EXCUSED, "0.10");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_EDGE_BLEND_OVERSHOOT, "0");
        return config;
    }

    @Test
    void edgeCheckIsCreatedWhenEnabledAlongsideFuzzy() {
        final Set<PixelCheck> checks = injector("recorded, dimension, strict, confidence, edge, fuzzy").getInstance(
                Key.get(new TypeLiteral<Set<PixelCheck>>() { }, named(InjectionKeys.BACKEND_SC_PIXEL_CHECKS)));

        assertEquals(6, checks.size());
        assertTrue(checks.stream().anyMatch(EdgeTolerantPixelCheck.class::isInstance));
    }

    @Test
    void edgeCheckIsCreatedWhenEnabledWithoutFuzzy() {
        final Set<PixelCheck> checks = injector("recorded, dimension, strict, confidence, edge").getInstance(
                Key.get(new TypeLiteral<Set<PixelCheck>>() { }, named(InjectionKeys.BACKEND_SC_PIXEL_CHECKS)));

        assertTrue(checks.stream().anyMatch(EdgeTolerantPixelCheck.class::isInstance));
    }

    @Test
    void shadowKeysAreOptionalAndDefaultToOff() {
        final Configuration config = config("recorded, dimension, strict, confidence");

        final Injector injector = Guice.createInjector(new ArcticBackendPlayerScModule(config),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TestRepository.class).toInstance(mock(TestRepository.class));
                        bind(TestLoadRepository.class).toInstance(mock(TestLoadRepository.class));
                        bind(TestSaveRepository.class).toInstance(mock(TestSaveRepository.class));
                    }
                });

        assertEquals(0f, injector.getInstance(Key.get(Float.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_DARKNESS))));
        assertEquals(3, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_TOLERANCE))));
    }

    @Test
    void shadowKeysAreAcceptedWhenPresent() {
        final Configuration config = config("recorded, dimension, strict, confidence");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_DARKNESS, "0.6");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_TOLERANCE, "5");

        final Injector injector = Guice.createInjector(new ArcticBackendPlayerScModule(config),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TestRepository.class).toInstance(mock(TestRepository.class));
                        bind(TestLoadRepository.class).toInstance(mock(TestLoadRepository.class));
                        bind(TestSaveRepository.class).toInstance(mock(TestSaveRepository.class));
                    }
                });

        assertEquals(0.6f, injector.getInstance(Key.get(Float.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_DARKNESS))));
        assertEquals(5, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_SHADOW_TOLERANCE))));
    }

    @Test
    void blendOvershootIsOptional() {
        final Configuration config = config("recorded, dimension, strict, confidence, edge");
        config.clearProperty(InjectionKeys.BACKEND_SC_PIXEL_EDGE_BLEND_OVERSHOOT);

        final Injector injector = Guice.createInjector(new ArcticBackendPlayerScModule(config),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TestRepository.class).toInstance(mock(TestRepository.class));
                        bind(TestLoadRepository.class).toInstance(mock(TestLoadRepository.class));
                        bind(TestSaveRepository.class).toInstance(mock(TestSaveRepository.class));
                    }
                });

        assertEquals(0f, injector.getInstance(Key.get(Float.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_BLEND_OVERSHOOT))));
    }

    @Test
    void patchAndUnexcusedKeysAreOptionalAndDefaultToOff() {
        final Configuration config = config("recorded, dimension, strict, confidence, edge");

        final Injector injector = Guice.createInjector(new ArcticBackendPlayerScModule(config),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TestRepository.class).toInstance(mock(TestRepository.class));
                        bind(TestLoadRepository.class).toInstance(mock(TestLoadRepository.class));
                        bind(TestSaveRepository.class).toInstance(mock(TestSaveRepository.class));
                    }
                });

        assertEquals(0, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_PATCH_MAX_SHIFT))));
        assertEquals(0, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_UNEXCUSED))));
    }

    @Test
    void patchAndUnexcusedKeysAreAcceptedWhenPresent() {
        final Configuration config = config("recorded, dimension, strict, confidence, edge");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_EDGE_PATCH_MAX_SHIFT, "3");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_UNEXCUSED, "40");

        final Injector injector = Guice.createInjector(new ArcticBackendPlayerScModule(config),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TestRepository.class).toInstance(mock(TestRepository.class));
                        bind(TestLoadRepository.class).toInstance(mock(TestLoadRepository.class));
                        bind(TestSaveRepository.class).toInstance(mock(TestSaveRepository.class));
                    }
                });

        assertEquals(3, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_PATCH_MAX_SHIFT))));
        assertEquals(40, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_UNEXCUSED))));
    }

    @Test
    void negativePatchShiftIsRejected() {
        final Configuration config = config("recorded, dimension, strict, confidence, edge");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_EDGE_PATCH_MAX_SHIFT, "-1");

        assertThrows(CreationException.class, () -> Guice.createInjector(new ArcticBackendPlayerScModule(config)));
    }

    @Test
    void negativeMaxUnexcusedIsRejected() {
        final Configuration config = config("recorded, dimension, strict, confidence, edge");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_EDGE_MAX_UNEXCUSED, "-1");

        assertThrows(CreationException.class, () -> Guice.createInjector(new ArcticBackendPlayerScModule(config)));
    }

    @Test
    void backgroundKeysAreOptional() {
        final Set<PixelCheck> checks = injector("recorded, dimension, strict, confidence").getInstance(
                Key.get(new TypeLiteral<Set<PixelCheck>>() { }, named(InjectionKeys.BACKEND_SC_PIXEL_CHECKS)));

        assertTrue(checks.stream().anyMatch(StrictPixelCheck.class::isInstance));
    }

    @Test
    void backgroundKeysAreAcceptedWhenPresent() {
        final Configuration config = config("recorded, dimension, strict, confidence");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_ENABLED, "true");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_TOLERANCE, "4");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_MARGIN, "3");

        final Injector injector = Guice.createInjector(new ArcticBackendPlayerScModule(config),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TestRepository.class).toInstance(mock(TestRepository.class));
                        bind(TestLoadRepository.class).toInstance(mock(TestLoadRepository.class));
                        bind(TestSaveRepository.class).toInstance(mock(TestSaveRepository.class));
                    }
                });

        assertEquals(3, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_MARGIN))));
        assertTrue(injector.getInstance(Key.get(Boolean.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_ENABLED))));
    }

    @Test
    void boundaryKeysAreAcceptedWhenPresent() {
        final Configuration config = config("recorded, dimension, strict, confidence");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_TOP, "9");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_RIGHT, "4");

        final Injector injector = Guice.createInjector(new ArcticBackendPlayerScModule(config),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TestRepository.class).toInstance(mock(TestRepository.class));
                        bind(TestLoadRepository.class).toInstance(mock(TestLoadRepository.class));
                        bind(TestSaveRepository.class).toInstance(mock(TestSaveRepository.class));
                    }
                });

        assertEquals(9, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_TOP))));
        assertEquals(4, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_RIGHT))));
        assertEquals(0, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_LEFT))));
    }

    @Test
    void negativeBoundaryIsRejected() {
        final Configuration config = config("recorded, dimension, strict, confidence");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_BOUNDARY_LEFT, "-1");

        assertThrows(CreationException.class, () -> Guice.createInjector(new ArcticBackendPlayerScModule(config)));
    }

    @Test
    void alignmentIsOffByDefaultWhenKeysAreMissing() {
        final Injector injector = injector("recorded, dimension, strict, confidence");

        assertFalse(injector.getInstance(Key.get(Boolean.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_ENABLED))));
        assertEquals(8, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_MAX_SHIFT))));
    }

    @Test
    void alignmentKeysAreAcceptedWhenPresent() {
        final Configuration config = config("recorded, dimension, strict, confidence");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_ENABLED, "true");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_MAX_SHIFT, "5");

        final Injector injector = Guice.createInjector(new ArcticBackendPlayerScModule(config),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TestRepository.class).toInstance(mock(TestRepository.class));
                        bind(TestLoadRepository.class).toInstance(mock(TestLoadRepository.class));
                        bind(TestSaveRepository.class).toInstance(mock(TestSaveRepository.class));
                    }
                });

        assertTrue(injector.getInstance(Key.get(Boolean.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_ENABLED))));
        assertEquals(5, injector.getInstance(Key.get(Integer.class,
                named(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_MAX_SHIFT))));
        assertTrue(injector.getInstance(VerticalAligner.class).isEnabled());
    }

    @Test
    void negativeAlignmentShiftIsRejected() {
        final Configuration config = config("recorded, dimension, strict, confidence");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_ALIGN_VERTICAL_MAX_SHIFT, "-1");

        assertThrows(CreationException.class, () -> Guice.createInjector(new ArcticBackendPlayerScModule(config)));
    }

    @Test
    void negativeBackgroundMarginIsRejected() {
        final Configuration config = config("recorded, dimension, strict, confidence");
        config.setProperty(InjectionKeys.BACKEND_SC_PIXEL_BACKGROUND_MARGIN, "-1");

        assertThrows(CreationException.class, () -> Guice.createInjector(new ArcticBackendPlayerScModule(config)));
    }
}
