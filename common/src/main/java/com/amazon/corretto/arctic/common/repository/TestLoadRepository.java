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

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Optional;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.repository.impl.JsonFileTestLoadRepositoryImpl;
import com.google.inject.ImplementedBy;

/**
 * Defines a repository that can load test recordings. Repositories have the ability to save tests, images and events.
 * At the moment, {@link JsonFileTestLoadRepositoryImpl} is the only implementation
 */
@ImplementedBy(JsonFileTestLoadRepositoryImpl.class)
public interface TestLoadRepository {
    /**
     * Loads an image using its absolute path (or a relative path to the current dir). This is used when loading images
     * not for running tests, but during review process when loading images from the failures folder.
     * @param imgPath path of the image to load.
     * @return Image loaded. Null if unable to load the image.
     */
    BufferedImage loadImageAbsolutePath(Path imgPath);

    /**
     * Checks if a specific test exists in the repository.
     * @param testId Id of the test to check.
     * @return true if the test is found in the repository.
     */
    boolean contains(TestId testId);

    /**
     * Fetches an {@link ArcticTest} from the repository. This method will search in the current scope and the global
     * scope. This method does not need to load the full set of events. The test scope will contain the scope on from
     * which the repository loaded the test.
     *
     * @param testId Id of the test to load.
     * @return Empty if the test is not found in the repository.
     */
    Optional<ArcticTest> getTestCase(TestId testId);

    /**
     * Fetches an {@link ArcticTest} from the repository, but looking only at the specific scope. This method does not
     * need to load the full set of events.
     * @param scope Scope we want to look for the test
     * @param testId Id of the test to load.
     * @return Empty if a testId with that specific scope was not found on the repository.
     */
    Optional<ArcticTest> getTestCase(String scope, TestId testId);

    /**
     * Load all the events for an existing test if those are not loaded yet. Events may be serialized independently of
     * the main test.
     * @param test Test for which to load the events.
     */
    void loadTestEvents(ArcticTest test);

    /**
     * Loads an image from the repository. This requires the scope of the test that has the image registered.
     * @param scope Scope of the test to which the image belongs.
     * @param imgPath Path of the image relative to the scope folder.
     * @return Image loaded. Null if unable to find the image.
     */
    BufferedImage loadImage(String scope, Path imgPath);
}
