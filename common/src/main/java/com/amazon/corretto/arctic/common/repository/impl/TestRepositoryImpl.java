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

package com.amazon.corretto.arctic.common.repository.impl;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.repository.TestLoadRepository;
import com.amazon.corretto.arctic.common.repository.TestRepository;
import com.amazon.corretto.arctic.common.repository.TestSaveRepository;
import com.amazon.corretto.arctic.common.util.Pair;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a full TestRepository based on the injected Load and Save repositories. Recording only requires
 * a load repository, and it is possible to do playback with just a save repository, but some options like the review
 * require the use of both at the same time.
 */
public final class TestRepositoryImpl implements TestRepository {
    private static final Logger log = LoggerFactory.getLogger(TestRepositoryImpl.class);

    private final TestLoadRepository loadRepository;
    private final TestSaveRepository saveRepository;

    /**
     * Implements a full repository by relying on an existing load repository and a save repository.
     * @param loadRepository instance of the load repository to use.
     * @param saveRepository instance of the save repository to use.
     */
    @Inject
    public TestRepositoryImpl(final TestLoadRepository loadRepository, final TestSaveRepository saveRepository) {
        this.loadRepository = loadRepository;
        this.saveRepository = saveRepository;
    }

    @Override
    public Optional<ArcticTest> getTestCase(final TestId testId) {
        return loadRepository.getTestCase(testId);
    }

    @Override
    public Optional<ArcticTest> getTestCase(final String scope, final TestId testId) {
        return loadRepository.getTestCase(scope, testId);
    }

    @Override
    public void loadTestEvents(final ArcticTest test) {
        loadRepository.loadTestEvents(test);
    }

    @Override
    public BufferedImage loadImage(final String scope, final Path imgPath) {
        return loadRepository.loadImage(scope, imgPath);
    }

    @Override
    public BufferedImage loadImageAbsolutePath(final Path imgPath) {
        return loadRepository.loadImageAbsolutePath(imgPath);
    }

    @Override
    public boolean contains(final TestId testId) {
        return loadRepository.contains(testId);
    }

    @Override
    public boolean saveTestCase(final ArcticTest test) {
        return saveRepository.saveTestCase(test);
    }

    @Override
    public boolean saveTestCase(final ArcticTest test, final boolean includeEvents) {
        return saveRepository.saveTestCase(test, includeEvents);
    }

    @Override
    public Path copyAlternativeImage(final String scope, final ScreenshotCheck sc,
                                     final Path alternativeImageAbsolutePath) {
        return saveRepository.copyAlternativeImage(scope, sc, alternativeImageAbsolutePath);
    }

    @Override
    public boolean removeTestCase(final TestId testId, final String scope) {
        return saveRepository.removeTestCase(testId, scope);
    }

    @Override
    public Pair<String, Path> saveImageAbsolutePath(final Path imagePath, final BufferedImage image) {
        return saveRepository.saveImageAbsolutePath(imagePath, image);
    }

    @Override
    public Pair<String, Path> saveImage(final String testName, final String testCase, final String scope,
                                        final String imgName, final BufferedImage image) {
        return saveRepository.saveImage(testName, testCase, scope, imgName, image);
    }

    @Override
    public boolean addAlternative(final TestId testId, final String scope, final Path scPath, final String scHash,
                                  final Path alternativePath, final String alternativeHash) {
        log.debug("Adding to {} for {} ---- {}", testId, scPath, alternativePath);
        if (alternativeHash == null || "".equals(alternativeHash)) {
            log.warn("Attempted to add an alternative to {} sc {}, but the hash was null", testId, scPath);
            return false;
        }
        if (alternativePath == null) {
            log.warn("Attempted to add an alternative to {} sc {}, but the alternative was null", testId, scPath);
            return false;
        }

        final Optional<ArcticTest> test = loadRepository.getTestCase(scope, testId);
        if (test.isEmpty()) {
            log.warn("Attempted to add an alternative to {} sc {}, but the test couldn't be loaded", testId, scPath);
            return false;
        }

        final ScreenshotCheck sc = Stream.concat(Stream.of(test.get().getInitialSc()),
                        test.get().getScreenChecks().stream())
                .filter(Objects::nonNull)
                .filter(it -> it.getFilename().equals(scPath))
                .findAny().orElse(null);

        if (sc == null) {
            log.warn("Attempted to add an alternative {} sc {}, but the screenshot check wasn't found", testId, scPath);
            return false;
        }

        if (sc.getHashValue() == null || !sc.getHashValue().equals(scHash)) {
            log.warn("Attempted to add an alternative to {} sc {}, but hash {} of the sc did not match {}", testId,
                    sc.getFilename(), scHash, sc.getHashValue());
            return false;
        }

        if (sc.getAlternativeHashes().contains(alternativeHash)) {
            log.warn("Ignoring duplicate alternative {}", alternativePath);
            return false;
        }

        final Path destination = saveRepository.copyAlternativeImage(scope, sc, alternativePath);
        if (destination == null) {
            log.warn("Attempted to add an alternative {} sc {}, but failed to copy the alternative {}", testId, scPath,
                    alternativePath);
            return false;
        }

        sc.getAlternativeHashes().add(alternativeHash);
        sc.getAlternativeImages().add(destination);
        return saveRepository.saveTestCase(test.get(), false);
    }
}
