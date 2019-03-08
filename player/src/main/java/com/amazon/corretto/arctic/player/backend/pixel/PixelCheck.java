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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface that represents a pixel check.
 */
public interface PixelCheck {
    Logger log = LoggerFactory.getLogger(PixelCheck.class);

    /**
     * For a check to pass, all non-sufficient checks plus one sufficient check need to pass.
     *
     * @return true if the test is sufficient
     */
    boolean isSufficient();

    /**
     * Used to determine the order on which the checks will be executed.
     *
     * @return lower numbers will be executed first
     */
    int getPriority();

    /**
     * A name to identify the check. This can be used to identify them
     *
     * @return A string with the name
     */
    Type getType();

    /**
     * Check if the currentImage as a whole passes. In certain situations, some pixels may fail yet still be good
     * enough for the image to be considered correct.
     * @param result Result of the pixel to pixel comparison.
     * @param alternative Alternative for which we want to do the check.
     * @return true if the check passed
     */
    boolean doCheck(PixelCheckResult result, Path alternative);

    /**
     * Generate the diff images, hints and masks by comparing the current image with a specific alternative stored
     * in the ArcticDiffImages.
     * @param alternative Alternative for which we want to generate the diff/hint/mask set.
     * @param diffImages Holds the current and alternative images, as well as storing results.
     */
    void doDiff(Path alternative, ArcticDiffImages diffImages);

    /**
     * A list with all the dependencies of a specific pixel check. This allows the code to verify that expected pixel
     * checks have indeed run before.
     * @return A list with different types of pixel checks.
     */
    List<Type> getDependencyChecks();

    /**
     * Default implementation that wraps the {@link PixelCheck#doCheck(PixelCheckResult, Path)} method. This
     * implementation validates dependencies between different checks, and compounds the results.
     * @param result Holds the current and alternative images, as well as storing results
     * @param alternative For which alternative we want to do the check
     * @return True if the image is considered acceptable
     */
    default boolean check(final PixelCheckResult result, final Path alternative) {
        log.trace("Check {}", getType());
        if (PixelCheckResult.Status.UNKNOWN.equals(result.getStatus())) {
            boolean checkResult = false;
            final Optional<Type> failedDependency = getDependencyChecks().stream()
                    .filter(it -> !result.hasRun(alternative, it))
                    .findFirst();
            failedDependency.ifPresent(it -> log.warn("Failed check {} as {} is missing", getType(), it));
            if (failedDependency.isEmpty()) {
                log.trace("Running check {}", getType());
                checkResult = doCheck(result, alternative);
            }
            log.trace("{} result: {}", getType(), checkResult);
            if (checkResult == isSufficient()) {
                log.trace("{} setting result {}", getType(), checkResult);
                result.setStatus(checkResult);
                log.trace("{} set result to {}", getType(), result.getStatus());
            }
            log.trace("{} recording result {}", getType(), checkResult);
            result.recordCheck(alternative, getType(), checkResult);
        } else {
            log.trace("{} skipped as status is {}", getType(), result.getStatus());
            log.trace("{} result set to false", getType());
            result.recordCheck(alternative, getType(), false);
            log.trace("{} set result to {}", getType(), result.getStatus());
        }
        log.trace("After {}, Status is {}", getType(), result.getStatus());
        log.trace("After {}, Passed: {}", getType(), PixelCheckResult.Status.PASSED.equals(result.getStatus()));

        return PixelCheckResult.Status.PASSED.equals(result.getStatus());
    }

    /**
     * Default implementation that wraps {@link PixelCheck#doDiff(Path, ArcticDiffImages)}. It performs dependency
     * checks and records stats of the execution.
     * @param alternative Alternative for which we want to generate the diff/hint/mask set.
     * @param diffImages Holds the current and alternative images, as well as storing results.
     */
    default void generateDiff(final Path alternative, final ArcticDiffImages diffImages) {
        log.trace("Generating diff {} for {}", getType(), alternative);
        long start = System.currentTimeMillis();

        final Optional<Type> failedDependency = getDependencyChecks().stream()
                .filter(it -> !diffImages.hasRun(alternative, it))
                .findFirst();
        if (failedDependency.isPresent()) {
            log.warn("Failed to generate diff {} as {} is missing", getType(), failedDependency.get());
            return;
        }

        doDiff(alternative, diffImages);
        long time = System.currentTimeMillis() - start;
        diffImages.addProperty(alternative, getType(), "time", time, "ms");
        diffImages.recordDiff(alternative, getType());
    }


    /**
     * Enum that represents the different types of checks as well as their associated images.
     */
    enum Type {
        /**
         * Basic hint, based on the Strict check type.
         */
        HINT("hint", false, true, null, 10),

        /**
         * Hint generated for a fuzzy image.
         */
        FUZZY_HINT("fuzzy.hint", false, true, null, 11),

        /**
         * Hint generated for a cluster resolution.
         */
        CLUSTER_HINT("cluster.hint", false, true, null, 12),

        /**
         * Basic mask, based on the Strict check type.
         */
        MASK("mask", false, true, null, 13),

        /**
         * Mask generated for a fuzzy image.
         */
        FUZZY_MASK("fuzzy.mask", false, true, null, 14),

        /**
         * Mask generated for a cluster resolution.
         */
        CLUSTER_MASK("cluster.mask", false, true, null, 15),

        /**
         * Represents the image that was captured during playback.
         */
        CURRENT("current", true, false, HINT, 0),

