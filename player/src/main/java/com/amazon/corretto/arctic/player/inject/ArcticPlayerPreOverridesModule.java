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

import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.common.model.ArcticTestMouseOffsets;
import com.amazon.corretto.arctic.common.model.ArcticTestTimings;
import com.amazon.corretto.arctic.common.model.ArcticTestTruncations;
import org.apache.commons.configuration2.Configuration;

/**
 * This module injects all classes related to overrides. Overrides are applied by
 * {@link com.amazon.corretto.arctic.player.preprocessing.impl.OverridesPreProcessor} and change how the test is
 * reproduced. Overrides are set at the player level, but if needed, the values can be saved back to the recording.
 * There are several types of overrides:
 * - Reproduction: It changes which types of events of the recording we want to post. This can be used to avoid posting
 *   events we don't really need, like mouse movement events if the test can just do clicks.
 * - Timings: How much we wait for the test and each of the individual events. This can be used to speed up the test
 *   after recording, removing unintended long pauses in the recording.
 * - Truncations: Sometimes there are garbage events at the beginning or the end of the test, depending on how they
 *   were recorded. Truncations allow us to ignore those events
 * - Mouse offsets: Sometimes we need to shift the mouse events a few pixels due to changes in the platform. This allows
 *   us to apply an offset to the x and y coordinates of all mouse events.
 */
public final class ArcticPlayerPreOverridesModule extends ArcticModule {

    /**
     * Creates a new instance of this module. This should only be needed if the overrides pre-processor is used.
     * @param config Our configuration.
     */
    public ArcticPlayerPreOverridesModule(final Configuration config) {
        super(config);
    }

    /**
     * Configure the current module. For each type of override, we inject two values. One representing whether the
     * specific overrides should apply or not and other with the values for the overrides.
     */
    public void configure() {
        bindFromConfig(Boolean.class, InjectionKeys.PRE_OVERRIDES_REPRODUCTION, Arrays.asList(true, false));
        bindFromConfig(Integer.class, InjectionKeys.PRE_OVERRIDES_REPRODUCTION_MODE,
                "a valid reproduction mask");

        configureTruncations();
        configureTimings();
        configureMouseOffsets();
    }

    private void configureTimings() {
        bindFromConfig(Boolean.class, InjectionKeys.PRE_OVERRIDES_TIMINGS, Arrays.asList(true, false));
        final ArcticTestTimings timings = new ArcticTestTimings();
        check(InjectionKeys.PRE_OVERRIDES_TIMINGS_START, "a valid long");
        check(InjectionKeys.PRE_OVERRIDES_TIMINGS_SC, "a valid long");
        check(InjectionKeys.PRE_OVERRIDES_TIMINGS_MIN, "a valid long");
        check(InjectionKeys.PRE_OVERRIDES_TIMINGS_MAX, "a valid long");

        timings.setStartDelayMs(getConfig().getLong(InjectionKeys.PRE_OVERRIDES_TIMINGS_START));
        timings.setScDelayMs(getConfig().getLong(InjectionKeys.PRE_OVERRIDES_TIMINGS_SC));
        timings.setMinWaitNs(getConfig().getLong(InjectionKeys.PRE_OVERRIDES_TIMINGS_MIN));
        timings.setMaxWaitNs(getConfig().getLong(InjectionKeys.PRE_OVERRIDES_TIMINGS_MAX));

        bind(ArcticTestTimings.class).toInstance(timings);
    }

    private void configureTruncations() {
        bindFromConfig(Boolean.class, InjectionKeys.PRE_OVERRIDES_TRUNCATIONS, Arrays.asList(true, false));
        final ArcticTestTruncations truncations = new ArcticTestTruncations();
        check(InjectionKeys.PRE_OVERRIDES_TRUNCATIONS_MOUSE_START, "a valid integer");
        check(InjectionKeys.PRE_OVERRIDES_TRUNCATIONS_MOUSE_END, "a valid integer");
        check(InjectionKeys.PRE_OVERRIDES_TRUNCATIONS_KB_START, "a valid integer");
        check(InjectionKeys.PRE_OVERRIDES_TRUNCATIONS_KB_END, "a valid integer");
        truncations.setMouseStart(getConfig().getInt(InjectionKeys.PRE_OVERRIDES_TRUNCATIONS_MOUSE_END));
        truncations.setMouseStart(getConfig().getInt(InjectionKeys.PRE_OVERRIDES_TRUNCATIONS_MOUSE_START));
        truncations.setKbStart(getConfig().getInt(InjectionKeys.PRE_OVERRIDES_TRUNCATIONS_KB_START));
        truncations.setKbEnd(getConfig().getInt(InjectionKeys.PRE_OVERRIDES_TRUNCATIONS_KB_END));
        bind(ArcticTestTruncations.class).toInstance(truncations);
    }

    private void configureMouseOffsets() {
        bindFromConfig(Boolean.class, InjectionKeys.PRE_OVERRIDES_MOUSE_OFFSETS, Arrays.asList(true, false));
        final ArcticTestMouseOffsets offsets = new ArcticTestMouseOffsets();
        check(InjectionKeys.PRE_OVERRIDES_MOUSE_OFFSETS_X, "a valid integer");
        check(InjectionKeys.PRE_OVERRIDES_MOUSE_OFFSETS_Y, "a valid integer");
        offsets.setX(getConfig().getInt(InjectionKeys.PRE_OVERRIDES_MOUSE_OFFSETS_X));
        offsets.setY(getConfig().getInt(InjectionKeys.PRE_OVERRIDES_MOUSE_OFFSETS_Y));
        bind(ArcticTestMouseOffsets.class).toInstance(offsets);
    }



}
