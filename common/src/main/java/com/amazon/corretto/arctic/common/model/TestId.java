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

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Class that represents a specific recording. A test group might cover multiple testClasses and one testClass might
 * have multiple testCase. TestGroup might be null and is not used to uniquely identify a recording.
 */
public final class TestId {
    private String testGroup;
    private String testClass;
    private String testCase;

    /**
     * Creates a new testId. The group is optional. The testClass and testCase uniquely identify the recording.
     * @param testGroup this value can change between recording and execution, but it is used for certain reporting
     *                  commands.
     * @param testClass class that identifies the test.
     * @param testCase specific testCase that identifies the recording.
     */
    public TestId(@Nullable final String testGroup, final String testClass, final String testCase) {
        this.testGroup = testGroup;
        this.testClass = testClass;
        this.testCase = testCase;
    }

    /**
     * Creates a new testId with null group. The testClass and testCase uniquely identify the recording.
     * @param testClass class that identifies the test.
     * @param testCase specific testCase that identifies the recording.
     */
    public TestId(final String testClass, final String testCase) {
        this(null, testClass, testCase);
    }

    /**
     * Returns the testGroup associated with this a recording. Can be null.
     * @return group of the test.
     */
    @Nullable
    public String getTestGroup() {
        return testGroup;
    }

    /**
     * Returns the test class associated with a recording.
     * @return class of the test
     */
    public String getTestClass() {
        return testClass;
    }

    /**
     * Returns the test case associated with a recording.
     * @return specific test case.
     */
    public String getTestCase() {
        return testCase;
    }

    /**
     * Sets the group associated with a recording.
     * @param testGroup group associated with the recording.
     */
    public void setTestGroup(@Nullable final String testGroup) {
        this.testGroup = testGroup;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TestId testId = (TestId) o;
        // We exclude testGroup from the comparison
        return Objects.equals(testClass, testId.testClass)
                && Objects.equals(testCase, testId.testCase);
    }

    @Override
    public int hashCode() {
        // We exclude testGroup from the hash
        return Objects.hash(testClass, testCase);
    }

    @Override
    public String toString() {
        return testClass + "/" + testCase;
    }
}
