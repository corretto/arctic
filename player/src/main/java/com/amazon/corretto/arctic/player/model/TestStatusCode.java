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

import java.util.HashMap;
import java.util.Map;

/**
 * State machine to represent the different situations a Test can be in. There are four transition operations:
 * pass: Indicates everything went ok and we should continue
 * fail: A regular check failed. This will lead to {@link TestStatusCode#ABORTED} or {@link TestStatusCode#FAILED}
 * error: An unexpected exception happened
 * stop: Received a stop signal
 */
public enum TestStatusCode {
    /**
     * This is the initial status for the test. A test will continue in the STARTING status until it finishes all the
     * preprocessors and passes to the RUNNING status. Alternative, it can go into the ABORTED status should one
     * preprocessor mark it so
     */
    STARTING,

    /**
     * A test will pass to the RUNNING status once all the preprocessors have been executed. The test will remain in
     * the RUNNING status as long as there are events to replay. If all the events are replayed without errors, the test
     * will transition to PASSED. If an event fails, it will go to FAILED status.
     */
    RUNNING,

    /**
     * A test that have successfully played all the events in the timeline.
     */
    UNCONFIRMED,

    /**
     * A test that have successfully played all the events in the timeline and for which we have receive confirmation
     * that the test passed. A test that passed can only go into ERROR_CONFIRMED
     */
    CONFIRMED,

    /**
     * ABORTED means the test was stopped during the pre-processing phase, and no events were replayed. Only an STARTING
     * test can go into ABORTED. ABORTED tests will not replay any events, but will go into the post-processing pipeline
     */
    ABORTED,

    /**
     * These is the status for RUNNING tests that fail, so some events were replayed.
     */
    FAILED,

    /**
     * This state is reserved for uncaught exceptions.
     */
    ERROR,

    /**
     * This state is reserved for uncaught exceptions that happen after a test have passed, during post-processing.
     */
    ERROR_CONFIRMED,

    /**
     * This state can only be set manually by the controller.
     */
    STOPPED,

    /**
     * Recording for this test is missing.
     */
    NO_RECORDING,

    /**
     * Recording for this test is missing, but we got confirmation it passed.
     */
    NO_RECORDING_OK,

    /**
     * Recording for this test is missing, and we got an error during confirmation.
     */
    NO_RECORDING_FAIL;

    public TestStatusCode pass() {
            return PASS_TRANSITIONS.getOrDefault(this, ERROR);
    }

    /**
     * A check failed, and the test needs to represent that.
     * If the failure happened during the pre-processing, transition to {@link TestStatusCode#ABORTED}
     * @return The new status after transitioning
     */
    public TestStatusCode fail() {
        return FAIL_TRANSITIONS.getOrDefault(this, ERROR);
    }

    public TestStatusCode error() {
        return ERROR_TRANSITIONS.getOrDefault(this, ERROR);
    }

    public TestStatusCode stop() {
        return STOPPED;
    }

    private static final Map<TestStatusCode, TestStatusCode> PASS_TRANSITIONS = new HashMap<>();
    static {
        PASS_TRANSITIONS.put(TestStatusCode.STARTING, TestStatusCode.RUNNING);
        PASS_TRANSITIONS.put(TestStatusCode.RUNNING, TestStatusCode.UNCONFIRMED);
        PASS_TRANSITIONS.put(TestStatusCode.ABORTED, TestStatusCode.ABORTED);
        PASS_TRANSITIONS.put(TestStatusCode.UNCONFIRMED, TestStatusCode.CONFIRMED);
        PASS_TRANSITIONS.put(TestStatusCode.CONFIRMED, TestStatusCode.CONFIRMED);
        PASS_TRANSITIONS.put(TestStatusCode.FAILED, TestStatusCode.FAILED);
        PASS_TRANSITIONS.put(TestStatusCode.ERROR, TestStatusCode.ERROR);
        PASS_TRANSITIONS.put(TestStatusCode.ERROR_CONFIRMED, TestStatusCode.ERROR_CONFIRMED);
        PASS_TRANSITIONS.put(TestStatusCode.STOPPED, TestStatusCode.STOPPED);
        PASS_TRANSITIONS.put(TestStatusCode.NO_RECORDING, TestStatusCode.NO_RECORDING_OK);
        PASS_TRANSITIONS.put(TestStatusCode.NO_RECORDING_OK, TestStatusCode.NO_RECORDING_OK);
        PASS_TRANSITIONS.put(TestStatusCode.NO_RECORDING_FAIL, TestStatusCode.NO_RECORDING_FAIL);

    }

