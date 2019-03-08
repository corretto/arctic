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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import javax.inject.Inject;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.amazon.corretto.arctic.recorder.postprocessing.ArcticRecorderPostProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * Performs a clean up of the mouse events by ensuring no button is released without it being pressed before. It also
 * checks that no button is pressed when it is already pressed and that no button is left pressed at the end of the test.
 *
 * If buttons are left pressed after doing the first run, a second run, in reverse order, is performed.
 */
@Slf4j
public final class MouseCoherencePostProcessor implements ArcticRecorderPostProcessor {
    public static final String NAME = "mouseFix";
    private static final int PRIORITY = 30;

    @Inject
    public MouseCoherencePostProcessor() {
        log.debug("{} loaded", NAME);
    }

    @Override
    public boolean postProcess(final ArcticTest test) {
        final LinkedList<MouseEvent> events = new LinkedList<>(test.getEvents().getMouseEvents());
        boolean workPending = postProcess(events.iterator(),
                ArcticEvent.SubType.PRESS, ArcticEvent.SubType.RELEASE);
        if (workPending) {
            workPending = postProcess(events.descendingIterator(),
                    ArcticEvent.SubType.RELEASE, ArcticEvent.SubType.PRESS);
        }
        if (events.size() != test.getEvents().getMouseEvents().size()) {
            test.getEvents().setMouseEvents(events);
        }
        return !workPending;
    }

    public boolean postProcess(final Iterator<MouseEvent> iterator, final ArcticEvent.SubType open, final ArcticEvent.SubType close) {
        final Set<Integer> pressedButtons = new HashSet<>();
        while(iterator.hasNext()) {
            final MouseEvent ev = iterator.next();
            if (ev.getSubType().equals(open) && !pressedButtons.add(ev.getButton())) {
                iterator.remove();
            }

            if (ev.getSubType().equals(close) && !pressedButtons.remove(ev.getButton())) {
                iterator.remove();
            }
        }
        return !pressedButtons.isEmpty();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
