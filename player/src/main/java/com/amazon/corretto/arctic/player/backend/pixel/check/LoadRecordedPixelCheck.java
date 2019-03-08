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

package com.amazon.corretto.arctic.player.backend.pixel.check;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import com.amazon.corretto.arctic.common.repository.TestLoadRepository;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
import com.amazon.corretto.arctic.shared.exception.ArcticException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pixel check that verifies for an image a recorded image exists. This pixel check will load it if present, and fail if
 * not.
 */
public final class LoadRecordedPixelCheck implements PixelCheck {
    public static final PixelCheck.Type NAME = PixelCheck.Type.RECORDED;

    private static final Logger log = LoggerFactory.getLogger(LoadRecordedPixelCheck.class);

    private final TestLoadRepository testLoadRepository;

    /**
     * Creates a new instance of the object. Usually called by the dependency injection software.
     * @param testLoadRepository A repository that is used to load the recorded image.
     */
    @Inject
    public LoadRecordedPixelCheck(final TestLoadRepository testLoadRepository) {
        this.testLoadRepository = testLoadRepository;
    }

    @Override
    public boolean isSufficient() {
        return false;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public Type getType() {
        return NAME;
    }

    @Override
    public List<Type> getDependencyChecks() {
        return Collections.emptyList();
    }

    @Override
    public boolean doCheck(final PixelCheckResult result, final Path alternative) {
        log.trace("doCheck. Result: {}", result);
        final BufferedImage img = testLoadRepository.loadImage(result.getScope(), alternative);
        if (img == null) {
            log.error("Error loading recording image {}", alternative);
            return false;
        }
        result.putSavedImage(alternative, img);
        return true;
    }

    @Override
    public void doDiff(final Path alternative, final ArcticDiffImages diffImages) {
        if (diffImages.getCurrentImage() == null) {
            // Load the current image if it has not been done before
            final BufferedImage currentImage = testLoadRepository.loadImageAbsolutePath(
                    diffImages.getCurrentImagePath());

            if (currentImage == null) {
                throw new ArcticException(String.format("Current image is null. Image path: %s",
                        diffImages.getCurrentImagePath()));
            }

            diffImages.setCurrentImage(currentImage);
        }

        diffImages.getImages(alternative).put(Type.CURRENT, diffImages.getCurrentImage());

        // Load the specific alternative
        final BufferedImage recordedImg = testLoadRepository.loadImage(diffImages.getScope(), alternative);

        if (recordedImg == null) {
            throw new ArcticException(String.format("Recorded alternative image is null. Scope: %s, alternative: %s",
                    diffImages.getScope(),
                    alternative));
        }

        diffImages.getImages(alternative).put(Type.RECORDED, recordedImg);
    }
}
