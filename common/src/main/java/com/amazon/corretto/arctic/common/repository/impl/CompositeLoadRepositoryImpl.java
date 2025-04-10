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
import java.util.LinkedHashMap;
import java.util.Optional;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.repository.TestLoadRepository;
import jakarta.inject.Inject;

/**
 * This is a TestLoadRepository that supports multiple scopes and priorities. It does that by keeping one instance of
 * a repository for each instance. Whenever it needs to load a test, it will check all the repositories in order until
 * it finds one that has the test.
 */
public final class CompositeLoadRepositoryImpl implements TestLoadRepository {
    private final LinkedHashMap<String, TestLoadRepository> repositories;

    /**
     * Constructor for the CompositeLoadRepositoryImpl. It receives a LinkedHashMap with the different repositories,
     * with the order of being
     * @param repositories
     */
    @Inject
    public CompositeLoadRepositoryImpl(final LinkedHashMap<String, TestLoadRepository> repositories) {
        this.repositories = repositories;
    }

    @Override
    public BufferedImage loadImageAbsolutePath(final Path imgPath) {
        return repositories.values().stream().findAny().map(it -> it.loadImageAbsolutePath(imgPath)).orElse(null);
    }

    @Override
    public boolean contains(final TestId testId) {
        return repositories.values().stream().anyMatch(it -> it.contains(testId));
    }

    @Override
    public Optional<ArcticTest> getTestCase(final TestId testId) {
        return repositories.values().stream()
                .map(it -> it.getTestCase(testId))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public Optional<ArcticTest> getTestCase(final String scope, final TestId testId) {
        return Optional.ofNullable(repositories.get(scope)).flatMap(it -> it.getTestCase(testId));
    }

    @Override
    public void loadTestEvents(final ArcticTest test) {
        repositories.get(test.getScope()).loadTestEvents(test);
    }

    @Override
    public BufferedImage loadImage(final String scope, final Path imgPath) {
        return repositories.get(scope).loadImage(scope, imgPath);
    }
}
