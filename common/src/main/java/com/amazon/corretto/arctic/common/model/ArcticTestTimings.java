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
package com.amazon.corretto.arctic.common.model;

import lombok.Data;

/**
 * Data class to store the values of the different timings of a test.
 * TODO: Move to record when migrated to Java 17
 */
@Data
public class ArcticTestTimings {
    private static final long DEFAULT_START_DELAY = 300;
    private static final long DEFAULT_SC_DELAY = 50;
    private static final long DEFAULT_MIN_WAIT = 1000000;
    private static final long DEFAULT_MAX_WAIT = -1;

    /**
     * Represents how much time we should wait before we start the test. This give time for the test to be rendered on
     * the screen.
     */
    private long startDelayMs = DEFAULT_START_DELAY;

    /**
     * How much we should wait before we take a screenshot during a screen check. Although during normal reproduction is
     * not a problem, as we optimize the test, reproducing fewer event and waiting less time, we may need to have a
     * higher wait to let the screen rendering catch up before we capture the screenshot.
     */
    private long scDelayMs = DEFAULT_SC_DELAY;

    /**
     * Indicates how much time is not worth to wait because sleeping for less that this amount is not really worth to
     * attempt to sleep the thread.
     * TODO: Update tests and rename to minWaitMs.
     */
    private long minWaitNs = DEFAULT_MIN_WAIT;

    /**
     * Represents how much at most we will wait before we send the next event. A value of -1 is treated equivalent to
     * {@link Long#MAX_VALUE}. Although the name suggests the value should be in nanoseconds, the actual value is
     * interpreted as milliseconds.
     * TODO: Update tests and rename to maxWaitMs.
     */
    private long maxWaitNs = DEFAULT_MAX_WAIT;
}