        /**
         * Represents the image that was captured during recording.
         */
        RECORDED("recorded", true, false, HINT, 1),

        /**
         * Comparator that checks pixel by pixel expecting all of them to be the same.
         */
        STRICT("strict", true, false, HINT, 2),

        /**
         * Comparator that checks pixel by pixel allowing some deviation in the RGB components.
         */
        FUZZY("fuzzy", true, false, FUZZY_HINT, 3),

        /**
         * Comparator that will accept a pixel that is not the same if pixels around it are.
         */
        CLUSTER("cluster", true, false, CLUSTER_HINT, 4),

        /**
         * Comparator that just checks the hash. Has no image/hint/mask associated
         */
        HASH("hash", false, false, null, 20),

        /**
         * Comparator that just checks the dimensions. Has no image/hint/mask associated
         */
        DIMENSION("dimension", false, false, null, 21),

        /**
         * Comparator that checks the percentage of pixels that match. Has no image/hint/mask associated
         */
        CONFIDENCE("confidence", false, false, null, 22),

        /**
         * A placeholder for properties that belong to the whole alternative.
         */
        ALTERNATIVE("alternative", false, false, null, 23),

        /**
         * A placeholder for properties that belong to the whole check.
         */
        GLOBAL("global", false, false, null, 24),

        /**
         * Should only appear when deserializing data between different versions of Arctic.
         */
        UNKNOWN("unknown", false, false, null, Integer.MAX_VALUE);

        static final Map<String, Type> ALL_TYPES;

        static {
            ALL_TYPES = Arrays.stream(Type.values()).collect(Collectors.toMap(Type::getName, Function.identity()));
        }
        private final String name;
        private final boolean isHint;
        private final boolean isImage;
        private final Type hint;
        private final int order;

        Type(final String name, final boolean isImage, final boolean isHint, final Type hint, final int order) {
            this.name = name.toLowerCase();
            this.isHint = isHint;
            this.isImage = isImage;
            this.hint = hint;
            this.order = order;
        }

        /**
         * Whether the current Type is to generate an image. Being an Image means we display it in the image reel of
         * the review process and the user can swap between images using the buttons.
         * @return True if the type is for an image.
         */
        public boolean isImage() {
            return isImage;
        }

        /**
         * Whether the current Type is to generate a hint. A hint is an overlay that is drawn on top of the image reel.
         * This overlay helps locate the parts of the images that are different. Masks are a type of hints.
         * @return True if the type is for a hint.
         */
        public boolean isHint() {
            return isHint;
        }

        /**
         * An integer to select the order on which this should be considered. This way, we always have a consistent
         * position for the different UI components associated to the PixelChecks. The order does not match the order of
         * the enumeration as hints have to be declared before images, but images should go before in the order.
         * @return An int representing the order.
         */
        public int getOrder() {
            return order;
        }

        /**
         * Gets the hint associated with this specific check. This is useful if we want to automatically select a hint
         * based on which image is currently being displayed in the reel.
         * @return The hint associated with this check. Can be null.
         */
        public Type getHint() {
            return hint;
        }

        /**
         * Name of the PixelCheck type.
         * @return Name of the PixelCheck type.
         */
        public String getName() {
            return name;
        }

        /**
         * Checks if a specific string matches a known PixelCheck type. Case-insensitive.
         * @param name Name of the PixelCheck type.
         * @return True if name is valid.
         */
        public static boolean isType(final String name) {
            return ALL_TYPES.containsKey(name.toLowerCase());
        }

        /**
         * Returns the PixelCheck type associated with one name. Case-insensitive.
         * @param name Name of the PixelCheck type.
         * @return The PixelCheck type associated with that name if exists. {@link PixelCheck.Type#UNKNOWN} otherwise.
         */
        public static Type fromString(final String name) {
            return Type.ALL_TYPES.getOrDefault(name.toLowerCase(), UNKNOWN);
        }
    }

    /**
     * A class that holds the Confidence check results. This includes a list of all the different pixels that were
     * different, and this is used by future checks
     */
    final class PixelCheckSummary {
        private final Set<Integer>[] failedPixels;
        private final int totalPixels;
        private int totalFailedPixels = 0;

         /**
         * Creates a new PixelCheckSummary.
         * @param width Width of the image for the summary
         * @param height Height of the image for the summary
         */
        @SuppressWarnings("unchecked")
        public PixelCheckSummary(final int width, final int height) {
            totalPixels = width * height;
            failedPixels = new Set[width];
        }

        /**
         * All the points that were different between both images.
         * @return A Set[], with the array index representing x and they different y values in the set.
         */
        public Set<Integer>[] getFailedPixels() {
            return failedPixels;
        }

        /**
         * Adds a failure to this summary.
         * @param x x coordinate of the failure
         * @param y y coordinate of the failure
         */
        public void addFailure(final int x, final int y) {
            if (failedPixels[x] == null) {
                failedPixels[x] = new HashSet<>();
            }
            failedPixels[x].add(y);
            totalFailedPixels++;
        }

        /**
         * Returns the number of pixels in the summary.
         * @return number of pixels
         */
        public int getTotalPixels() {
            return totalPixels;
        }

        /**
         * Total number of pixels that failed.
         * @return A number with the total number of pixels failed.
         */
        public int getTotalFailedPixels() {
            return totalFailedPixels;
        }

        /**
         * Returns the confidence, which is the ratio of valid pixels.
         * @return the ratio of valid pixels.
         */
        public float getConfidence() {
            return 1 - ((float) totalFailedPixels / totalPixels);
        }
    }
}
