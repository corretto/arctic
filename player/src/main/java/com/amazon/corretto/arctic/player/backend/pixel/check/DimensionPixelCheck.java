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
import java.util.List;

import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pixel check that compares two images size. The check fails if both images do not have the same width and length.
 * Passing this check is not enough to consider both images equal (but failing is enough to fail further checks.
 */
public final class DimensionPixelCheck implements PixelCheck {
    private static final Logger log = LoggerFactory.getLogger(DimensionPixelCheck.class);
    private static final int PRIORITY = 20;
    public static final Type NAME = Type.DIMENSION;

    @Override
    public boolean isSufficient() {
        return false;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public Type getType() {
        return NAME;
    }

    @Override
    public List<Type> getDependencyChecks() {
        return List.of(Type.RECORDED);
    }

    @Override
    public boolean doCheck(final PixelCheckResult result, final Path alternative) {
        final BufferedImage current = result.getCurrentImage();
        if (current == null) {
            log.debug("Current image is null");
            return false;
        }

        final BufferedImage saved = result.getSavedImage(alternative);
        if (saved == null) {
            log.debug("Recorded image {} is null", alternative);
            return false;
        }

        if ((current.getWidth() != saved.getWidth()) || current.getHeight() != saved.getHeight()) {
            log.debug("Current image dimensions ({}x{}) do not match recorded dimensions ({}x{})", current.getWidth(),
                    current.getHeight(), saved.getWidth(), saved.getHeight());
            return false;
        }
        return true;
    }

    @Override
    public void doDiff(final Path alternative, final ArcticDiffImages diffImages) {
        if (diffImages.getCurrentImage() == null) {
            diffImages.addLog(alternative, NAME, "Current image is null");
            return;
        }

        if (!diffImages.getImages(alternative).containsKey(Type.RECORDED)) {
            diffImages.addLog(alternative, NAME, "Recorded image is null");
            return;
        }

        final BufferedImage current = diffImages.getCurrentImage();
        final BufferedImage saved = diffImages.getImages(alternative).get(Type.RECORDED);
        boolean passed = true;
        if ((current.getWidth() != saved.getWidth()) || current.getHeight() != saved.getHeight()) {
            diffImages.addLog(alternative, NAME, String.format("Current image dimensions (%sx%s) do not match recorded "
                            + "dimensions (%sx%s)", current.getWidth(),
                    current.getHeight(), saved.getWidth(), saved.getHeight()));
            passed = false;
        }
        int totalPixels = diffImages.getCurrentImage().getWidth() * diffImages.getCurrentImage().getHeight();
        diffImages.addProperty(alternative, NAME, "passed", String.valueOf(passed));
        diffImages.addProperty(alternative, NAME, "width", diffImages.getCurrentImage().getWidth(), "px");
        diffImages.addProperty(alternative, NAME, "height", diffImages.getCurrentImage().getHeight(), "px");
        diffImages.addProperty(alternative, NAME, "total", totalPixels, "px");

    }
}
