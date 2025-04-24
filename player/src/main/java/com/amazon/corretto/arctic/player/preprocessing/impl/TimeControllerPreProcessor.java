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

import com.amazon.corretto.arctic.player.control.TimeController;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import com.amazon.corretto.arctic.player.preprocessing.ArcticPlayerPreProcessor;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TimeControllerPreProcessor implements ArcticPlayerPreProcessor {
    private static final Logger log = LoggerFactory.getLogger(TimeControllerPreProcessor.class);

    public static final String NAME = "timeControl";
    private static final int PRIORITY = 80;
    private final TimeController timeController;

    @Inject
    public TimeControllerPreProcessor(final TimeController timeController) {
        this.timeController = timeController;
    }

    @Override
    public boolean preProcess(final ArcticRunningTest test) {
        timeController.startTestCase(test.getRecording());
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

    @Override
    public Set<TestStatusCode> getRegisteredStatuses() {
        return Set.of(TestStatusCode.STARTING);
    }
}
