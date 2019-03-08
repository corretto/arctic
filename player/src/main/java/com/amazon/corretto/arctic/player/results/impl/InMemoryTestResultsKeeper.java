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
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.common.model.ArcticResultTuple;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.session.ArcticSessionKeeper;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.results.ArcticTestResultsKeeper;

/**
 * Implementation for an ArcticTestResultsKeeper backed by memory.
 */
public final class InMemoryTestResultsKeeper implements ArcticTestResultsKeeper {
    private final Map<TestId, ArcticResultTuple<TestId, TestStatusCode>> results = new HashMap<>();

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean hasData() {
        return !results.isEmpty();
    }

    @Override
    public Collection<ArcticResultTuple<TestId, TestStatusCode>> getResults() {
        return results.values().stream()
                .sorted(Comparator.comparing(ArcticResultTuple::getLastUpdated))
                .collect(Collectors.toList());
    }

    @Override
    public ArcticResultTuple<TestId, TestStatusCode> getResult(final TestId testId) {
        return results.getOrDefault(testId, null);
    }

    @Override
    public void clear(final TestId testId) {
        results.remove(testId);
    }

    @Override
    public void addValue(final TestId testId, final TestStatusCode value) {
        results.computeIfAbsent(testId, ArcticResultTuple::new).setValue(value);
    }

    @Override
    public void updateValue(final TestId testId, final Consumer<TestStatusCode> value) {
        if (results.containsKey(testId)) {
            results.get(testId).updateValue(value);
        }
    }

    @Override
    public ArcticSessionKeeper.SessionObject getSession() {
        return new SessionObject(results);
    }

    @Override
    public <S extends ArcticSessionKeeper.SessionObject> void restoreSession(final S sessionObject) {
        if (sessionObject instanceof SessionObject) {
            results.clear();
            results.putAll(((SessionObject) sessionObject).results);
        }
    }

    @Override
    public Class<? extends ArcticSessionKeeper.SessionObject> getSessionObjectClass() {
        return SessionObject.class;
    }

    @Override
    public void clear(final String testName) {
        results.entrySet().removeIf(it -> it.getKey().getTestClass().equals(testName));
    }

    @Override
    public void clear() {
        results.clear();
    }

    /**
     * SessionObject for the InMemoryTestResultsKeeper. It persists a list of TestStatusCode. By persisting this
     * information, we can retrieve the results of a previously run session.
     */
    public static final class SessionObject implements ArcticSessionKeeper.SessionObject {
        private Map<TestId, ArcticResultTuple<TestId, TestStatusCode>> results;

        /**
         * Creates a new SessionObject that will be used to persist information regarding test results.
         * @param results List of results that need to be persisted
         */
        SessionObject(final Map<TestId, ArcticResultTuple<TestId, TestStatusCode>> results) {
            this.results = results;
        }

        /**
         * Empty constructor to use during deserialization.
         */
        public SessionObject() {

        }

        /**
         * Returns the list of test results that were persisted in this SessionObject.
         * @return List of test results that were persisted
         */
        public Map<TestId, ArcticResultTuple<TestId, TestStatusCode>> getResults() {
            return results;
        }
    }
}
