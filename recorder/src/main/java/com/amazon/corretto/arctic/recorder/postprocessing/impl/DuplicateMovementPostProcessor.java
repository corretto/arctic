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

import java.util.ListIterator;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.amazon.corretto.arctic.recorder.postprocessing.ArcticRecorderPostProcessor;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * The backend my end up reporting duplicated events, specially for mouse movement related ones. This PostProcessor
 * scans the recording and removes duplicated events
 */
@Slf4j
public final class DuplicateMovementPostProcessor implements ArcticRecorderPostProcessor {
    public static final String NAME = "dupRemoval";
    public static final int PRIORITY = 20;

    @Inject
    public DuplicateMovementPostProcessor() {
        log.debug("{} loaded", NAME);
    }

    private static final int DUPLICATE_EVENTS_MASK = ArcticEvent.SubType.MOVE.getValue()
            | ArcticEvent.SubType.DRAG.getValue();

    @Override
    public boolean postProcess(final ArcticTest test) {
        final ListIterator<MouseEvent> iter = test.getEvents().getMouseEvents().listIterator();
        int lastX = -1;
        int lastY = -1;
        while(iter.hasNext()) {
            final MouseEvent ev = iter.next();
            if (ev.getSubType().inMask(DUPLICATE_EVENTS_MASK)
                    && ev.getX() == lastX && ev.getY() == lastY) {
                iter.remove();
            } else {
                lastX = ev.getX();
                lastY = ev.getY();
            }
        }
        return true;
    }

    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
