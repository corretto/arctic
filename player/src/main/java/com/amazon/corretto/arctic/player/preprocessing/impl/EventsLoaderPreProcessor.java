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

import com.amazon.corretto.arctic.common.repository.TestLoadRepository;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.preprocessing.ArcticPlayerPreProcessor;
import com.amazon.corretto.arctic.shared.exception.ArcticException;

/**
 * Most of the information of the test, like the actual events recording, is only loaded on demand, as it is
 * persisted in a different file. This will happen once the player has identified the current test case and it is
 * ready to start posting events.
 */
public final class EventsLoaderPreProcessor implements ArcticPlayerPreProcessor {
    public static final String NAME = "eventsLoader";
    private static final int PRIORITY = 60;

    private final TestLoadRepository testLoadRepository;

    @Inject
    public EventsLoaderPreProcessor(final TestLoadRepository testLoadRepository) {
        this.testLoadRepository = testLoadRepository;
    }

    @Override
    public boolean preProcess(final ArcticRunningTest test) {
        try {
            testLoadRepository.loadTestEvents(test.getRecording());
            return true;
        } catch (final ArcticException e) {
            return false;
        }
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
