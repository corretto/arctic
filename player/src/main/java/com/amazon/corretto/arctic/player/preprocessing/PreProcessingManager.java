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

package com.amazon.corretto.arctic.player.preprocessing;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PreProcessingManager {
    private static final Logger log = LoggerFactory.getLogger(PreProcessingManager.class);

    private final List<ArcticPlayerPreProcessor> preProcessors;

    @Inject
    public PreProcessingManager(final Set<ArcticPlayerPreProcessor> preProcessors) {
        this.preProcessors = preProcessors.stream()
                .sorted(Comparator.comparing(ArcticPlayerPreProcessor::getPriority))
                .collect(Collectors.toList());
        log.debug("Loaded pre-processors {}", preProcessors);
    }

    public void preProcess(final ArcticRunningTest test) {
        for (final ArcticPlayerPreProcessor preProcessor : preProcessors) {
            try {
                if (preProcessor.isRegisteredStatus(test.getStatus())) {
                    log.debug("Running pre-processor: {} for event {}", preProcessor.getName(), test.getStatus().getStatusCode());
                    final boolean isOk = preProcessor.preProcess(test);
                    if (!isOk) {
                        // Move to ABORTED immediately, so we don't execute other preprocessor unless they explicitly
                        // request the ABORTED status.
                        test.getStatus().passed(false);
                    }
                }
            } catch (final Exception e) {
                log.error("Unexpected error in pre-processing stage {} for test {}:{}", preProcessor.getName(),
                        test.getRecording().getTestName(), test.getRecording().getTestCase(), e);
                test.getStatus().error();
            }
        }

        // Attempt to move to RUNNING
        test.getStatus().passed(true);
    }

    public void reset() {
        preProcessors.forEach(ArcticPlayerPreProcessor::reset);
    }
}
