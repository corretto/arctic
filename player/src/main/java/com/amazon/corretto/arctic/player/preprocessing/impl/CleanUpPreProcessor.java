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

package com.amazon.corretto.arctic.player.preprocessing.impl;

import java.util.Set;
import javax.inject.Inject;

import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.preprocessing.ArcticPlayerPreProcessor;
import com.amazon.corretto.arctic.common.session.ArcticSessionKeeper;
import com.amazon.corretto.arctic.player.results.ArcticTestResultsKeeper;

/**
 * Pre-processor that clears the results of the test we are about to run. This guarantees there are no duplicate sc
 * failures. This is also needed to guarantee the proper value is stored after the test is executed.
 */
public final class CleanUpPreProcessor implements ArcticPlayerPreProcessor {
    public static final String NAME = "cleanUp";
    private static final int PRIORITY = 10;
    private final Set<ArcticSessionKeeper<?, ?>> keepers;
    private final ArcticTestResultsKeeper testKeeper;

    /**
     * Creates a new instance for this preprocessor.
     * @param keepers All the {@link ArcticSessionKeeper} that we need to clean up.
     * @param testKeeper Keeper of the result, will be updated for the current test
     */
    @Inject
    public CleanUpPreProcessor(final Set<ArcticSessionKeeper<?, ?>> keepers, final ArcticTestResultsKeeper testKeeper) {
        this.keepers = keepers;
        this.testKeeper = testKeeper;
    }

    @Override
    public boolean preProcess(final ArcticRunningTest test) {
        keepers.forEach(it -> it.clear(test.getTestId()));
        testKeeper.addValue(test.getTestId(), test.getStatus().getStatusCode());
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
        return Set.of(TestStatusCode.STARTING);
    }
}
