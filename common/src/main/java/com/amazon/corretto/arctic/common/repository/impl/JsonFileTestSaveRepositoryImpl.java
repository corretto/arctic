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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.backend.ArcticImageSaver;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.repository.TestSaveRepository;
import com.amazon.corretto.arctic.common.util.Pair;
import com.amazon.corretto.arctic.shared.exception.ArcticException;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

/**
 * This class implements the {@link TestSaveRepository} using json files to save the different tests and events. This
 * class should be paired with the {@link JsonFileTestLoadRepositoryImpl} that will load those files.
 */
@Slf4j
public final class JsonFileTestSaveRepositoryImpl implements TestSaveRepository {
    private final Path rootPath;
    private final Gson gson;
    private final String testFileName;
    private final ArcticImageSaver imageSaver;

    /**
     * Constructor for the instance, called by the dependency injection framework. There should be only one instance of
     * this class in the application, as we are capable of saving into all the different scopes (compared to the load
     * repository than checks only one).
     * @param rootPath a folder representing the start of the repository.
     * @param testFileName which name is used for the actual test file. By default, `Test.json`.
     * @param imageSaver used to save the BufferedImages into disk.
     * @param gson An instance of gson used to serialize test and event files.
     */
    @Inject
    public JsonFileTestSaveRepositoryImpl(@Named(CommonInjectionKeys.REPOSITORY_JSON_PATH) final String rootPath,
                                          @Named(CommonInjectionKeys.REPOSITORY_JSON_NAME) final String testFileName,
                                          final ArcticImageSaver imageSaver,
                                          final Gson gson) {
        this.rootPath = Path.of(rootPath);
        this.gson = gson;
        this.testFileName = testFileName;
        this.imageSaver = imageSaver;
    }

    @Override
    public boolean saveTestCase(final ArcticTest test) {
        return saveTestCase(test, true);
    }
    @Override

    public boolean saveTestCase(final ArcticTest test, final boolean includeEvents) {
        log.debug("Saving test {}:{}:{} with {}:{}:{} events", test.getScope(), test.getTestName(), test.getTestCase(),
                test.getScreenChecks().size(), test.getEvents().getMouseEvents().size(),
                test.getEvents().getKeyboardEvents().size());
        final Path testCaseFolder = rootPath.resolve(test.getScope()).resolve(getTestShortName(test.getTestName()))
                .resolve(test.getTestCase());
        testCaseFolder.toFile().mkdirs();
        if (includeEvents) {
            if (test.isZip()) {
                saveCompressed(testCaseFolder, test.getEventsFile(), test.getEvents());
            } else {
                save(testCaseFolder, test.getEventsFile(), test.getEvents());
            }
        }
        save(testCaseFolder, testFileName, test);
        return true;
    }

    @Override
    public Path copyAlternativeImage(final String scope, final ScreenshotCheck sc,
                                     final Path alternativeImageAbsolutePath) {
        if (scope != null) {
            final Path destinationPath = rootPath.resolve(scope).resolve(getNextAlternativeImageName(sc));
            try {
                log.debug("Copying {} to {}", alternativeImageAbsolutePath, destinationPath);
                Files.copy(alternativeImageAbsolutePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException e) {
                log.warn("Unable to copy {} to {}", alternativeImageAbsolutePath, destinationPath, e);
                return null;
            }
            return rootPath.resolve(scope).relativize(destinationPath);
        }
        return null;
    }

    @Override
    public boolean removeTestCase(final TestId testId, final String scope) {
        final Path folderPath = rootPath.resolve(scope).resolve(testId.getTestClass()).resolve(testId.getTestCase());
        return Arrays.stream(Objects.requireNonNull(folderPath.toFile().listFiles())).allMatch(File::delete);
    }

    @Override
    public Pair<String, Path> saveImage(final String testName, final String testCase, final String scope,
                                        final String imgName, final BufferedImage image) {
        final Path folder = rootPath.resolve(scope);
        final Path output = imageSaver.saveImage(image, folder, Path.of(testName, testCase, imgName));
        return Pair.of(imageSaver.getFormat(), output);
    }

    @Override
    public Pair<String, Path> saveImageAbsolutePath(final Path imagePath, final BufferedImage image) {
        final Path output = imageSaver.saveImage(image, imagePath.getParent(), imagePath.getFileName());
        return Pair.of(imageSaver.getFormat(), imagePath.getParent().resolve(output));
    }

    private Path getNextAlternativeImageName(final ScreenshotCheck sc) {
        final Path imagePath = sc.getFilename();
        final Path folderPath = imagePath.getParent();
        final String originalImageFileName = sc.getFilename().getFileName().toString().split("\\.")[0];
        return folderPath.resolve(String.format("%s_%d.%s", originalImageFileName, sc.getAlternativeImages().size() + 1,
                sc.getFormat()));
    }

    private void saveCompressed(final Path path, final String fileName, final Object data) {
        final String compressedFileName;
        final String uncompressedFileName;
        if (fileName.endsWith(".zip")) {
            compressedFileName = fileName;
            uncompressedFileName = fileName.substring(0, fileName.length() - 4);
        } else {
            compressedFileName = fileName + ".zip";
            uncompressedFileName = fileName;
        }
        final Path fullFileName = path.resolve(compressedFileName);
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fullFileName.toFile()));
             OutputStreamWriter osw = new OutputStreamWriter(zos)) {
            zos.putNextEntry(new ZipEntry(uncompressedFileName));
            gson.toJson(data, osw);
        } catch (final IOException e) {
            log.error("Unable to save test data in path {}", fullFileName);
            throw new ArcticException("Unable to save tests data", e);
        }
    }

    private void save(final Path path, final String fileName, final Object data) {
        final Path fullFileName = path.resolve(fileName);
        try (Writer writer = new FileWriter(fullFileName.toFile())) {
            gson.toJson(data, writer);
        } catch (final IOException e) {
            log.error("Unable to save test in path {}", fullFileName);
            throw new ArcticException("Unable to save tests", e);
        }
    }

    private String getTestShortName(final String testName) {
        final String[] tokens = testName.split("#");
        return tokens[tokens.length - 1];
    }
}
