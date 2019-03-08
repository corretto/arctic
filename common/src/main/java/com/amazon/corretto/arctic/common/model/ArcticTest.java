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

import java.util.LinkedList;
import java.util.List;

import com.amazon.corretto.arctic.common.model.event.Events;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.model.gui.Point;
import com.amazon.corretto.arctic.common.repository.TestRepository;
import com.amazon.corretto.arctic.common.serialization.GsonIntegerAsHexAdapter;
import com.google.gson.annotations.JsonAdapter;
import lombok.Data;

/**
 * Represents a complete test within Arctic.
 */
@Data
public final class ArcticTest {
    /**
     * The test name is also the name of the folder where all the test cases for the test will be stored.
     */
    private String testName;

    /**
     * When working with multi-tests, the identifier of the test case.
     */
    private String testCase;

    /**
     * TestId that represents the test, based on testName and testCase.
     */
    private transient TestId testId;

    /**
     * TestId that represents the test, based on testName and testCase.
     * @return The testId for this test.
     */
    public TestId getTestId() {
        if (testId == null) {
            testId = new TestId(testName, testCase);
        }
        return testId;
    }

    /**
     * Represents a greater group where the tests belong.
     */
    private String testGroup;

    /**
     * A scope the test belongs to, this is set when the test is loaded. We do not want to persist this value, as it
     * depends on which folder the test is located.
     */
    private transient String scope = TestRepository.DEFAULT_SCOPE;

    /**
     * When working with multi-tests, the order of the testCase.
     */
    private int testCaseNumber;

    /**
     * Where we need to click to ensure this test has focus.
     */
    private Point focusPoint;
    /**
     * When the test was recorded as reported by {@link System#nanoTime()}.
     */
    private long testRecordTime;

    /**
     * How long the test takes in nanoseconds.
     */
    private long testDuration;

    /**
     * Relative path (based on the test repository folder) where the file with events for this test is located).
     */
    private String eventsFile;

    /**
     * True if the events file for the test has been compressed using zip algorithm.
     */
    private boolean zip;

    /**
     * An integer that represents a mask with all the different events that were captured during the test recording.
     * @see com.amazon.corretto.arctic.common.model.event.ArcticEvent
     */
    @JsonAdapter(GsonIntegerAsHexAdapter.class)
    private int recordingMode;

    /**
     * This masks represents what events would the test want to be reproduced if not overriden.
     * @see com.amazon.corretto.arctic.common.model.event.ArcticEvent
     */
    @JsonAdapter(GsonIntegerAsHexAdapter.class)
    private int preferredPlayMode;

    private ArcticTestTimings timings = new ArcticTestTimings();

    private ArcticTestTruncations truncations = new ArcticTestTruncations();

    private ArcticTestMouseOffsets mouseOffsets = new ArcticTestMouseOffsets();

    /**
     * The resolution of the screen when te test was recorded.
     */
    private Point screenRes;

    /**
     * A capture of the test screen the moment we started.
     */
    private ScreenshotCheck initialSc;


    /**
     * All the different Screenshot checks for the tests.
     */
    private List<ScreenshotCheck> screenChecks = new LinkedList<>();

    /**
     * List with all the values of the events. This value is usually stored within the Events file and only loaded on
     * demand.
     */
    private transient Events events = new Events();
}
