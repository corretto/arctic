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

import javax.inject.Inject;

import com.amazon.corretto.arctic.common.gui.WorkbenchManager;
import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.recorder.postprocessing.ArcticRecorderPostProcessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class WorkbenchToBackPostProcessor implements ArcticRecorderPostProcessor {
    public static final String NAME = "wbFix";
    private static final int PRIORITY = 70;

    private final WorkbenchManager wbManager;

    @Inject
    public WorkbenchToBackPostProcessor(final WorkbenchManager wbManager) {
        this.wbManager = wbManager;
        log.debug("{} loaded", NAME);
    }

    @Override
    public boolean postProcess(final ArcticTest test) {
        wbManager.toBack();
        return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public int getPriority() {
        return PRIORITY;
    }
}
