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
package com.amazon.corretto.arctic.player;

import com.amazon.corretto.arctic.common.control.TestController;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.repository.TestLoadRepository;
import com.amazon.corretto.arctic.player.backend.MultiBackendPlayer;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.postprocessing.PostProcessingManager;
import com.amazon.corretto.arctic.player.preprocessing.PreProcessingManager;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class for the ArcticPlayer. It receives signals when tests start and end and will coordinate the replay of the
 * events.
 */
@Singleton
public final class ArcticPlayer implements TestController.Listener {
    private static final Logger log = LoggerFactory.getLogger(ArcticPlayer.class);
    private final TestLoadRepository testLoadRepository;
    private final MultiBackendPlayer backendPlayer;
    private final PreProcessingManager preProcessingManager;
    private final PostProcessingManager postProcessingManager;
    private final boolean confirmationMode;
    private ArcticRunningTest runningTest;
    private ArcticRunningTest previousTest;

    /**
     * Creates a new instance. Usually called by the dependency injector.
     * @param testRepository A repository from when to load the different tests.
     * @param backendPlayer To post the events back during replay;
     * @param preProcessingManager To execute the preprocessing pipeline before replaying a test.
     * @param postProcessingManager To execute the postprocessing pipeline after replaying a test
     * @param confirmationMode If true, the player will wait for a signal indicating the test has finished successfully
     *                         before marking it as a success.
     */
    @Inject
    public ArcticPlayer(final TestLoadRepository testRepository,
                        final MultiBackendPlayer backendPlayer,
                        final PreProcessingManager preProcessingManager,
                        final PostProcessingManager postProcessingManager,
                        @Named(InjectionKeys.CONFIRMATION_MODE) final boolean confirmationMode, TestController testController) {
        this.testLoadRepository = testRepository;
        this.backendPlayer = backendPlayer;
        this.preProcessingManager = preProcessingManager;
        this.postProcessingManager = postProcessingManager;
        this.confirmationMode = confirmationMode;
    }

   /**
     * Starts the playback of a case.
     * @param testClass Name of the test to start executing.
     * @param testCase Test case to execute.
     */
    @Override
    public void startTestCase(final String testGroup, final String testClass, final String testCase) {
        backendPlayer.interrupt(); // This will signal any thread that might be running to finish.
        synchronized (this) {
            previousTest = runningTest;
            TestId id = new TestId(testClass, testCase);
            runningTest = testLoadRepository.getTestCase(id)
                    .map(ArcticRunningTest::of)
                    .orElseGet(() -> ArcticRunningTest.noRecording(id));
            if (!runningTest.getStatus().getStatusCode().equals(TestStatusCode.NO_RECORDING)) {
                processTestCase();
            } else {
                log.debug("{}:{} is not processed, as it has no recording", testClass, testCase);
            }
        }
    }

    /**
     * Signals ArcticPlayer that a test has finished. This can only be done for the test that is currently running or
     * the test that was running just before that. This means it is possible for a call to this method to overlap
     * and happen at the same time we are handling a {@link ArcticPlayer#startTestCase(String, String, String)}.
     * @param testClass Name of the testCase that has finished
     * @param testCase Exact case that has finished
     * @param result Result of the operation, passed of failed.
     */
    @Override
    public void finishTestCase(final String testGroup, final String testClass, final String testCase, final boolean result) {
        log.debug("Processing finished for {}:{}", testClass, testCase);
        log.debug("Test that was running: {}", runningTest == null ? null : runningTest.getTestId());
        log.debug("Test that was previous: {}", previousTest == null ? null : previousTest.getTestId());
        ArcticRunningTest test = runningTest;
        final ArcticRunningTest previous = previousTest;
        if (!isSameTest(test, testClass, testCase) && !isSameTest(previous, testClass, testCase)) {
            // We have received order to finish a test that is not the one we were running, not the previous one.
            // Do a full stop.
            log.trace("We got a stop");
            stopTest();
        } else {
            if (!isSameTest(test, testClass, testCase) && isSameTest(previous, testClass, testCase)) {
                // We need to ensure the pipeline is executed for the proper test, which could be the current one or
                // the previous one.
                log.trace("Doing processing for {} instead of {}", previous.getTestId(), test.getTestId());
                test = previous;
            } else {
                // We only interrupt when the runningTest matches the one we are stopping, not if we are processing a
                // finishedTest for a test that has already finished
                log.trace("Interrupting {}", test.getTestId());
                backendPlayer.interrupt(test);
            }
            if (testGroup != null) {
                log.debug("Assigning test group {} to {}", testGroup, test.getTestId());
                test.getTestId().setTestGroup(testGroup);
                if (test.getRecording() != null) {
                    test.getRecording().setTestGroup(testGroup);
                }
            } else {
                log.trace("Not applying test group {} to {}", testGroup, test.getTestId());
            }
            if (confirmationMode) {
                // We intentionally transition twice. We don't know if we are coming before or after the backend
                // finishes, so the test can be in RUNNING or UNCONFIRMED at this moment.
                log.trace("Double test pass for {}", test.getTestId());
                test.getStatus().passed(result);
                test.getStatus().passed(result);
                postProcessingManager.postProcess(test);
                log.info("FINISHED: {}:{} with {}", test.getTestId().getTestClass(),
                        test.getTestId().getTestCase(), test.getStatus().getStatusCode());
            }
        }
    }

    /**
     * Checks if the running test is the same as an specific test.
     * @param test Test that is running.
     * @param testName Name of the test to check.
     * @param testCase Test case to check.
     * @return True if the running test matches the TestName and TestCase
     */
    private boolean isSameTest(final ArcticRunningTest test, final String testName, final String testCase) {
        return test != null && testName != null && testCase != null
                && testName.equals(test.getTestId().getTestClass())
                && testCase.equals(test.getTestId().getTestCase());
    }

    @Override
    public void stopTest() {
        log.info("Got stop signal");
        if (runningTest != null) {
            runningTest.getStatus().stop();
            backendPlayer.interrupt();
        }

        preProcessingManager.reset();
        postProcessingManager.reset();
    }

    private void processTestCase() {
        log.debug("Processing test {}:{}", runningTest.getRecording().getTestName(),
                runningTest.getRecording().getTestCase());

        preProcessingManager.preProcess(runningTest);
        backendPlayer.runTestEvents(runningTest);
        if (!confirmationMode) {
            // If we are not using confirmation mode, we run the post-processing pipeline just after running the events
            // If we are running in confirmation mode, the pipeline will be run as part of the finishedTestCase
            postProcessingManager.postProcess(runningTest);
            log.info("FINISHED: {}:{} with {}", runningTest.getRecording().getTestName(),
                    runningTest.getRecording().getTestCase(), runningTest.getStatus().getStatusCode());
        }
    }
}
