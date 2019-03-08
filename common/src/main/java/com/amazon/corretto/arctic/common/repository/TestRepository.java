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

package com.amazon.corretto.arctic.common.repository;

import java.nio.file.Path;

import com.amazon.corretto.arctic.common.model.TestId;

/**
 * A full repository interface. It contains all the methods from {@link TestLoadRepository} and
 * {@link TestSaveRepository} in addition to new methods that may update existing tests.
 */
public interface TestRepository extends TestLoadRepository, TestSaveRepository {
    /**
     * The global scope name. This is the fallback scope on which the repository will look for tests if not found in the
     * specific running scope. This can be use to record tests that are valid regardless of the scope.
     */
    String DEFAULT_SCOPE = "default";

    /**
     * It updates a test to support an additional image for a screen check.
     * @param testId Id of the test to update.
     * @param scope The scope on which the test to add the alternative exists
     * @param scFilename value of the filename for the screen check we want to update.
     * @param scHash expected has value of the stored image. This is used to check we are not adding the alternative to
     *               the wrong image.
     * @param alternativePath location of the alternative image.
     * @param alternativeHash hash of the alternative image.
     * @return true if the alternative image for the screen check was successfully added.
     */
    boolean addAlternative(TestId testId, String scope, Path scFilename, String scHash, Path alternativePath,
                           String alternativeHash);

    /**
     * Types of scope working modes.
     * single: Only tests that match the specific scope will be loaded.
     * default: Tests of the specific scope will be loaded. If not found, fall back to the default scope.
     * incremental: Treat the scope as a number that increase to represent versioning. Pick the highest version of a
     *   test. The default scope is considered as 0.
     * custom: Manually specify the scope load chain priority in the configuration.
     */
    enum Mode {
        SINGLE("single"),
        DEFAULT("default"),
        INCREMENTAL("incremental"),
        CUSTOM("custom");

        private final String value;
        Mode(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
