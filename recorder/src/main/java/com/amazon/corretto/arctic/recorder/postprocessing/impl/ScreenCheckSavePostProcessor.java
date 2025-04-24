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

import java.nio.file.Path;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.repository.TestSaveRepository;
import com.amazon.corretto.arctic.common.util.Pair;
import com.amazon.corretto.arctic.recorder.postprocessing.ArcticRecorderPostProcessor;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * This postprocessor saves a copy of the ScreenChecks as images. They can be used for pixel by pixel comparison and
 * are useful for humans.
 */
@Slf4j
public final class ScreenCheckSavePostProcessor implements ArcticRecorderPostProcessor {
    public static final String NAME = "scSave";
    private static final int PRIORITY = 50;

    private final TestSaveRepository repository;

    /**
     * Creates a new instance of the post processor. Called by the dependency injection framework.
     * @param repository A repository that is used to store the ScreenChecks.
     */
    @Inject
    public ScreenCheckSavePostProcessor(final TestSaveRepository repository) {
        this.repository = repository;
        log.debug("{} loaded", NAME);
    }

    @Override
    public boolean postProcess(final ArcticTest test) {
        if (test.getInitialSc() != null)
            postProcess(test.getInitialSc(), test.getTestName(), test.getTestCase(), test.getScope(), String.valueOf(0));
        int scCount = 1;
        for (final ScreenshotCheck sc : test.getScreenChecks()) {
            postProcess(sc, test.getTestName(), test.getTestCase(), test.getScope(), String.valueOf(scCount));
            scCount++;
        }
        return true;
    }

    private void postProcess(final ScreenshotCheck sc, final String testName, final String testCase, final String scope,
                             final String imgName) {
        final Pair<String, Path> savedImage = repository.saveImage(testName, testCase, scope, imgName, sc.getImage());
        sc.setFormat(savedImage.getKey());
        sc.setFilename(savedImage.getValue());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
