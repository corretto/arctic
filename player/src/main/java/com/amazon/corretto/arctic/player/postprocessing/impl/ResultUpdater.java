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

import java.util.EnumSet;
import java.util.Set;

import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.postprocessing.ArcticPlayerPostProcessor;
import com.amazon.corretto.arctic.player.results.ArcticTestResultsKeeper;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates the results of the test execution. This is done for all {@link TestStatusCode} at the end of the
 * postProcessing pipeline
 */
public final class ResultUpdater implements ArcticPlayerPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(ResultUpdater.class);
    private static final int PRIORITY = 90;

    public static final String NAME = "resultsUpdater";
    private final ArcticTestResultsKeeper resultsKeeper;

    /**
     * Creates a new instance of the postProcessor.
     * @param resultsKeeper Object that will store the different test results.
     */
    @Inject
    public ResultUpdater(final ArcticTestResultsKeeper resultsKeeper) {
        this.resultsKeeper = resultsKeeper;
    }

    @Override
    public boolean postProcess(final ArcticRunningTest test) {
        log.debug("Updating result for {}:{} as {}", test.getTestId().getTestClass(), test.getTestId().getTestCase(),
                test.getStatus().getStatusCode());
        resultsKeeper.addValue(test.getTestId(), test.getStatus().getStatusCode());
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
        return EnumSet.of(TestStatusCode.UNCONFIRMED,
                TestStatusCode.CONFIRMED,
                TestStatusCode.FAILED,
                TestStatusCode.STOPPED,
                TestStatusCode.ABORTED,
                TestStatusCode.ERROR,
                TestStatusCode.ERROR_CONFIRMED,
                TestStatusCode.NO_RECORDING,
                TestStatusCode.NO_RECORDING_OK,
                TestStatusCode.NO_RECORDING_FAIL);
    }
}