    private static final Map<TestStatusCode, TestStatusCode> FAIL_TRANSITIONS = new HashMap<>();
    static {
        FAIL_TRANSITIONS.put(TestStatusCode.STARTING, TestStatusCode.ABORTED);
        FAIL_TRANSITIONS.put(TestStatusCode.RUNNING, TestStatusCode.FAILED);
        FAIL_TRANSITIONS.put(TestStatusCode.ABORTED, TestStatusCode.ABORTED);
        FAIL_TRANSITIONS.put(TestStatusCode.UNCONFIRMED, TestStatusCode.FAILED);
        FAIL_TRANSITIONS.put(TestStatusCode.CONFIRMED, TestStatusCode.CONFIRMED); // A confirmed test can't go to FAILED
        FAIL_TRANSITIONS.put(TestStatusCode.FAILED, TestStatusCode.FAILED);
        FAIL_TRANSITIONS.put(TestStatusCode.ERROR, TestStatusCode.ERROR);
        FAIL_TRANSITIONS.put(TestStatusCode.ERROR_CONFIRMED, TestStatusCode.ERROR_CONFIRMED);
        FAIL_TRANSITIONS.put(TestStatusCode.STOPPED, TestStatusCode.STOPPED);
        FAIL_TRANSITIONS.put(TestStatusCode.NO_RECORDING, TestStatusCode.NO_RECORDING_FAIL);
        FAIL_TRANSITIONS.put(TestStatusCode.NO_RECORDING_OK, TestStatusCode.NO_RECORDING_OK);
        FAIL_TRANSITIONS.put(TestStatusCode.NO_RECORDING_FAIL, TestStatusCode.NO_RECORDING_FAIL);

    }

    private static final Map<TestStatusCode, TestStatusCode> ERROR_TRANSITIONS = new HashMap<>();
    static {
        ERROR_TRANSITIONS.put(TestStatusCode.STARTING, TestStatusCode.ERROR);
        ERROR_TRANSITIONS.put(TestStatusCode.RUNNING, TestStatusCode.ERROR);
        ERROR_TRANSITIONS.put(TestStatusCode.ABORTED, TestStatusCode.ERROR);
        ERROR_TRANSITIONS.put(TestStatusCode.UNCONFIRMED, TestStatusCode.ERROR);
        ERROR_TRANSITIONS.put(TestStatusCode.CONFIRMED, TestStatusCode.ERROR_CONFIRMED);
        ERROR_TRANSITIONS.put(TestStatusCode.FAILED, TestStatusCode.ERROR);
        ERROR_TRANSITIONS.put(TestStatusCode.ERROR, TestStatusCode.ERROR);
        ERROR_TRANSITIONS.put(TestStatusCode.ERROR_CONFIRMED, TestStatusCode.ERROR_CONFIRMED);
        ERROR_TRANSITIONS.put(TestStatusCode.STOPPED, TestStatusCode.STOPPED);
        ERROR_TRANSITIONS.put(TestStatusCode.NO_RECORDING, TestStatusCode.NO_RECORDING);
        ERROR_TRANSITIONS.put(TestStatusCode.NO_RECORDING_OK, TestStatusCode.NO_RECORDING_OK);
        ERROR_TRANSITIONS.put(TestStatusCode.NO_RECORDING_FAIL, TestStatusCode.NO_RECORDING_FAIL);
    }
}
