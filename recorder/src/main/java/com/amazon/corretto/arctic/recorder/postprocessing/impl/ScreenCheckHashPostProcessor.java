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
package com.amazon.corretto.arctic.recorder.postprocessing.impl;

import java.security.NoSuchAlgorithmException;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.backend.ArcticHashCalculator;
import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.recorder.inject.InjectionKeys;
import com.amazon.corretto.arctic.recorder.postprocessing.ArcticRecorderPostProcessor;
import com.amazon.corretto.arctic.shared.exception.ArcticException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ScreenCheckHashPostProcessor implements ArcticRecorderPostProcessor {
    public static final String NAME = "scHash";
    private static final int PRIORITY = 40;

    private final ArcticHashCalculator digestCalculator;
    private final String hashAlgorithm;

    @Inject
    public ScreenCheckHashPostProcessor(final ArcticHashCalculator digestCalculator,
            @Named(InjectionKeys.POST_SC_HASH_ALGORITHM) final String hashAlgorithm) {
        this.digestCalculator = digestCalculator;
        this.hashAlgorithm = hashAlgorithm;
        log.debug("{} loaded", NAME);
    }

    @Override
    public boolean postProcess(final ArcticTest test) {
        if (test.getInitialSc() != null) postProcess(test.getInitialSc());
        test.getScreenChecks().forEach(this::postProcess);
        return true;
    }

    private void postProcess(final ScreenshotCheck it) {
        try {
            final String hashValue = digestCalculator.calculateHash(it.getImage(), hashAlgorithm);
            it.setHashValue(hashValue);
            it.setHashMode(hashAlgorithm);
        } catch (final NoSuchAlgorithmException e) {
            log.error("Invalid DigestAlgorithm provided: {}", hashAlgorithm, e);
            throw new ArcticException(hashAlgorithm + " is not a valid algorithm", e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    public int getPriority() {
        return PRIORITY;
    }
}
