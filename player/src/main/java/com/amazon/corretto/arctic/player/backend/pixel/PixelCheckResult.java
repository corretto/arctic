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

package com.amazon.corretto.arctic.player.backend.pixel;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.model.gui.ArcticFrame;
import com.amazon.corretto.arctic.common.model.gui.ScreenArea;

/**
 * This class contains all the information related to pixel by pixel comparison of images during the reproduction of the
 * tests. We start with both {@link ScreenshotCheck}, the recorded and the current one, and we try to see if they match.
 * Different comparators will check if they match, looking at different properties. A hash comparator may automatically
 * mark the result as passed, while one comparator that checks the dimensions will mark them as failed. Others may need
 * to work in tandem. A fuzzy comparator will not be enough alone to consider the images equals, but it will be enough
 * if other checks like the confidence also pass.
 */
public final class PixelCheckResult {
    private final transient Map<Path, BufferedImage> savedImages = new LinkedHashMap<>();
    private final List<Path> savedImagePaths = new ArrayList<>();
    private final Path mainSavedImagePath;
    private final Set<String> savedHashes = new HashSet<>();
    private final String mainSavedHash;

    private final float requiredConfidence;
    private final String hashMode;
    private final TestId testId;
    private final String scope;

    private final List<ScreenArea> shades;

    private final transient BufferedImage currentImage;
    private String currentHash = null;
    private PixelCheck.PixelCheckSummary strictSummary;
    private PixelCheck.PixelCheckSummary fuzzySummary;
    private final Map<Path, Map<PixelCheck.Type, Boolean>> performedChecks = new HashMap<>();

    private Status status = Status.UNKNOWN;

    /**
     * Creates a new pixel check result. This is done before we know the final results, as this object will be passed as
     * argument to the different checks.
     * @param current A screenshot check that represents what we have just seen on the screen.
     * @param saved A screenshot check that represents what was recorded.
     * @param testId The id of the test we were testing when found this.
     * @param scope From which scope we loaded the test. This is not always the same scope as the one we are running on.
     */
    public PixelCheckResult(final ScreenshotCheck current, final ScreenshotCheck saved, final TestId testId,
                            final String scope) {
        mainSavedImagePath = saved.getFilename();
        mainSavedHash = saved.getHashValue();
        currentImage = current.getImage();
        requiredConfidence = saved.getConfidenceLevel();
        savedImagePaths.add(saved.getFilename());
        savedImagePaths.addAll(saved.getAlternativeImages());
        savedHashes.add(saved.getHashValue());
        savedHashes.addAll(saved.getAlternativeHashes());
        hashMode = saved.getHashMode();
        ScreenArea wb = saved.getSa();
        shades = saved.getShades().stream()
                .map(ArcticFrame::getSa)
                .map(it -> new ScreenArea(it.getX() - wb.getX(), it.getY() - wb.getY(), it.getW(), it.getH()))
                .collect(Collectors.toList());
        this.testId = testId;
        this.scope = scope;
    }

    /**
     * Gets the location of all the shades during the screen capture. This is useful, as we may want to skip those areas
     * from the pixel by pixel comparison, both for reliability and performance.
     * @return list with the position of all the active shades.
     */
    public List<ScreenArea> getShades() {
        return shades;
    }

    /**
     * Get the summary of the strict check. The result contains the total number of pixels, the total number of failed
     * pixels and all the failed pixels.
     * @return The summary of the strict check.
     */
    public PixelCheck.PixelCheckSummary getStrictSummary() {
        return strictSummary;
    }

    /**
     * Get the summary of the fuzzy check. The result contains the total number of pixels, the total number of failed
     * pixels and all the failed pixels.
     * @return The summary of the fuzzy check.
     */
    public PixelCheck.PixelCheckSummary getFuzzySummary() {
        return fuzzySummary;
    }

    /**
     * Sets the summary of the strict check. This method should only be called by
     * {@link com.amazon.corretto.arctic.player.backend.pixel.check.StrictPixelCheck}, but it will be read by other
     * checks, specially the failures that were detected. Further checks may analyze those failures to see if they are
     * relevant or not, for example, the fuzzy check will consider a failed pixel to be ok if the different is only
     * within a small threshold.
     * @param strictSummary The result of the strict check.
     */
    public void setStrictSummary(final PixelCheck.PixelCheckSummary strictSummary) {
        this.strictSummary = strictSummary;
    }

    /**
     * Sets the summary of the fuzzy check. This method should only be called by
     * {@link com.amazon.corretto.arctic.player.backend.pixel.check.FuzzyPixelCheck}, but it will be read by other
     * checks, specially the failures that were detected. Further checks may analyze those failures to see if they are
     * relevant or not, for example, the fuzzy check will consider a failed pixel to be ok if the different is only
     * within a small threshold.
     * @param fuzzySummary The result of the strict check.
     */
    public void setFuzzySummary(final PixelCheck.PixelCheckSummary fuzzySummary) {
        this.fuzzySummary = fuzzySummary;
    }

    /**
     * Id of the test associated with this pixel check.
     * @return The Id of the test.
     */
    public TestId getTestId() {
        return testId;
    }

