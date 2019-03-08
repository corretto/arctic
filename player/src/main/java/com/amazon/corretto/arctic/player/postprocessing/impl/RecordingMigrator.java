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
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.repository.TestSaveRepository;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.postprocessing.ArcticPlayerPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class saves the test back into the disk as a fresh new test. This has several implications:
 * - Test is cleaned (may cause all files in the test folder to be deleted)
 * - All alternatives are discarded
 * - Images for the test will be replaced with the ones captured on our current run
 * - Current overrides are applied to the test
 */
public final class RecordingMigrator implements ArcticPlayerPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(RecordingMigrator.class);
    private static final int PRIORITY = 90;
    public static final String NAME = "migrator";
    private final TestSaveRepository repository;
    private final boolean migrate;

    /**
     * Creates a new instance of the migrator. Called by the dependency injection framework.
     * @param repository A repository to save the migrated tests to.
     * @param migrate True if we want to migrate the tests. False to disable migration.
     */
    @Inject
    public RecordingMigrator(final TestSaveRepository repository,
                             @Named(InjectionKeys.POST_MIGRATE) final boolean migrate) {
        this.repository = repository;
        this.migrate = migrate;
    }

    @Override
    public boolean postProcess(final ArcticRunningTest test) {
        log.debug("Post processing migration");
        if (migrate) {
            log.info("Migrating test {}", test.getTestId());
            ArcticTest recording = test.getRecording();
            repository.removeTestCase(recording.getTestId(), recording.getScope());
            Stream.concat(Stream.of(recording.getInitialSc()), recording.getScreenChecks().stream())
                    .filter(it -> it.getImage() != null)
                    .forEach(it -> {
                        it.getAlternativeImages().clear();
                        it.getAlternativeHashes().clear();
                        repository.saveImage(recording.getTestName(), recording.getTestCase(),
                                recording.getScope(), getScName(it.getFilename()), it.getImage());
                    });
            return repository.saveTestCase(test.getRecording(), true);
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
        return Set.of(TestStatusCode.CONFIRMED, TestStatusCode.FAILED);
    }

    private String getScName(final Path scPath) {
        return (scPath.getFileName().toString().split("\\."))[0];
    }
}
