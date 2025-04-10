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

import java.awt.Toolkit;

import com.amazon.corretto.arctic.common.control.TestController;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.ArcticTestTimings;
import com.amazon.corretto.arctic.common.model.ArcticTestTruncations;
import com.amazon.corretto.arctic.common.model.gui.Point;
import com.amazon.corretto.arctic.recorder.inject.InjectionKeys;
import com.amazon.corretto.arctic.recorder.preprocessing.ArcticRecorderPreProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public final class InitialPreProcessor implements ArcticRecorderPreProcessor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InitialPreProcessor.class);

    public static final int PRIORITY = 10;
    public static final String NAME = "init";

    private final TestController testNameProvider;
    private final int recordingMode;
    private final int preferredPlayMode;
    private final ArcticTestTimings defaultTimings;
    private final ArcticTestTruncations defaultTruncations;
    private final String scope;
    private final Point screenResolution;


    @Inject
    public InitialPreProcessor(final TestController testNameProvider,
                               @Named(InjectionKeys.BACKEND_RECORDING_MODE) final int recordingMode,
                               @Named(InjectionKeys.PRE_INIT_PREFERRED_PLAY_MODE) final int preferredPlayMode,
                               final ArcticTestTimings defaultTimings, final ArcticTestTruncations defaultTruncations,
                               @Named(CommonInjectionKeys.REPOSITORY_SCOPE) final String scope) {
        this.testNameProvider = testNameProvider;
        this.recordingMode = recordingMode;
        this.preferredPlayMode = preferredPlayMode;
        this.defaultTimings = defaultTimings;
        this.defaultTruncations = defaultTruncations;
        this.scope = scope;
        this.screenResolution = new Point();
        this.screenResolution.setX(Toolkit.getDefaultToolkit().getScreenSize().width);
        this.screenResolution.setY(Toolkit.getDefaultToolkit().getScreenSize().height);
        log.debug("{} loaded", NAME);
    }
    @Override
    public boolean preProcess(final ArcticTest test) {
        test.setTestName(testNameProvider.getCurrentTestClass());
        test.setTestCase(testNameProvider.getCurrentTestCase());
        test.setRecordingMode(recordingMode);
        test.setPreferredPlayMode(preferredPlayMode);
        test.setTimings(defaultTimings);
        test.setTruncations(defaultTruncations);
        test.setScreenRes(screenResolution);
        test.setTestRecordTime(System.nanoTime());
        test.setScope(scope);
        return true;
    }

    public int getPriority() {
        return PRIORITY;
    }
}
