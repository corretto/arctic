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

package com.amazon.corretto.arctic.player.postprocessing.impl;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.repository.TestSaveRepository;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.postprocessing.ArcticPlayerPostProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * This post processor will save a recording back into the disk. This is useful to store the overrides that are applied
 * at the player level as new test values. This post-processor does not save the events of the test, nor it modifies the
 * different images. Only confirmed tests are saved
 */
public final class TestAutoUpdater implements ArcticPlayerPostProcessor {
    public static final String NAME = "autoUpdater";
    private static final int PRIORITY = 80;
    private final TestSaveRepository repository;
    private final boolean autoSave;

    /**
     * Creates a new instance of this post-processor. Called by the dependency injection framework.
     * @param repository Save repository to use
     * @param autoSave Whether we want to save the test or not. A way to disable this post-processor without editing
     *                 the post-processing pipeline.
     */
    @Inject
    public TestAutoUpdater(final TestSaveRepository repository,
                           @Named(InjectionKeys.POST_AUTO_UPDATER_SAVE) final boolean autoSave) {
        this.repository = repository;
        this.autoSave = autoSave;
    }

    @Override
    public boolean postProcess(final ArcticRunningTest test) {
        if (autoSave) {
            ArcticTest recording = test.getRecording();
            Stream.concat(Stream.of(recording.getInitialSc()), recording.getScreenChecks().stream())
                    .filter(Objects::nonNull)
                    .filter(it -> it.getImage() != null)
                    .forEach(it -> repository.saveImage(recording.getTestName(), recording.getTestCase(),
                            recording.getScope(), getScName(it.getFilename()), it.getImage()));
            return repository.saveTestCase(test.getRecording(), false);
        }
        return true;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Set<TestStatusCode> getRegisteredStatuses() {
        return Set.of(TestStatusCode.CONFIRMED);
    }

    private String getScName(final Path scPath) {
        return (scPath.getFileName().toString().split("\\."))[0];
    }
}
