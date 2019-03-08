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

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;

/**
 * Class that holds data necessary to perform the review, as it includes all the images the user can check for a
 * screenshot check. It is constructed from a {@link PixelCheckFailure} but it holds the actual images in memory.
 */
public final class ArcticDiffImages {
    private final FailureId failureId;
    private final List<Path> alternatives;
    private final Path currentImagePath;
    private final String currentHash;
    private final String hashMode;
    private final String scope;

    private BufferedImage currentImage;
    private final Map<Path, Map<PixelCheck.Type, BufferedImage>> images = new LinkedHashMap<>();
    private final Map<Path, Map<PixelCheck.Type, BufferedImage>> hints = new LinkedHashMap<>();
    private final Map<Path, Set<PixelCheck.Type>> ranChecks = new HashMap<>();
    private final Map<Path, List<String>> log = new LinkedHashMap<>();
    private final Map<Path, PixelCheck.PixelCheckSummary> strictSummaries = new HashMap<>();
    private final Map<Path, PixelCheck.PixelCheckSummary> fuzzySummaries = new HashMap<>();
    private final List<ScreenArea> shades;
    private final float testConfidence;
    private final Map<String, ArcticDiffProperty<?>> globalProperties;
    private final Map<Path, Map<String, ArcticDiffProperty<?>>> properties;
    private boolean completed = false;

    /**
     * Creates a new instance based on a PixelCheckFailure. PixelCheckFailures can be serialized and don't include any
     * actual image.
     * @param failure Failure to create the instance from.
     */
    public ArcticDiffImages(final PixelCheckFailure failure) {
        this.failureId = failure.getFailureId();
        this.alternatives = failure.getSavedImagesPaths();
        this.currentImagePath = failure.getCurrentImageFullPath();
        this.currentHash = failure.getCurrentImageHash();
        this.hashMode = failure.getHashMode();
        this.testConfidence = failure.getRequiredConfidence();
        this.shades = failure.getShades();
        this.globalProperties = new LinkedHashMap<>();
        this.properties = new LinkedHashMap<>();
        alternatives.forEach(it -> {
            images.put(it, new LinkedHashMap<>());
            hints.put(it, new LinkedHashMap<>());
            ranChecks.put(it, new HashSet<>());
            properties.put(it, new LinkedHashMap<>());
            log.put(it, new ArrayList<>());
        });
        this.scope = failure.getScope();
    }

    /**
     * Gets the hash calculated for the current image.
     * @return Hash for the current image as a string.
     */
    public String getCurrentHash() {
        return currentHash;
    }

    /**
     * Gets the algorithm used to calculate the hash of the current image.
     * @return String that represents the algorithm used.
     */
    public String getHashMode() {
        return hashMode;
    }

    /**
     * Returns the failureId that identifies the screenshot check that failed.
     * @return FailureId that represents the screenshot check that failed.
     */
    public FailureId getFailureId() {
        return failureId;
    }

    /**
     * Return the scope of the recording that failed.
     * @return Scope the test was loaded from.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Returns an ordered list of the different types of images (not hints) that this instance holds.
     * @return Ordered lists with all the types in the images.
     */
    public List<PixelCheck.Type> getImageTypes() {
        return images.values().stream().flatMap(it -> it.keySet().stream()).distinct()
                .sorted(Comparator.comparing(PixelCheck.Type::getOrder))
                .collect(Collectors.toList());
    }

    /**
     * Returns an ordered list of the different types of hints that this instance holds.
     * @return Ordered lists with all the types in the hints.
     */
    public List<PixelCheck.Type> getHintTypes() {
        return hints.values().stream().flatMap(it -> it.keySet().stream()).distinct()
                .sorted(Comparator.comparing(PixelCheck.Type::getOrder))
                .collect(Collectors.toList());
    }

    /**
     * Adds an entry to the log.
     * @param alternative Alternative that was being checked for which the log entry belongs.
     * @param type PixelCheck.Type that was being checked for which the log entry belongs.
     * @param msg Message that we want to add to the log.
     */
    public void addLog(final Path alternative, final PixelCheck.Type type, final String msg) {
        log.get(alternative).add(type + ": " + msg);
    }

    /**
     * Adds a property for the image.
     * @param <T> A Number
     * @param alternative Alternative that was being checked for which the property is being added.
     * @param type PixelCheck.Type that was being checked for which the log entry belongs.
     * @param name Name of the property.
     * @param value Value of the property
     */
    public <T extends Number> void addProperty(final Path alternative, final PixelCheck.Type type, final String name,
                                               final T value) {
        addProperty(alternative, type, name, value, "");
    }

    /**
     * Adds a property for the image.
     * @param <T> A Number
     * @param alternative Alternative that was being checked for which the property is being added.
     * @param type PixelCheck.Type that was being checked for which the log entry belongs.
     * @param name Name of the property.
     * @param value Value of the property
     * @param unit Unit associated with the property
     */
    public <T extends Number> void addProperty(final Path alternative, final PixelCheck.Type type, final String name,
                                               final T value,
                                               final String unit) {
        ArcticDiffProperty<T> property = new ArcticDiffNumberProperty<>(type, name, value, unit);
        properties.get(alternative).put(property.getKey(), property);
    }

    /**
     * Adds a property for the image.
     * @param alternative Alternative that was being checked for which the property is being added.
     * @param type PixelCheck.Type that was being checked for which the log entry belongs.
     * @param name Name of the property.
     * @param value Value of the property
     */
    public void addProperty(final Path alternative, final PixelCheck.Type type, final String name,
                            final String value) {
        ArcticDiffStringProperty property = new ArcticDiffStringProperty(type, name, value);
        properties.get(alternative).put(property.getKey(), property);
    }

