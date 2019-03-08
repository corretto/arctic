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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import javax.inject.Inject;

import com.amazon.corretto.arctic.common.model.ArcticResultTuple;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.repository.TestRepository;
import com.amazon.corretto.arctic.common.session.ArcticSessionKeeper;
import com.amazon.corretto.arctic.player.model.FailureId;
import com.amazon.corretto.arctic.player.model.PixelCheckFailure;
import com.amazon.corretto.arctic.player.results.ArcticScFailureKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation for an ArcticScFailureKeeper backed by memory.
 */
public final class InMemoryScFailureKeeper implements ArcticScFailureKeeper {
    private static final Logger log = LoggerFactory.getLogger(InMemoryScFailureKeeper.class);
    private final TestRepository repository;
    private final PriorityQueue<ArcticResultTuple<FailureId, PixelCheckFailure>> queue =
            new PriorityQueue<>(Comparator.comparing(ArcticResultTuple::getLastUpdated));
    private final Map<FailureId, ArcticResultTuple<FailureId, PixelCheckFailure>> failures = new LinkedHashMap<>();

    /**
     * Creates a new instance for an arcticScFailureKeeper.
     * @param repository Test repository used to update the tests with new alternative images whenever they are approved
     *                   during failures review.
     */
    @Inject
    public InMemoryScFailureKeeper(final TestRepository repository) {
        this.repository = repository;
    }


    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean hasData() {
        return !failures.isEmpty();
    }

    @Override
    public Collection<ArcticResultTuple<FailureId, PixelCheckFailure>> getResults() {
        return failures.values();
    }

    @Override
    public ArcticResultTuple<FailureId, PixelCheckFailure> getResult(final FailureId failureId) {
        return failures.getOrDefault(failureId, null);
    }


    @Override
    public PixelCheckFailure peek() {
        ArcticResultTuple<FailureId, PixelCheckFailure> tuple = queue.peek();
        if (tuple != null) {
            return tuple.getValue();
        }
        return null;
    }

    @Override
    public PixelCheckFailure poll() {
        ArcticResultTuple<FailureId, PixelCheckFailure> tuple = queue.poll();
        if (tuple != null) {
            return tuple.getValue();
        }
        return null;
    }

    @Override
    public boolean acceptResult(final Result result, final FailureId failureId) {
        ArcticResultTuple<FailureId, PixelCheckFailure> tuple = failures.get(failureId);
        if (tuple == null) {
            log.warn("Attempted to process {} that is no longer present", failureId);
            return false;
        }
        switch (result) {
            case ACCEPT:
                failures.remove(failureId);
                return repository.addAlternative(tuple.getId().getTestId(), tuple.getId().getScope(),
                        tuple.getValue().getMainSavedImagePath(), tuple.getValue().getMainImageHash(),
                        tuple.getValue().getCurrentImageFullPath(), tuple.getValue().getCurrentImageHash());
            case REJECT:
                // Remove completely
                failures.remove(failureId);
                break;
            case IGNORE:
            default:
                // A dummy update will cause the time in the tuple to be changed, pushing it to the end of the queue
                tuple.updateValue(it -> { });
                queue.add(tuple);
                // Add back to the list for future review
        }
        return false;
    }

    @Override
    public void clear() {
        failures.clear();
        queue.clear();
    }

    @Override
    public void clear(final String testName) {
        failures.keySet().stream().filter(it -> it.getTestId().getTestClass().equals(testName)).forEach(this::clear);
    }

    @Override
    public void clear(final TestId testId) {
        failures.keySet().stream().filter(it -> it.getTestId().equals(testId)).forEach(this::clear);
    }

    @Override
    public void clear(final FailureId failureId) {
        if (failures.containsKey(failureId)) {
            ArcticResultTuple<FailureId, PixelCheckFailure> tuple = failures.get(failureId);
            failures.remove(tuple.getId());
            queue.remove(tuple);
        }
    }

    @Override
    public void addValue(final FailureId failureId, final PixelCheckFailure value) {
        if (failures.containsKey(failureId)) {
            failures.get(failureId).setValue(value);
        } else {
            ArcticResultTuple<FailureId, PixelCheckFailure> tuple = new ArcticResultTuple<>(failureId, value);
            failures.put(tuple.getId(), tuple);
            queue.add(tuple);
        }
    }

    @Override
    public void updateValue(final FailureId failureId, final Consumer<PixelCheckFailure> valueUpdater) {
        if (failures.containsKey(failureId)) {
            failures.get(failureId).updateValue(valueUpdater);
        }
    }

    @Override
    public ArcticSessionKeeper.SessionObject getSession() {
        return new SessionObject(failures);
    }

    @Override
    public <S extends ArcticSessionKeeper.SessionObject> void restoreSession(final S sessionObject) {
        if (sessionObject instanceof SessionObject) {
            failures.clear();
            queue.clear();
            failures.putAll(((SessionObject) sessionObject).getFailures());
            queue.addAll(failures.values());
        }
    }

    @Override
    public Class<? extends ArcticSessionKeeper.SessionObject> getSessionObjectClass() {
        return SessionObject.class;
    }

    /**
     * SessionObject for the InMemoryScFailureKeeper. It persists a list of PixelCheckFailures. By persisting this
     * information, we are able to do a review of failures that happened during a previous session.
     */
    public static final class SessionObject implements ArcticSessionKeeper.SessionObject {
        // No longer used, but keep for backwards compatibility with 0.4.11 or older
        private Map<FailureId, ArcticResultTuple<FailureId, PixelCheckFailure>> failures;

        /**
         * Creates a new SessionObject that will be used to persist information regarding ScreenCheck failures.
         * @param failures List of failures that need to be persisted.
         */
        public SessionObject(final Map<FailureId, ArcticResultTuple<FailureId, PixelCheckFailure>> failures) {
            this.failures = failures;
        }

        /**
         * Empty constructor to use during deserialization.
         */
        public SessionObject() {

        }

        /**
         * Returns the map of failures that were persisted in this SessionObject.
         * @return Map of failures that were persisted
         */
        public Map<FailureId, ArcticResultTuple<FailureId, PixelCheckFailure>> getFailures() {
            return failures;
        }
    }
}
