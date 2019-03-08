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

package com.amazon.corretto.arctic.player.model;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.TestId;

import static com.amazon.corretto.arctic.player.model.TestStatusCode.NO_RECORDING;

/**
 * Represent a test that is being executed right now.
 */
public final class ArcticRunningTest {
    private final TestId testId;
    private final ArcticTest recording;
    private final ArcticPlayerTestStatus status;

    /**
     * Creates a new instance for a test. This assumes the test execution will be starting.
     * @param recording Recording for the test that is going to be replayed.
     */
    public ArcticRunningTest(final ArcticTest recording) {
        this.testId = new TestId(recording.getTestName(), recording.getTestCase());
        this.recording = recording;
        this.status = new ArcticPlayerTestStatus();
    }

    /**
     * Creates a new instance for a test that has no recording.
     * @param id Id of the test that is being executed
     */
    public ArcticRunningTest(final TestId id) {
        this.testId = id;
        this.recording = null;
        this.status = new ArcticPlayerTestStatus(NO_RECORDING);
    }

    /**
     * Creates a new instance for a test. This assumes the test execution will be starting even if there is no
     * recording.
     * @param id ID for the test for which we don't have a recording
     * @return Instance of ArcticRunningTest that wraps the recording.
     */
    public static ArcticRunningTest noRecording(final TestId id) {
            return new ArcticRunningTest(id);
    }

    /**
     * Creates a new instance for a test. This assumes the test execution will be starting.
     * @param recording Recording of the test we are running
     * @return Instance of ArcticRunningTest that wraps the recording.
     */
    public static ArcticRunningTest of(final ArcticTest recording) {
        return new ArcticRunningTest(recording);
    }

    /**
     * Get the id that identifies the test that is running.
     * @return A testId to identify the test.
     */
    public TestId getTestId() {
        return testId;
    }

    /**
     * The recording of the test that is running.
     * @return Recording of the test.
     */
    public ArcticTest getRecording() {
        return recording;
    }

    /**
     * Get the execat status during the playback of the test.
     * @return A {@link TestStatusCode} wrapped in {@link ArcticPlayerTestStatus#}.
     */
    public ArcticPlayerTestStatus getStatus() {
        return status;
    }
}
