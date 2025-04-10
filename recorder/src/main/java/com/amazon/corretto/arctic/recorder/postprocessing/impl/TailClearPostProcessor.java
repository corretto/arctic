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
package com.amazon.corretto.arctic.recorder.postprocessing.impl;

import java.util.List;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.amazon.corretto.arctic.recorder.postprocessing.ArcticRecorderPostProcessor;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * The last relevant event that happens when recording a test is the user clicking, however, we keep recording until the
 * stop is done. This postprocessor cleans any event that happens after the last relevant event.
 */
@Slf4j
public final class TailClearPostProcessor implements ArcticRecorderPostProcessor {
    public static final String NAME = "tailCleaner";
    private static final int PRIORITY = 30;

    @Inject
    public TailClearPostProcessor() {
        log.debug("{} loaded", NAME);
    }

    @Override
    public boolean postProcess(final ArcticTest test) {
        final long timestamp = cleanMouseEvents(test);
        removeExtraEvents(test.getScreenChecks(), timestamp);
        removeExtraEvents(test.getEvents().getKeyboardEvents(), timestamp);
        test.setTestDuration(timestamp);
        return true;
    }

    private <T extends ArcticEvent> void removeExtraEvents(final List<T> eventList, final long timestamp) {
        T ev;
        while ((ev = eventList.get(eventList.size() - 1)) != null && ev.getTimestamp() > timestamp) {
            eventList.remove(eventList.size() - 1);
        }
    }

    private long cleanMouseEvents(final ArcticTest test) {
        final List<MouseEvent> mouseEventList = test.getEvents().getMouseEvents();
        MouseEvent ev;
        while ((ev = mouseEventList.get(mouseEventList.size() - 1)) != null &&
                ev.getSubType() == ArcticEvent.SubType.MOVE) {
            mouseEventList.remove(mouseEventList.size() - 1);
        }
        if (!mouseEventList.isEmpty()) {
            return mouseEventList.get(mouseEventList.size() - 1).getTimestamp();
        }
        return 0;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public int getPriority() {
        return PRIORITY;
    }
}
