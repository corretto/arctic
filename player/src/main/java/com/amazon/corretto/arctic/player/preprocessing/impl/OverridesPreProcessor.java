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

package com.amazon.corretto.arctic.player.preprocessing.impl;

import java.util.Set;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.model.ArcticTestMouseOffsets;
import com.amazon.corretto.arctic.common.model.ArcticTestTimings;
import com.amazon.corretto.arctic.common.model.ArcticTestTruncations;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.preprocessing.ArcticPlayerPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This preprocessor is able to replace some test properties with new values define by the player.
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
public final class OverridesPreProcessor implements ArcticPlayerPreProcessor {
    public static final String NAME = "overrides";

    private static final Logger log = LoggerFactory.getLogger(OverridesPreProcessor.class);
    private static final int PRIORITY = 30;
    private static final int OVERRIDE_THRESHOLD = -1;
    private static final int MAX_WAIT_OVERRIDE_THRESHOLD = -2;

    private final boolean reproductionOverride;
    private final int reproductionMode;
    private final boolean timingsOverride;
    private final ArcticTestTimings newTimings;
    private final boolean truncateOverride;
    private final ArcticTestTruncations overriddenTruncations;
    private final boolean mouseOffsetsOverride;
    private final ArcticTestMouseOffsets overridenMouseOffsets;

    /**
     * Creates a new instance of the preprocessor. Called by the dependency injection framework.
     * @param reproductionOverride Whether to apply an override to the reproduction mode.
     * @param reproductionMode The new reproduction mode to use if the override applies.
     * @param timingsOverride Whether to apply an override to the timings.
     * @param overriddenTimings The new timings to use if the override applies.
     * @param truncateOverride Whether to apply an override to the truncations.
     * @param overriddenTruncations The new truncations to use if the override applies.
     * @param mouseOffsetsOverride Whether to apply an override to the mouse offsets.
     * @param overridenMouseOffsets The new mouse offsets to use if the override applies.
     */
    @Inject
    public OverridesPreProcessor(final @Named(InjectionKeys.PRE_OVERRIDES_REPRODUCTION) boolean reproductionOverride,
                                 final @Named(InjectionKeys.PRE_OVERRIDES_REPRODUCTION_MODE) int reproductionMode,
                                 final @Named(InjectionKeys.PRE_OVERRIDES_TIMINGS) boolean timingsOverride,
                                 final ArcticTestTimings overriddenTimings,
                                 final @Named(InjectionKeys.PRE_OVERRIDES_TRUNCATIONS) boolean truncateOverride,
                                 final ArcticTestTruncations overriddenTruncations,
                                 final @Named(InjectionKeys.PRE_OVERRIDES_MOUSE_OFFSETS) boolean mouseOffsetsOverride,
                                 final ArcticTestMouseOffsets overridenMouseOffsets) {

        this.reproductionOverride = reproductionOverride;
        this.reproductionMode = reproductionMode;
        this.timingsOverride = timingsOverride;
        this.newTimings = overriddenTimings;
        this.truncateOverride = truncateOverride;
        this.overriddenTruncations = overriddenTruncations;
        this.mouseOffsetsOverride = mouseOffsetsOverride;
        this.overridenMouseOffsets = overridenMouseOffsets;
    }

    @Override
    public boolean preProcess(final ArcticRunningTest test) {
        if (reproductionOverride & reproductionMode > 0) {
            // Reproduction mode override will not enable an event Subtype that is disabled by default
            test.getRecording().setPreferredPlayMode(test.getRecording().getPreferredPlayMode() & reproductionMode);
        }
        if (timingsOverride) {
            overrideTimings(test.getRecording().getTimings());
        }
        if (truncateOverride) {
            overrideTruncations(test.getRecording().getTruncations());
        }
        if (mouseOffsetsOverride) {
            overrideMouseOffsets(test.getRecording().getMouseOffsets());
        }
        return true;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Set<TestStatusCode> getRegisteredStatuses() {
        return Set.of(TestStatusCode.STARTING);
    }

    private void overrideTimings(final ArcticTestTimings timings) {
        overrideTiming(MAX_WAIT_OVERRIDE_THRESHOLD, newTimings.getMaxWaitNs(), timings.getMaxWaitNs(),
                timings::setMaxWaitNs, false);
        overrideTiming(OVERRIDE_THRESHOLD, newTimings.getMinWaitNs(), timings.getMinWaitNs(),
                timings::setMinWaitNs, true);
        overrideTiming(OVERRIDE_THRESHOLD, newTimings.getScDelayMs(), timings.getScDelayMs(),
                timings::setScDelayMs, true);
        overrideTiming(OVERRIDE_THRESHOLD, newTimings.getStartDelayMs(), timings.getStartDelayMs(),
                timings::setStartDelayMs, true);
    }

    private void overrideTruncations(final ArcticTestTruncations truncations) {
        overrideTruncation(OVERRIDE_THRESHOLD, overriddenTruncations.getMouseStart(), truncations::setMouseStart);
        overrideTruncation(OVERRIDE_THRESHOLD, overriddenTruncations.getMouseEnd(), truncations::setMouseEnd);
        overrideTruncation(OVERRIDE_THRESHOLD, overriddenTruncations.getKbStart(), truncations::setKbStart);
        overrideTruncation(OVERRIDE_THRESHOLD, overriddenTruncations.getKbEnd(), truncations::setKbEnd);
    }

    private void overrideTiming(final long threshold, final long value, final long recordedValue,
                                final Consumer<Long> setter, final boolean allowSlowdown) {
        if (value > threshold && (allowSlowdown || (recordedValue < 0 || value < recordedValue))) {
            log.debug("Applied override {}", value);
            setter.accept(value);
        }
    }

    private void overrideTruncation(final int threshold, final int value, final Consumer<Integer> setter) {
        if (value > threshold) {
            setter.accept(value);
        }
    }

    private void overrideMouseOffsets(final ArcticTestMouseOffsets mouseOffsets) {
        mouseOffsets.setX(overridenMouseOffsets.getX());
        mouseOffsets.setY(overridenMouseOffsets.getY());
    }
}
