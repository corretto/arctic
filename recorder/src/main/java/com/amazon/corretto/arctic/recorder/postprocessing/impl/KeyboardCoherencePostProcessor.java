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

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.model.event.KeyboardEvent;
import com.amazon.corretto.arctic.recorder.postprocessing.ArcticRecorderPostProcessor;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Performs a clean up of the keyboard events by ensuring no key is released without it being pressed before. It also
 * checks that no key is pressed when it is already pressed and that no key is left pressed at the end of the test.
 *
 * If keys are left pressed after doing the first run, a second run, in reverse order, is performed.
 */
@Slf4j
public final class KeyboardCoherencePostProcessor implements ArcticRecorderPostProcessor {
    public static final String NAME = "kbFix";
    private static final int PRIORITY = 30;

    @Inject
    public KeyboardCoherencePostProcessor() {
        log.debug("{} loaded", NAME);
    }

    @Override
    public boolean postProcess(final ArcticTest test) {
        final LinkedList<KeyboardEvent> events = new LinkedList<>(test.getEvents().getKeyboardEvents());
        boolean workPending = postProcess(events.iterator(),
                ArcticEvent.SubType.KEY_PRESSED, ArcticEvent.SubType.KEY_RELEASED);
        if (workPending) {
            workPending = postProcess(events.descendingIterator(),
                    ArcticEvent.SubType.KEY_RELEASED, ArcticEvent.SubType.KEY_PRESSED);

        }
        if (events.size() != test.getEvents().getKeyboardEvents().size()) {
            test.getEvents().setKeyboardEvents(events);
        }
        return !workPending;
    }

    public boolean postProcess(final Iterator<KeyboardEvent> iterator, final ArcticEvent.SubType open, final ArcticEvent.SubType close) {
        final Set<Integer> pressedKeys = new HashSet<>();
        while(iterator.hasNext()) {
            final KeyboardEvent ev = iterator.next();
            if (ev.getSubType().equals(open) && !pressedKeys.add(ev.getKeyCode())) {
                iterator.remove();
            }

            if (ev.getSubType().equals(close) && !pressedKeys.remove(ev.getKeyCode())) {
                iterator.remove();
            }
        }
        return !pressedKeys.isEmpty();
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
