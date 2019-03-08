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

import java.nio.file.Path;
import java.util.Objects;

import com.amazon.corretto.arctic.common.model.TestId;

/**
 * Simple class to represent a specific failure. This is identified by a {@link TestId} and a string that represents
 * the pixel check that was failed. Immutable.
 */
public final class FailureId {

    private final TestId testId;
    private final String scope;
    private final Path savedImagePath;

    /**
     * @return the TestId that identifies to which test this failure belongs.
     */
    public TestId getTestId() {
        return testId;
    }

    /**
     * @return the scope on which the test this failure belongs was located.
     */
    public String getScope() {
        return scope;
    }

    /**
     * @return The path of the current image for the screen check.
     */
    public Path getSavedImagePath() {
        return savedImagePath;
    }

    /**
     * Creates a new instance of the TestId.
     * @param testId Identifies to which test the failure belongs.
     * @param scope The scope on which the test was located.
     * @param savedImagePath Identifies the screen check that failed.
     */
    public FailureId(final TestId testId, final String scope, final Path savedImagePath) {
        this.testId = testId;
        this.scope = scope;
        this.savedImagePath = savedImagePath;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FailureId failureId = (FailureId) o;
        return testId.equals(failureId.testId)
                && scope.equals(failureId.scope)
                && savedImagePath.equals(failureId.savedImagePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testId, scope, savedImagePath);
    }

    @Override
    public String toString() {
        return savedImagePath.toString().replace('\\', '/');
    }
}
