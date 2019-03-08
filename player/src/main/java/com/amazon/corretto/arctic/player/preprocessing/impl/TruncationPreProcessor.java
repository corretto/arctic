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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.event.KeyboardEvent;
import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.preprocessing.ArcticPlayerPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TruncationPreProcessor implements ArcticPlayerPreProcessor {
    private static final Logger log = LoggerFactory.getLogger(EventsLoaderPreProcessor.class);

    public static final String NAME = "truncations";
    private static final int PRIORITY = 70;

    @Override
    public boolean preProcess(final ArcticRunningTest test) {
        test.getRecording().getEvents().setKeyboardEvents(getTruncatedKbEvents(test.getRecording()));
        test.getRecording().getEvents().setMouseEvents(getTruncatedMouseEvents(test.getRecording()));

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

    private List<MouseEvent> getTruncatedMouseEvents(final ArcticTest test) {
        final List<MouseEvent> list = test.getEvents().getMouseEvents();
        final int startTruncation = test.getTruncations().getMouseStart();
        final int endTruncation = test.getTruncations().getMouseEnd();
        if (startTruncation + endTruncation >= list.size()) {
            return Collections.emptyList();
        }
        return list.subList(startTruncation, list.size() - endTruncation);
    }

    private List<KeyboardEvent> getTruncatedKbEvents(final ArcticTest test) {
        final List<KeyboardEvent> list = test.getEvents().getKeyboardEvents();
        final int startTruncation = test.getTruncations().getKbStart();
        final int endTruncation = test.getTruncations().getKbEnd();
        if (startTruncation + endTruncation >= list.size()) {
            return Collections.emptyList();
        }
        return list.subList(startTruncation, list.size() - endTruncation);
    }

    @Override
    public Set<TestStatusCode> getRegisteredStatuses() {
        return Set.of(TestStatusCode.STARTING);
    }
}
