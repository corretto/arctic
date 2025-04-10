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

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.repository.TestSaveRepository;
import com.amazon.corretto.arctic.recorder.inject.InjectionKeys;
import com.amazon.corretto.arctic.recorder.postprocessing.ArcticRecorderPostProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TestSavePostProcessor implements ArcticRecorderPostProcessor {
    public static final String NAME = "save";
    private static final int PRIORITY = 60;

    private final boolean useCompression;
    private final String eventsFileName;
    private final TestSaveRepository repository;

    @Inject
    public TestSavePostProcessor(@Named(InjectionKeys.POST_SAVE_TEST_ZIP) final boolean useCompression,
                                 @Named(InjectionKeys.POST_SAVE_EVENTS_FILENAME) final String eventsFileName,
                                 final TestSaveRepository repository) {
        this.useCompression = useCompression;
        this.eventsFileName = eventsFileName + (useCompression ? ".zip" : "");
        this.repository = repository;
        log.debug("{} loaded", NAME);

    }

    @Override
    public boolean postProcess(final ArcticTest test) {
        log.debug("Saving test {}:{} with {}:{}:{} events", test.getTestName(), test.getTestCase(),
                test.getScreenChecks().size(), test.getEvents().getMouseEvents().size(),
                test.getEvents().getKeyboardEvents().size());
        test.setTestDuration(System.nanoTime() - test.getTestRecordTime());
        test.setZip(useCompression);
        test.setEventsFile(eventsFileName);
        return repository.saveTestCase(test);
    }

    @Override
    public String getName() {
        return NAME;
    }

    public int getPriority() {
        return PRIORITY;
    }
}
