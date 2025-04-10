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

import java.util.Collection;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.recorder.postprocessing.ArcticRecorderPostProcessor;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TimestampFixPostProcessor implements ArcticRecorderPostProcessor {
    public static final String NAME = "tsFix";
    private static final int PRIORITY = 10;

    @Inject
    public TimestampFixPostProcessor() {
        log.debug("{} loaded", NAME);
    }

    @Override
    public boolean postProcess(final ArcticTest test) {
        final long recordDate = test.getTestRecordTime();
        fixDates(test.getScreenChecks(), recordDate);
        fixDates(test.getEvents().getMouseEvents(), recordDate);
        fixDates(test.getEvents().getKeyboardEvents(), recordDate);

        return true;
    }

    private void fixDates(final Collection<? extends ArcticEvent> events, final long recordDate) {
        events.forEach(it -> it.setTimestamp(it.getTimestamp() - recordDate));
    }

    @Override
    public String getName() {
        return NAME;
    }

    public int getPriority() {
        return PRIORITY;
    }
}
