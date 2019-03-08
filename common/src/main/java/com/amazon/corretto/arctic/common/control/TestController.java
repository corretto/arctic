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

package com.amazon.corretto.arctic.common.control;


import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class TestController {
    private String currentTestGroup = "";
    private String currentTestClass = "Unknown";
    private String currentTestCase = "Unknown";
    private final List<Listener> listeners = new ArrayList<>();

    public String getCurrentTestCase() {
        return currentTestCase;
    }

    public String getCurrentTestClass() {
        return currentTestClass;
    }

    public String getCurrentTestGroup() {
        return currentTestGroup;
    }

    public void register(Listener observer) {
        listeners.add(observer);
    }

    public void startTestGroup(String testGroup) {
        log.debug("startTestGroup {}", testGroup);
        this.currentTestGroup = testGroup;
        listeners.forEach((it) -> it.startTestGroup(testGroup));
    }

    public void finishTestGroup(String testGroup, boolean groupResult) {
        log.debug("finishTestGroup {} with {}", testGroup, groupResult);
        this.currentTestGroup = null;
        listeners.forEach((it) -> it.finishTestGroup(testGroup, groupResult));
    }

    public void startTestCase(String testClass, String testCase) {
        log.debug("startTestCase {}:{}", testClass, testCase);
        this.currentTestClass = testClass;
        this.currentTestCase = testCase;
        listeners.forEach((it) -> it.startTestCase(currentTestGroup, testClass, testCase));
    }

    public void finishTestCase(String testClass, String testCase, boolean testResult) {
        log.debug("finishTestCase {}:{} with {}", testClass, testCase, testResult);
        this.currentTestClass = null;
        this.currentTestCase = null;
        listeners.forEach((it) -> it.finishTestCase(currentTestGroup, testClass, testCase, testResult));
    }

    public void stop() {
        log.debug("stop");
        listeners.forEach(Listener::stopTest);
    }

    public interface Listener {

        default void startTestGroup(String testGroup) {}
        default void finishTestGroup(String testGroup, boolean groupResult) {}

        /**
         * Starts a specific test.
         * @param testGroup the group is a value that can change between runs and is not used for test identification, but
         *                  can be used in certain reporting situations.
         * @param testClass Class for the test. This is used to uniquely identify the recording alongside the testCase
         * @param testCase Specific test case to start. This is used to uniquely identify the recording alongside testClass.
         */
        default void startTestCase(String testGroup, String testClass, String testCase){}

        /**
         * Finish the execution of a specific test. This is used in confirmation mode, when we get an external source of
         * data to confirm that the test did pass.
         * @param testGroup group to which the test belongs. Might change between runs.
         * @param testClass Class for the test. This is used to uniquely identify the recording alongside the testCase
         * @param testCase Specific test case to start. This is used to uniquely identify the recording alongside testClass.
         * @param result whether the test is confirmed to pass.
         */
        default void finishTestCase(String testGroup, String testClass, String testCase, boolean result){}
        default void stopTest(){}
    }
}