    /**
     * Scope from which the image was loaded. This can be different that the scope we are running, depdending on the
     * scope mode.
     * @return The scope we are running in.
     */
    public String getScope() {
        return scope;
    }

    /**
     * The path for the originally recorded image.
     * @return The path of the original recorded image.
     */
    public Path getMainSavedImagePath() {
        return mainSavedImagePath;
    }

    /**
     * The hash of the originally recorded image.
     * @return A String with the hash of the originally recorded image.
     */
    public String getMainSavedHash() {
        return mainSavedHash;
    }

    /**
     * The current status of the pixel check. There are three possible values:
     * {@link Status#PASSED} The check has passed
     * {@link Status#FAILED} The check has failed
     * {@link Status#UNKNOWN} We haven't determined yet if the check passes.
     * @return Status of the pixel check.
     */
    public PixelCheckResult.Status getStatus() {
        return status;
    }

    /**
     * Sets the status of the check. Setting the status to true if the check is already in Failed will do nothing.
     * @param result True if we want to attempt to mark the test as passed.
     */
    void setStatus(final boolean result) {
        if (!result) {
            status = PixelCheckResult.Status.FAILED;
        }
        if (result && PixelCheckResult.Status.UNKNOWN.equals(status)) {
            status = PixelCheckResult.Status.PASSED;
        }
    }

    /**
     * Checks if a specific pixel check has run.
     * @param alternative For which alternative we want to see if a specific check has passed.
     * @param type The check we want to see if it has run.
     * @return True if the specific check has been executed for that alternative. False otherwise.
     */
    public boolean hasRun(final Path alternative, final PixelCheck.Type type) {
        return performedChecks.containsKey(alternative)
                && performedChecks.get(alternative).containsKey(type);
    }

    /**
     * Get a map with all the checks that have run for a specific alternative.
     * @param alternative The alternative for which we want the checks.
     * @return A map with all the checks that have run and their result.
     */
    public Map<PixelCheck.Type, Boolean> getRanChecks(final Path alternative) {
        return performedChecks.computeIfAbsent(alternative, k -> new HashMap<>());
    }

    /**
     * Record the result of one check for one specific alternative.
     * @param alternative The alternative for which we are recording the result.
     * @param type The PixelCheck type for which we are recording the result.
     * @param value Whether the specific pixel check type passed.
     */
    void recordCheck(final Path alternative, final PixelCheck.Type type, final boolean value) {
        performedChecks.computeIfAbsent(alternative, k -> new HashMap<>()).put(type, value);
    }

    /**
     * Returns the BufferedImage that represents what has been captured from the screen during playback.
     * @return What has been captured during playback
     */
    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    /**
     * Retrieves a specific BufferedImage for an alternative. This method will not load the image from disk, only return
     * the image if it has been previously loaded and stored with {@link #putSavedImage(Path, BufferedImage)}.
     * @param alternative Path of the alternative we want to retrieve.
     * @return The buffered image if it was loaded already. Null otherwise.
     */
    public BufferedImage getSavedImage(final Path alternative) {
        return savedImages.get(alternative);
    }

    /**
     * Stores a BufferedImage that can be later retrieved with {@link #getSavedImage(Path)}.
     * @param path Alternative for the image
     * @param image The buffered image to store
     */
    public void putSavedImage(final Path path, final BufferedImage image) {
        savedImages.put(path, image);
    }

    /**
     * A list of all the paths representing all the different alternatives we need to compare to determine if this
     * pixel check passes. This includes the one in {@link #getMainSavedImagePath()}
     * @return The list with all the images to compare.
     */
    public List<Path> getSavedImagePaths() {
        return savedImagePaths;
    }

    /**
     * What algorithm was used to calculate the hash of the images.
     * @return A string representing which hash algorithm was used.
     */
    public String getHashMode() {
        return hashMode;
    }

    /**
     * Gets the hash of the image that has been captured during playback.
     * @return String representing the hash of the image.
     */
    public String getCurrentHash() {
        return currentHash;
    }


    /**
     * Sets the hash of the image that has been captured during playback.
     * @param hash String representing the hash of the image.
     */
    public void setCurrentHash(final String hash) {
        this.currentHash = hash;
    }

    /**
     * Whether a specific hash matches the hash of any of the alternatives.
     * @param hash String representing the hash of the image.
     * @return True if any of the alternatives has the same hash.
     */
    public boolean isValidHash(final String hash) {
        return savedHashes.contains(hash);
    }

    /**
     * Returns the percentage of pixels that need to be exactly the same for the check to pass.
     * @return A float representing the percentage of pixels that must match.
     */
    public float getTestConfidence() {
        return requiredConfidence;
    }

    /**
     * Represents the status of the PixelCheck as a whole.
     */
    enum Status {
        /**
         * This is the default value when we start. We have not determined yet whether we have passed or not.
         */
        UNKNOWN,

        /**
         * The pixel check has passed. Even if there are checks to perform, we have already determined the image is
         * valid
         */
        PASSED,

        /**
         * The pixel check has failed. Even if there are still checks pending, they will not be enough to pass the
         * check.
         */
        FAILED
    }
}
