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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 * Converts ArcticResults into a junit xml file. Whether a test is considered ok or not ok depends on the value of
 * {@link InjectionKeys#CONFIRMATION_MODE}. If disabled, {@link TestStatusCode#UNCONFIRMED} are considered ok, but if
 * enabled, only {@link TestStatusCode#CONFIRMED} tests are considered ok.
 * @see <a href='https://llg.cubic.org/docs/junit/'>Junit format and XSD</a>
 */
public final class XmlResultsConverter implements ArcticResultsConverter<String> {
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
    public XmlResultsConverter(final ArcticTestResultsKeeper resultsKeeper,
                                 @Named(InjectionKeys.CONFIRMATION_MODE) final boolean confirmationMode) {
        this.resultsKeeper = resultsKeeper;
        this.okCodes = confirmationMode ? CONFIRMED_OK_CODES : UNCONFIRMED_OK_CODES;

    }

    @Override
    public String getResults() throws ArcticNoResultsException {
        if (!resultsKeeper.hasData()) {
            throw new ArcticNoResultsException();
        }
        Collection<ArcticResultTuple<TestId, TestStatusCode>> results = resultsKeeper.getResults();
        // If all tests have group information, we can use that for the report
        boolean useGroups = results.stream()
                .map(ArcticResultTuple::getId)
                .map(TestId::getTestGroup)
                .allMatch(Objects::nonNull);
        return generateTestSuites(resultsKeeper.getResults(), useGroups);
    }

    private String generateTestSuites(final Collection<ArcticResultTuple<TestId, TestStatusCode>> results,
                                      final boolean useGroup) {
        int tests = results.size();
        long testPass = results.stream().map(ArcticResultTuple::getValue).filter(okCodes::contains).count();
        long testFail = results.stream().map(ArcticResultTuple::getValue).filter(TestStatusCode.FAILED::equals).count();
        long testError = tests - (testPass + testFail);
        Map<String, Map<TestId, TestStatusCode>> groupedResults = new LinkedHashMap<>();
        if (useGroup) {
            results.forEach(it -> groupedResults.computeIfAbsent(it.getId().getTestGroup(), key -> new HashMap<>())
                    .put(it.getId(), it.getValue()));
        } else {
            results.forEach(it -> groupedResults.computeIfAbsent(it.getId().getTestClass(), key -> new HashMap<>())
                    .put(it.getId(), it.getValue()));
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<testsuites tests=\"%s\" errors=\"%s\" failures=\"%s\">\n", tests, testError,
                testFail));
        for (String testClass : groupedResults.keySet()) {
            sb.append(generateTestSuite(testClass, groupedResults.get(testClass))).append("\n");
        }
        sb.append("</testsuites>");
        return sb.toString();
    }

    private String generateTestSuite(final String suite, final Map<TestId, TestStatusCode> testCases) {
        StringBuilder sb = new StringBuilder();
        int tests = testCases.size();
        long testPass = testCases.values().stream().filter(okCodes::contains).count();
        long testFail = testCases.values().stream().filter(TestStatusCode.FAILED::equals).count();
        long testError = tests - (testPass + testFail);
        sb.append(String.format("  <testsuite name=\"%s\" tests=\"%s\" errors=\"%s\" failures=\"%s\">\n", suite,
                tests, testError, testFail));
        testCases.entrySet().stream().sorted(Comparator.comparing(it -> it.getKey().getTestCase()))
                .forEach(it -> sb.append(generateTestCase(it.getKey(), it.getValue())).append("\n"));
        sb.append("  </testsuite>");
        return sb.toString();
    }

    private String generateTestCase(final TestId id, final TestStatusCode result) {
        if (okCodes.contains(result)) {
            return String.format("    <testcase classname=\"%s\" name=\"%s\"/>", id.getTestClass(), id.getTestCase());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("    <testcase classname=\"%s\" name=\"%s\">\n", id.getTestClass(),
                    id.getTestCase()));
            if (TestStatusCode.FAILED.equals(result)) {
                sb.append(String.format("      <failure type=\"%s\"/>", result));
            } else {
                sb.append(String.format("      <error type=\"%s\"/>", result));
            }
            sb.append("\n    </testcase>");
            return sb.toString();
        }
    }
}
