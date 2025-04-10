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
package com.amazon.corretto.arctic.player.backend.impl;

import java.security.NoSuchAlgorithmException;

import com.amazon.corretto.arctic.common.backend.ArcticHashCalculator;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.player.backend.ImageComparator;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HashImageComparator implements ImageComparator {
    private static final Logger log = LoggerFactory.getLogger(HashImageComparator.class);

    public static final String NAME = "hash";

    private final ArcticHashCalculator imageHasher;

    @Inject
    public HashImageComparator(final ArcticHashCalculator imageHasher) {
        this.imageHasher = imageHasher;
    }

    @Override
    public boolean compare(final ScreenshotCheck current, final ScreenshotCheck saved,
                           final TestId testId, final String scope) {
        try {
            final String currentHash = imageHasher.calculateHash(current.getImage(), saved.getHashMode());
            if (currentHash.equals(saved.getHashValue())
                    || (saved.getAlternativeHashes() != null && saved.getAlternativeHashes().contains(currentHash))) {
                return true;
            }
            log.warn("Expected {} but got hash {}", saved.getHashValue(), currentHash);
            return false;
        } catch (final NoSuchAlgorithmException e) {
            log.warn("Format {} is not a valid Digest format for image hashes", saved.getHashMode(), e);
            return false;
        }
    }
}
