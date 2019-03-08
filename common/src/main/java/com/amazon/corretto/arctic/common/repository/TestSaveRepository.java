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

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.util.Pair;

/**
 * This interface represents a repository with saving (but not loading) capabilities. A save repository should be able
 * to persist the different test cases. Also, save repositories should be scope agnostic, being able to record tests
 * for any scope. Additionally, some helper functions are expected from the repository, like the ability to persist
 * images.
 */
public interface TestSaveRepository {
    /**
     * Save or updates a test in the repository. All events will be persisted
     * @param test Test to save/update
     * @return true if the test was successfully saved/updated.
     */
    boolean saveTestCase(ArcticTest test);

    /**
     * Save or updates a test in the repository.
     * @param test Test to save/update
     * @param includeEvents Whether we want to persist all the events. If we are only updating an existing test, it may
     *                      not require to save the events again.
     * @return true if the test was successfully saved/updated.
     */
    boolean saveTestCase(ArcticTest test, boolean includeEvents);

    /**
     * Stores an image in the repository.
     * @param testName Test the image belongs to.
     * @param testCase Test case the image belongs to.
     * @param scope The scope on which we are saving the image.
     * @param imgName Name of the image
     * @param image Contents of the image
     * @return A Pair. Left value is the format on which the image has been saved. Right value is the repository path.
     */
    Pair<String, Path> saveImage(String testName, String testCase, String scope, String imgName, BufferedImage image);

    /**
     * Save an image in an arbitrary path. This is used for images we don't want to be associated with a test. This is
     * the case of images we may save for failures. There may be useful for reviews, but they shouldn't be stored
     * permanently as part of the test.
     * @param imagePath Path where we want to store the image
     * @param image image to save
     * @return A pair. The left value is the format used to persist the image, the right value, the final path.
     */
    Pair<String, Path> saveImageAbsolutePath(Path imagePath, BufferedImage image);

    /**
     * Moves an alternative image that has been previously saved to disk to the repository. This is used when a failure
     * that was stored into the failures gets promoted to an alternative image for the test.
     * @param sc ScreenshotCheck for which we want to copy the alternative image into the repository.
     * @param scope The scope on which we want to copy the alternative.
     * @param alternativeImageAbsolutePath Absolute path of the image to move.
     * @return The path and name assigned to the alternative image within the repository.
     */
    Path copyAlternativeImage(String scope, ScreenshotCheck sc, Path alternativeImageAbsolutePath);

    /**
     * Remove a test from the repository.
     * @param testId The id of the test to remove
     * @param scope The scope from which the test should be removed.
     * @return true if all the files for the test where removed correctly.
     */
    boolean removeTestCase(TestId testId, String scope);
}
