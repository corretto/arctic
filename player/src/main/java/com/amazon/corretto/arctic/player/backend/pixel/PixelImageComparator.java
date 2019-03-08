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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.repository.TestSaveRepository;
import com.amazon.corretto.arctic.player.backend.ImageComparator;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
import com.amazon.corretto.arctic.player.model.PixelCheckFailure;
import com.amazon.corretto.arctic.player.results.ArcticScFailureKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.function.Predicate.not;

/**
 * A pixel level comparator. It can perform multiple checks to determine whether the two images are acceptable or not
 */
public final class PixelImageComparator implements ImageComparator {
    private static final Logger log = LoggerFactory.getLogger(PixelImageComparator.class);
    public static final String NAME = "pixel";

    private final TestSaveRepository saveRepository;
    private final boolean saveDiffs;
    private final boolean clearFolder;
    private final ArcticScFailureKeeper pixelCheckFailureKeeper;
    private final Path outFolder;
    private final List<PixelCheck> checks;

    /**
     * Creates a new instance with injected dependencies.
     * @param checks The different PixelChecks that will be executed during the comparison
     * @param saveDiffs Whether we want to generate images containing the differences between saved and current images
     *                  for future review
     * @param saveRepository Used to save the diff images into files
     * @param outFolderName Where to store the diff images
     * @param clearFolder Clear the folder that contains diff images during start up
     * @param pixelCheckFailureKeeper Used to stored the different failures encountered during execution for future
     *                                review
     */
    @Inject
    public PixelImageComparator(@Named(InjectionKeys.BACKEND_SC_PIXEL_CHECKS) final Set<PixelCheck> checks,
                                @Named(InjectionKeys.BACKEND_SC_PIXEL_SAVE) final boolean saveDiffs,
                                final TestSaveRepository saveRepository,
                                @Named(InjectionKeys.BACKEND_SC_PIXEL_SAVE_FOLDER) final String outFolderName,
                                @Named(InjectionKeys.BACKEND_SC_PIXEL_SAVE_CLEAR) final boolean clearFolder,
                                final ArcticScFailureKeeper pixelCheckFailureKeeper) {

        this.saveRepository = saveRepository;
        this.saveDiffs = saveDiffs;
        this.clearFolder = clearFolder;
        this.pixelCheckFailureKeeper = pixelCheckFailureKeeper;
        this.outFolder = Path.of(outFolderName);
        clearDiffResults();
        this.checks = checks.stream()
                .sorted(Comparator.comparing(PixelCheck::getPriority))
                .collect(Collectors.toList());
    }

    private void clearDiffResults() {
        if (saveDiffs && clearFolder && outFolder != null && !outFolder.toString().equals("")) {
            try {
                if (outFolder.toFile().exists()) {
                    Files.walk(outFolder)
                            .sorted(Comparator.reverseOrder())
                            .filter(it -> !it.equals(outFolder))
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            } catch (final Exception e) {
                log.warn("Unable to delete output folder {}", outFolder, e);
            }
        }
    }

    @Override
    public boolean compare(final ScreenshotCheck current, final ScreenshotCheck saved, final TestId testId,
                           final String scope) {
        // Check the principal ScreenshotCheck image
        final PixelCheckResult result = new PixelCheckResult(current, saved, testId, scope);
        boolean success = false;
        for (final Path alternative : result.getSavedImagePaths()) {
            final PixelCheckResult.Status status = doCompare(result, alternative);
            if (PixelCheckResult.Status.PASSED.equals(status)) {
                success = true;
            }
        }
        saved.setImage(result.getCurrentImage());
        saved.setHashValue(result.getCurrentHash());

        if (!success) {
            fail(result);
        }
        return success;
    }

    /**
     * Called whenever a pixel check failed, this method will handle the generation of diff images and reporting to the
     * {@link ArcticScFailureKeeper} if needed.
     * @param result PixelCheck that failed
     */
    private void fail(final PixelCheckResult result) {
        for (final Path alternative : result.getSavedImagePaths()) {
            final String failReason = result.getRanChecks(alternative).entrySet().stream()
                    .filter(not(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .map(PixelCheck.Type::getName)
                    .collect(Collectors.joining(","));
            log.debug("Failed {} due to: {}", alternative, failReason);
        }
        if (saveDiffs) {
            final Path absolutePath = outFolder.resolve(result.getMainSavedImagePath() + "."
                    + PixelCheck.Type.CURRENT.getName());
            final Path currentImagePath = saveRepository.saveImageAbsolutePath(absolutePath,
                    result.getCurrentImage()).getValue();
            final PixelCheckFailure failure = new PixelCheckFailure(result, outFolder, currentImagePath);
            pixelCheckFailureKeeper.addValue(failure.getFailureId(), failure);
        }
    }


    private PixelCheckResult.Status doCompare(final PixelCheckResult result, final Path alternative) {
        for (final PixelCheck check : checks) {
            check.check(result, alternative);
        }
        return result.getStatus();
    }

    /**
     * Generates the diffs for all the alternatives for a given ArcticDiffImages. Method is thread safe and idempotent.
     * @param diffImages ArcticDiffImages for which to generate the diffs and hints.
     */
    public void generateDiff(final ArcticDiffImages diffImages) {
        if (diffImages.isCompleted()) {
            return;
        }
        synchronized (diffImages) {
            log.debug("Generating diffs for {}", diffImages.getFailureId());
            if (!diffImages.isCompleted()) {
                long globalStart = System.currentTimeMillis();
                diffImages.getSavedImagePaths().parallelStream().forEach(alternative -> {
                    long altStart = System.currentTimeMillis();
                    for (final PixelCheck check : checks) {
                        check.generateDiff(alternative, diffImages);
                    }
                    long altTime = System.currentTimeMillis() - altStart;
                    diffImages.addProperty(alternative, PixelCheck.Type.ALTERNATIVE, "time", altTime, "ms");
                });
                long globalTime = System.currentTimeMillis() - globalStart;
                diffImages.addGlobalProperty("time", globalTime, "ms");
                diffImages.complete();
            }
        }
    }
}
