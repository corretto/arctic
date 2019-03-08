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
package com.amazon.corretto.arctic.recorder.preprocessing.impl;

import javax.inject.Inject;

import com.amazon.corretto.arctic.common.backend.ArcticTestWindowFocusManager;
import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.gui.Point;
import com.amazon.corretto.arctic.recorder.identification.ArcticTestWindowOffsetCalculator;
import com.amazon.corretto.arctic.recorder.preprocessing.ArcticRecorderPreProcessor;

public final class FocusPreProcessor implements ArcticRecorderPreProcessor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FocusPreProcessor.class);

    public static final int PRIORITY = 20;
    public static final String NAME = "focus";

    private final ArcticTestWindowOffsetCalculator offsetCalculator;
    private final ArcticTestWindowFocusManager focusManager;

    @Inject
    public FocusPreProcessor(final ArcticTestWindowOffsetCalculator offsetCalculator,
            final ArcticTestWindowFocusManager focusManager) {
        this.offsetCalculator = offsetCalculator;
        this.focusManager = focusManager;
        log.debug("{} loaded", NAME);
    }

    @Override
    public boolean preProcess(final ArcticTest test) {
        final Point focusPoint = offsetCalculator.getOffset();
        test.setFocusPoint(focusPoint);
        focusManager.giveFocus(focusPoint);
        return true;
    }

    public int getPriority() {
        return PRIORITY;
    }
}
