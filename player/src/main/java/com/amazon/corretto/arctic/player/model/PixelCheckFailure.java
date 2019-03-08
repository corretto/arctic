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
import java.util.LinkedHashMap;
import java.util.List;

import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;

/**
 * Class to hold data relevant to a pixel check failure. This will be used during review to display images to the user.
 */
public final class PixelCheckFailure {
    private final FailureId failureId;
    private final Path mainSavedImagePath;
    private final List<Path> savedImagePaths;
    private final String mainImageHash;
    private final String currentImageHash;
    private final String hashMode;
    private final Path currentImagePath;
    private final Path failuresFolder;
    private final float requiredConfidence;
    private final List<ScreenArea> shades;
    private final String scope;
    private final LinkedHashMap<PixelCheck.Type, Path> images = new LinkedHashMap<>();

    /**
     * Creates a new instance for a failure. This requires the PixelCheckResult for which there was a failure.
     * @param result PixelCheckResult that failed.
     * @param failuresFolder Where the image that was recorded is stored
     * @param currentImagePath How the image that was recorded is named
     */
    public PixelCheckFailure(final PixelCheckResult result, final Path failuresFolder, final Path currentImagePath) {
        this.failureId = new FailureId(result.getTestId(), result.getScope(), result.getMainSavedImagePath());
        this.mainSavedImagePath = result.getMainSavedImagePath();
        this.mainImageHash = result.getMainSavedHash();
        this.failuresFolder = failuresFolder;
        this.currentImagePath = currentImagePath;
        this.currentImageHash = result.getCurrentHash();
        this.hashMode = result.getHashMode();
        this.savedImagePaths = result.getSavedImagePaths();
        this.requiredConfidence = result.getTestConfidence();
        this.shades = result.getShades();
        this.scope = result.getScope();
    }

    /**
     * Returns the position of the shades (referenced to the workbench).
     * @return List of the shades positions
     */
    public List<ScreenArea> getShades() {
        return shades;
    }

    /**
     * Id of the test where the failure happened.
     * @return Id of the failure.
     */
    public FailureId getFailureId() {
        return failureId;
    }


    /**
     * Path of the recorded image within the repository.
     * @return Path of the recorded image within the repository.
     */
    public Path getMainSavedImagePath() {
        return mainSavedImagePath;
    }


    /**
     * Return the scope of the recording that failed.
     * @return Scope the test was loaded from.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Hash of the image that was captured during the replay.
     * @return Hash of the image that was captured during the replay.
     */
    public String getCurrentImageHash() {
        return currentImageHash;
    }

    /**
     * Path of the image that was captured during the replay.
     * @return Path of the image that was captured during the replay.
     */
    public Path getCurrentImageFullPath() {
        return currentImagePath;
    }

    /**
     * Hash of the image that was recorded. Used to ensure we don't modify the wrong ScreenshotCheck.
     * @return Hash of the image that was recorded. Used to ensure we don't modify the wrong ScreenshotCheck.
     */
    public String getMainImageHash() {
        return mainImageHash;
    }

    /**
     * Map containing the paths of all the images that can be checked during review.
     * @return Map containing the paths of all the images that can be checked during review.
     */
    public LinkedHashMap<PixelCheck.Type, Path> getImages() {
        return images;
    }

    @Override
    public String toString() {
        return "PixelCheckFailure{"
                + "testName='" + failureId.getTestId().getTestClass() + "'"
                + ", testCase='" + failureId.getTestId().getTestCase() + "'"
                + ", mainImageRecordedPath='" + failureId.getSavedImagePath() + "'"
                + ", targetImageHash='" + currentImageHash + "'"
                + ", destinationFolder='" + failuresFolder + "'"
                + ", destinationFile='" + currentImagePath + "'"
                + '}';
    }

    /**
     * Returns the required global level of confidence. This represents the percentage of pixels that should be an
     * exact match.
     * @return the global level of confidence for this failure.
     */
    public float getRequiredConfidence() {
        return requiredConfidence;
    }

    /**
     * Return a list with all the paths of the images that are valid for this ScreenCheck (main recorded image plus all
     * the alternatives).
     * @return a list with all the paths of the different valid images.
     */
    public List<Path> getSavedImagesPaths() {
        return savedImagePaths;
    }

    /**
     * Returns the name of the hashing algorithm used to calculate image hashes.
     * @return Name of the algorithm, usually MD5.
     */
    public String getHashMode() {
        return hashMode;
    }
}
