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

import java.util.HashSet;
import java.util.Set;

import com.amazon.corretto.arctic.player.control.TimeController;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.preprocessing.ArcticPlayerPreProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Different tests might be run by different VMs. That means the first testcase of a group can take significantly more
 * to be drawn in the screen, as it needs to initialize the VM. To avoid having to increase the delay for all the tests,
 * this preprocessor will add a delay, but only for the first test of the test.
 *
 * This is detected by storing the previous testName and all the testCases associated with it. If the testName changes
 * or we are asked to run a testCase we have already run, it means we are restarting the process.
 */
public final class FirstTestDelayPreProcessor implements ArcticPlayerPreProcessor {
    private static final Logger log = LoggerFactory.getLogger(FirstTestDelayPreProcessor.class);

    public static final String NAME = "firstTestDelay";
    private static final int PRIORITY = 20;
    private final TimeController timeController;
    private final long firstTestDelay;

    private String lastTest;
    private final Set<String> knownTestCases = new HashSet<>();

    /**
     * Creates a new instance of the preprocessor. To be called by the dependency injector.
     * @param firstTestDelay amount of time to wait for the first test.
     * @param timeController Used to issue the delay in a controlled manner.
     */
    @Inject
    public FirstTestDelayPreProcessor(@Named(InjectionKeys.PRE_FIRST_TEST_DELAY_WAIT) final long firstTestDelay,
                                      final TimeController timeController) {
        this.firstTestDelay = firstTestDelay;
        this.timeController = timeController;

    }

    @Override
    public boolean preProcess(final ArcticRunningTest test) {
        if (test.getStatus().getStatusCode().equals(TestStatusCode.STARTING)) {
            if (!test.getRecording().getTestName().equals(lastTest)
                    || knownTestCases.contains(test.getRecording().getTestCase())) {
                log.debug("Delaying first test for {}", firstTestDelay);
                timeController.waitFor(firstTestDelay);
                lastTest = test.getRecording().getTestName();
                knownTestCases.clear();
                knownTestCases.add(test.getRecording().getTestCase());
            } else {
                knownTestCases.add(test.getRecording().getTestCase());
            }
        } else if ((test.getStatus().getStatusCode().equals(TestStatusCode.ABORTED))) {
            lastTest = null;
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
        return Set.of(TestStatusCode.STARTING, TestStatusCode.ABORTED);
    }
}