    /**
     * Adds a global property for the image.
     * @param <T> A Number
     * @param name Name of the property.
     * @param value Value of the property
     */
    public <T extends Number> void addGlobalProperty(final String name, final T value) {
        addGlobalProperty(name, value, "");
    }

    /**
     * Adds a property for the image.
     * @param <T> A Number
     * @param name Name of the property.
     * @param value Value of the property
     * @param unit Unit associated with the property
     */
    public <T extends Number> void addGlobalProperty(final String name, final T value, final String unit) {
        globalProperties.put(name, new ArcticDiffNumberProperty<>(PixelCheck.Type.GLOBAL, name, value, unit));
    }

    /**
     * Adds a property for the image.
     * @param name Name of the property.
     * @param value Value of the property
     */
    public void addGlobalProperty(final String name, final String value) {
        globalProperties.put(name, new ArcticDiffStringProperty(PixelCheck.Type.GLOBAL, name, value));
    }

    /**
     * Returns the strict summary against an alternative.
     * @param alternative Alternative for which we want to retrieve the summary.
     * @return Summary of running the strict check.
     */
    public PixelCheck.PixelCheckSummary getStrictSummary(final Path alternative) {
        return strictSummaries.getOrDefault(alternative, null);
    }

    /**
     * Returns the fuzzy summary against an alternative.
     * @param alternative Alternative for which we want to retrieve the summary.
     * @return Summary of running the fuzzy check.
     */
    public PixelCheck.PixelCheckSummary getFuzzySummary(final Path alternative) {
        return fuzzySummaries.getOrDefault(alternative, null);
    }

    /**
     * Stores the strict summary result for one of the alternatives.
     * @param alternative Alternative for which we want to store the summary.
     * @param strictSummary Summary to store.
     */
    public void setStrictSummary(final Path alternative, final PixelCheck.PixelCheckSummary strictSummary) {
        strictSummaries.put(alternative, strictSummary);
    }

    /**
     * Stores the fuzzy summary result for one of the alternatives.
     * @param alternative Alternative for which we want to store the summary.
     * @param fuzzySummary Summary to store.
     */
    public void setFuzzySummary(final Path alternative, final PixelCheck.PixelCheckSummary fuzzySummary) {
        fuzzySummaries.put(alternative, fuzzySummary);
    }

    /**
     * Checks if a specific PixelCheck diff has already been executed for a specific alternative.
     * @param alternative alternative for which we want to check.
     * @param type The PixelCheck.Type we want to check.
     * @return True if the diff has already been executed
     */
    public boolean hasRun(final Path alternative, final PixelCheck.Type type) {
        return ranChecks.get(alternative).contains(type);
    }

    /**
     * Stores that the diff for a specific check has run for an alternative.
     * @param alternative Alternative for which we want to store the fact it has ran.
     * @param type The check type that was run.
     */
    public void recordDiff(final Path alternative, final PixelCheck.Type type) {
        ranChecks.get(alternative).add(type);
    }

    /**
     * Returns the different alternatives that are present for the image that we will review.
     * @return List with all the different alternatives.
     */
    public List<Path> getSavedImagePaths() {
        return alternatives;
    }

    /**
     * Returns the image that was captured when the test run.
     * @return Image that was captured when the test run.
     */
    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    /**
     * Set the image that was captured when the test was run. This is usually invoked after the image is loaded from
     * disk.
     * @param image Image we want to store.
     */
    public void setCurrentImage(final BufferedImage image) {
        this.currentImage = image;
    }

    /**
     * Returns the images (not hints) generated for a specific alternative.
     * @param alternative Alternative for which we want to retrieve the images.
     * @return Map with the images using the type as key.
     */
    public Map<PixelCheck.Type, BufferedImage> getImages(final Path alternative) {
        return images.get(alternative);
    }

    /**
     * Returns the hints generated for a specific alternative.
     * @param alternative Alternative for which we want to retrieve the hint.
     * @return Map with the hint using the type as key.
     */
    public  Map<PixelCheck.Type, BufferedImage> getHints(final Path alternative) {
        return hints.get(alternative);

    }

    /**
     * Retrieves the path where the image that was captured during replay is saved.
     * @return Path pointing to the file with the image
     */
    public Path getCurrentImagePath() {
        return currentImagePath;
    }

    /**
     * Level of confidence required by the confidence check.
     * @return Float representing the required confidence as a value between 0 and 1.
     */
    public float getTestConfidence() {
        return testConfidence;
    }

    /**
     * Retrieves the log messages generated during the execution of the PixelChecks for an alternative.
     * @param alternative Alternative for which we want to retrieve the log.
     * @return The log, as a list of strings.
     */
    public List<String> getLog(final Path alternative) {
        return log.getOrDefault(alternative, Collections.emptyList());
    }

    /**
     * Returns the properties for one alternative.
     * @param alternative Alternative for which we want to retrieve the properties.
     * @return A map containing all the properties for that alternative.
     */
    public Map<String, ArcticDiffProperty<?>> getProperties(final Path alternative) {
        return properties.getOrDefault(alternative, Collections.emptyMap());
    }

    /**
     * Returns the global properties.
     * @return A map containing all the global properties.
     */
    public Map<String, ArcticDiffProperty<?>> getGlobalProperties() {
        return globalProperties;
    }

    /**
     * Returns the position of the shades (referenced to the workbench).

    /**
     * Returns the position of the shades (referenced to the workbench).
     * @return List of the shades positions
     */
    public List<ScreenArea> getShades() {
        return shades;
    }

    /**
     * Whether the diff images have already been generated.
     * @return True if the diff images have been generated.
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Mark all the diff images as completed.
     */
    public void complete() {
        this.completed = true;
    }
}
