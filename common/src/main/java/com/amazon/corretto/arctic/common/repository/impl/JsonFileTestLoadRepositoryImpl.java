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
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.model.event.Events;
import com.amazon.corretto.arctic.common.repository.TestLoadRepository;
import com.amazon.corretto.arctic.api.exception.ArcticException;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

/**
 * This class implements the {@link TestLoadRepository} using json files to load the different tests and events. This
 * class should be paired with the {@link JsonFileTestSaveRepositoryImpl} that will save those files.
 */
@Slf4j
public final class JsonFileTestLoadRepositoryImpl implements TestLoadRepository {

    private final Path repositoryPath;
    private final String scope;
    private final Gson gson;
    private final String testFileName;

    /**
     * Constructor for the load repository. This is being called by the dependency injection framework. Multiple
     * of this class will all coexists, each one with a different scope.
     *
     * @param rootPath a folder representing the start of the repository.
     * @param testFileName which name is used for the actual test file. By default, `Test.json`.
     * @param scope The scope this instance reads. The scope is represented as a subfolder inside the rootPath.
     * @param gson An instance of gson used to deserialize test and event files.
     */
    public JsonFileTestLoadRepositoryImpl(final String rootPath, final String testFileName, final String scope,
                                          final Gson gson) {
        this.repositoryPath = Path.of(rootPath).resolve(scope);
        this.scope = scope;
        this.gson = gson;
        this.testFileName = testFileName;
    }

    /**
     * Searches for the test at repositoryRoot/scope/testName/testCase/testFileName.
     *
     * @param testId Id of the test to load.
     * @return Null if the test is not found. If found, the returned ArcticTest will contain the scope where it was
     * found
     */
    @Override
    public Optional<ArcticTest> getTestCase(final TestId testId) {
        Path testCasePath = getTestCasePath(testId);
        if (Files.exists(testCasePath)) {
            final ArcticTest test = getTestCaseFromPath(testCasePath);
            if (test != null) {
                // Stamp the test with the scope it was loaded from.
                test.setScope(scope);
                return Optional.of(test);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ArcticTest> getTestCase(final String testScope, final TestId testId) {
        return scope.equals(testScope) ? getTestCase(testId) : Optional.empty();
    }

    @Override
    public void loadTestEvents(final ArcticTest test) {
        if (test.isZip()) {
            loadCompressed(test);
        } else {
            loadUncompressed(test);
        }
    }

    @Override
    public BufferedImage loadImage(final String imageScope, final Path imgPath) {
        if (!imageScope.equals(scope)) {
            throw new ArcticException(String.format("Wrong load repository (%s) assigned to load image from %s", scope,
                    imageScope));
        }
        Path fullPath = repositoryPath.resolve(imgPath);
        try {
            if (fullPath.toFile().exists()) {
                return ImageIO.read(fullPath.toFile());
            }
        } catch (final IOException e) {
            log.warn("Unable to read image {}", imgPath, e);
        }
        return null;
    }

    @Override
    public BufferedImage loadImageAbsolutePath(final Path imgPath) {
        try {
            if (imgPath.toFile().exists()) {
                return ImageIO.read(imgPath.toFile());
            }
        } catch (final IOException e) {
            log.warn("Unable to read image {}", imgPath, e);
        }
        return null;
    }

    @Override
    public boolean contains(final TestId testId) {
        return Files.exists(getTestCasePath(testId));
    }

    private ArcticTest getTestCaseFromPath(final Path testCasePath) {
        log.debug("Loading recording {}", testCasePath);
        try (FileReader fr = new FileReader(testCasePath.toFile())) {
            return gson.fromJson(fr, ArcticTest.class);
        } catch (IOException e) {
            log.warn("IO Error when reading test case {}", testCasePath, e);
        } catch (RuntimeException e) {
            log.warn("Error when reading test case {}", testCasePath, e);
        }
        return null;
    }

    private void loadUncompressed(final ArcticTest test) {
        final Path eventsPath = getTestCaseFolderPath(test.getTestId())
                .resolve(test.getEventsFile());
        try (FileReader reader = new FileReader(eventsPath.toFile())) {
            final Events events = gson.fromJson(reader, Events.class);
            test.setEvents(events);
        } catch (IOException e) {
            log.error("Unable to read events for {}:{} in {}", test.getTestName(), test.getTestCase(), eventsPath);
            throw new ArcticException("Bad test recording, unable to load Events file", e);
        }
    }

    private void loadCompressed(final ArcticTest test) {
        final Path eventsFilePath;
        if (test.getEventsFile().toLowerCase().endsWith(".zip")) {
            eventsFilePath = getTestCaseFolderPath(test.getTestId())
                    .resolve(test.getEventsFile());
        } else {
            // The events file might be the compressed or uncompressed name, but the file will always end with .zip
            eventsFilePath = getTestCaseFolderPath(test.getTestId())
                    .resolve(test.getEventsFile() + ".zip");
        }
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(eventsFilePath.toFile()));
             InputStreamReader isr = new InputStreamReader(zis)) {
            zis.getNextEntry();
            Events events = gson.fromJson(isr, Events.class);
            test.setEvents(events);
        } catch (IOException e) {
            log.error("Unable to read events for {}:{} in {}", test.getTestName(), test.getTestCase(), eventsFilePath);
            throw new ArcticException("Bad test recording, unable to load Events file", e);
        }
    }

    private Path getTestCaseFolderPath(final TestId testId) {
        return repositoryPath.resolve(getTestShortName(testId.getTestClass())).resolve(testId.getTestCase());
    }

    private Path getTestCasePath(final TestId testId) {
        return getTestCaseFolderPath(testId).resolve(testFileName);
    }

    private String getTestShortName(final String testName) {
        final String[] tokens = testName.split("#");
        return tokens[tokens.length - 1];
    }
}
