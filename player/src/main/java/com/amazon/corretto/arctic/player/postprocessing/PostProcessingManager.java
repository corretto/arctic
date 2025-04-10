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

package com.amazon.corretto.arctic.player.postprocessing;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.preprocessing.PreProcessingManager;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to control the execution of different player post processors.
 */
public final class PostProcessingManager {
    private static final Logger log = LoggerFactory.getLogger(PreProcessingManager.class);

    private final List<ArcticPlayerPostProcessor> postProcessors;

    @Inject
    public PostProcessingManager(final Set<ArcticPlayerPostProcessor> postProcessors) {
        this.postProcessors = postProcessors.stream()
                .sorted(Comparator.comparing(ArcticPlayerPostProcessor::getPriority))
                .collect(Collectors.toList());
    }

    public void postProcess(final ArcticRunningTest test) {
            for (final ArcticPlayerPostProcessor postProcessor : postProcessors) {
                try {
                    if (postProcessor.isRegisteredStatus(test.getStatus())) {
                        final boolean isOk = postProcessor.postProcess(test);
                        if (!isOk) {
                            test.getStatus().passed(false);
                        }
                    }
                } catch (final Exception e) {
                    log.error("Unexpected error in post-processing stage {} for test {}:{}", postProcessor.getName(),
                            test.getRecording().getTestName(), test.getRecording().getTestCase(), e);
                    test.getStatus().error();
                }
            }
    }

    public void reset() {
        postProcessors.forEach(ArcticPlayerPostProcessor::reset);
    }
}
