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

import java.nio.file.Path;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic confidence check that calculates how many pixels in the current image differ from the pixels in the recorded
 * image. Failing a confidence check will automatically fail the whole image comparison, but passing a confidence
 * check is not enough. Another check that looks at the specific pixels is needed.
 */
public final class ConfidencePixelCheck implements PixelCheck {
    public static final Type NAME = Type.CONFIDENCE;

    private static final Logger log = LoggerFactory.getLogger(ConfidencePixelCheck.class);
    private static final int PRIORITY = 40;

    private final float globalConfidence;

    /**
     * Creates a new instance, usually called by the dependency injector.
     * @param globalConfidence A general confidence applied to all tests. Defined by the arctic properties.

     */
    @Inject
    public ConfidencePixelCheck(@Named(InjectionKeys.BACKEND_SC_PIXEL_CONFIDENCE_MIN) final float globalConfidence) {
        this.globalConfidence = globalConfidence;
    }

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
        return List.of(Type.RECORDED, Type.DIMENSION, Type.STRICT);
    }

    @Override
    public boolean doCheck(final PixelCheckResult result, final Path alternative) {
        final PixelCheckSummary summary = result.getStrictSummary();

        log.trace("Total pixels: {}", summary.getTotalPixels());
        log.trace("Failed pixels: {}", summary.getTotalFailedPixels());
        log.trace("Confidence: {}", summary.getConfidence());

        if (summary.getConfidence() < globalConfidence) {
            log.trace("Confidence below global threshold {}", globalConfidence);
            return false;
        }

        if (summary.getConfidence() < result.getTestConfidence()) {
            log.trace("Confidence below test threshold {}", result.getTestConfidence());
            return false;
        }

        return true;
    }

    @Override
    public void doDiff(final Path alternative, final ArcticDiffImages diffImages) {
        final PixelCheckSummary summary = diffImages.getStrictSummary(alternative);
        boolean passed = summary.getConfidence() >= diffImages.getTestConfidence();
        diffImages.addProperty(alternative, NAME, "passed", String.valueOf(passed));
        diffImages.addProperty(alternative, NAME, "globalThreshold", globalConfidence);
        diffImages.addProperty(alternative, NAME, "testThreshold", diffImages.getTestConfidence());

        if (summary.getConfidence() < globalConfidence) {
            diffImages.addLog(alternative, NAME, "Confidence below global threshold");
        }

        if (summary.getConfidence() < diffImages.getTestConfidence()) {
            diffImages.addLog(alternative, NAME, "Confidence below test threshold");
        }
    }

}
