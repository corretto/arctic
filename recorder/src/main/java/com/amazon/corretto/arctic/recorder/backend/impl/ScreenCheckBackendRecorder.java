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
package com.amazon.corretto.arctic.recorder.backend.impl;

import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;

import com.amazon.corretto.arctic.common.backend.ArcticScreenRecorder;
import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.recorder.backend.ArcticBackendRecorder;
import com.amazon.corretto.arctic.recorder.control.ArcticController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ScreenCheckBackendRecorder implements ArcticBackendRecorder {
    public static final String NAME = "sc";
    private final ArcticScreenRecorder screenRecorder;

    @Getter private List<ArcticEvent> recordingBuffer;

    @Inject
    public ScreenCheckBackendRecorder(final ArcticScreenRecorder screenRecorder) {
        log.debug("ScreenCheckRecorder loaded");
        this.screenRecorder = screenRecorder;
    }

    @Override
    public void acceptEvent(final ArcticController.Event event) {
        switch (event) {
            case START:
                recordingBuffer = new LinkedList<>();
                break;
            case SCREEN_CHECK:
                recordingBuffer.add(screenRecorder.capture());
                break;
            default:
                break;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
