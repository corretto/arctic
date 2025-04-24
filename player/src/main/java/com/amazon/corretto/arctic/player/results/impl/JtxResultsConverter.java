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

package com.amazon.corretto.arctic.player.results.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.common.model.ArcticResultTuple;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.player.exception.ArcticNoResultsException;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.results.ArcticResultsConverter;
import com.amazon.corretto.arctic.player.results.ArcticTestResultsKeeper;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Converts ArcticResults into a tap file. Whether a test is considered ok or not ok depends on the value of
 * {@link InjectionKeys#CONFIRMATION_MODE}. If disabled, {@link TestStatusCode#UNCONFIRMED} are considered ok, but if
 * enabled, only {@link TestStatusCode#CONFIRMED} tests are considered ok.
 * @see <a href='https://testanything.org/tap-version-13-specification.html'>Tap v13 specification</a>
 */
public final class JtxResultsConverter implements ArcticResultsConverter<String> {
    private final ArcticTestResultsKeeper resultsKeeper;
    private final Set<TestStatusCode> okCodes;

    private static final Set<TestStatusCode> UNCONFIRMED_OK_CODES = Set.of(
            TestStatusCode.UNCONFIRMED
    );

    private static final Set<TestStatusCode> CONFIRMED_OK_CODES = Set.of(
            TestStatusCode.CONFIRMED,
            TestStatusCode.NO_RECORDING_OK
    );

    /**
     * Creates a new instance of the converter.
     * @param resultsKeeper To fetch the results from
     * @param confirmationMode Whether we are executing in confirmation mode
     */
    @Inject
    public JtxResultsConverter(final ArcticTestResultsKeeper resultsKeeper,
                               @Named(InjectionKeys.CONFIRMATION_MODE) final boolean confirmationMode) {
        this.resultsKeeper = resultsKeeper;
        this.okCodes = confirmationMode ? CONFIRMED_OK_CODES : UNCONFIRMED_OK_CODES;

    }

    @Override
    public String getResults() throws ArcticNoResultsException {
        if (!resultsKeeper.hasData()) {
            throw new ArcticNoResultsException();
        }
        final List<ArcticResultTuple<TestId, TestStatusCode>> results = resultsKeeper.getResults().stream()
                .sorted(Comparator.comparing(ArcticResultTuple::getLastUpdated))
                .collect(Collectors.toList());
        final StringBuilder out = new StringBuilder();
        for (final ArcticResultTuple<TestId, TestStatusCode> result : results) {
            if (okCodes.contains(result.getValue())) {
                out.append(String.format("%s[%s]", result.getId().getTestGroup(), result.getId().getTestCase()));
                out.append(System.lineSeparator());
            }
        }

        return out.toString();
    }
}
