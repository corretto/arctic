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
import javax.inject.Named;

import com.amazon.corretto.arctic.common.backend.ArcticScreenRecorder;
import com.amazon.corretto.arctic.common.gui.WorkbenchManager;
import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import com.amazon.corretto.arctic.recorder.inject.InjectionKeys;
import com.amazon.corretto.arctic.recorder.preprocessing.ArcticRecorderPreProcessor;
import com.amazon.corretto.arctic.api.exception.ArcticException;

public final class FirstScCheckPreProcessor implements ArcticRecorderPreProcessor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FirstScCheckPreProcessor.class);
    private static final int PRIORITY = 30;
    public static final String NAME = "firstSc";


    private final WorkbenchManager wbManager;
    private final ArcticScreenRecorder screenRecorder;
    private final int matchSize;
    private final int delay;

    @Inject
    public FirstScCheckPreProcessor(final WorkbenchManager wbManager,
            final ArcticScreenRecorder screenRecorder,
            final @Named(InjectionKeys.PRE_FIRST_SC_MATCH) int match_size,
            final @Named(InjectionKeys.PRE_FIRST_SC_DELAY) int delay) {
        this.wbManager = wbManager;

        this.screenRecorder = screenRecorder;
        this.matchSize = match_size;
        this.delay = delay;
        log.debug("{} loaded", NAME);
    }

    @Override
    public boolean preProcess(final ArcticTest test) {
        waitFor(delay);
        final ScreenArea wbArea = wbManager.getScreenArea();
        final int matchHeight = test.getFocusPoint().getY() - wbArea.getY() + matchSize;
        final ScreenArea sa = new ScreenArea(wbArea.getX(), wbArea.getY(), wbArea.getW(), matchHeight);
        final ScreenshotCheck sc = screenRecorder.capture(sa);
        test.setInitialSc(sc);
        sc.setTimestamp(0);
        return true;
    }

    private void waitFor(final long timeMs) {
        try {
            log.debug("Waiting for {}", timeMs);
            Thread.sleep(timeMs);
        } catch (final InterruptedException e) {
            log.error("Time controller has been interrupted");
            throw new ArcticException("TimeController has been interrupted", e);
        }
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }
}
