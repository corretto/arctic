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
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import com.amazon.corretto.arctic.common.backend.ArcticHashCalculator;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheckResult;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pixel check that in reality, it doesn't really check the pixels. It compares the hash of the recorded and current
 * image, and if they match, they are assumed to be the same. For OS without random shadows, this is the fastest way
 * to check images.
 */
public final class HashPixelCheck implements PixelCheck {
    private static final Logger log = LoggerFactory.getLogger(HashPixelCheck.class);
    public static final Type NAME = Type.HASH;
    private final ArcticHashCalculator hashCalculator;

    /**
     * Creates a new instance, usually called by the dependency injection software.
     * @param hashCalculator Used to calculate the hash of the image.
     */
    @Inject
    public HashPixelCheck(final ArcticHashCalculator hashCalculator) {
        this.hashCalculator = hashCalculator;
    }


    @Override
    public boolean isSufficient() {
        return true;
    }

    @Override
    public int getPriority() {
        return 0;
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
        try {
            if (result.getCurrentHash() != null || result.getCurrentImage() == null) {
                // No need to check again
                return false;
            }
            final String currentHash = hashCalculator.calculateHash(result.getCurrentImage(), result.getHashMode());
            result.setCurrentHash(currentHash);
            log.trace("Calculated hash: {}", currentHash);
            return result.isValidHash(currentHash);
        } catch (final NoSuchAlgorithmException e) {
            log.warn("Format {} is not a valid Digest format for image hashes", result.getHashMode(), e);
            return false;
        }
    }

    @Override
    public void doDiff(final Path alternative, final ArcticDiffImages diffImages) {
        diffImages.addProperty(alternative, NAME, "hashMode", diffImages.getHashMode());
        diffImages.addProperty(alternative, NAME, "hash", diffImages.getCurrentHash());
        log.trace("doDiff called");
    }
}
