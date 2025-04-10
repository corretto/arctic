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

package com.amazon.corretto.arctic.recorder;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.common.control.TestController;
import com.amazon.corretto.arctic.common.gui.ShadeManager;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.model.event.KeyboardEvent;
import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.recorder.backend.ArcticBackendRecorder;
import com.amazon.corretto.arctic.recorder.control.ArcticController;
import com.amazon.corretto.arctic.recorder.inject.InjectionKeys;
import com.amazon.corretto.arctic.recorder.postprocessing.ArcticRecorderPostProcessor;
import com.amazon.corretto.arctic.recorder.preprocessing.ArcticRecorderPreProcessor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public final class ArcticRecorder implements ArcticController.Listener, TestController.Listener {
    private final ArcticBackendRecorder backendRecorder;
    private final ShadeManager shadeManager;
    private final List<ArcticRecorderPostProcessor> postProcessors;
    private final List<ArcticRecorderPreProcessor> preProcessors;
    private final String runningScope;
    private final boolean autoStopRecording;
    private ArcticTest currentTest;
    private boolean recording = false;

    @Inject
    public ArcticRecorder(final ArcticBackendRecorder backendRecorder, final ShadeManager shadeManager,
                          final Set<ArcticRecorderPostProcessor> postProcessors,
                          final Set<ArcticRecorderPreProcessor> preProcessors,
                          @Named(CommonInjectionKeys.REPOSITORY_SCOPE) final String runningScope,
                          @Named(InjectionKeys.CONTROL_AUTO_STOP) final boolean autoStopRecording) {
        this.backendRecorder = backendRecorder;
        this.shadeManager = shadeManager;
        this.postProcessors = postProcessors.stream()
                .sorted(Comparator.comparing(ArcticRecorderPostProcessor::getPriority))
                .collect(Collectors.toList());
        this.preProcessors = preProcessors.stream()
                .sorted(Comparator.comparing(ArcticRecorderPreProcessor::getPriority))
                .collect(Collectors.toList());
        this.runningScope = runningScope;
        this.autoStopRecording = autoStopRecording;
    }

    void startRecording() {
        recording = true;
        currentTest = new ArcticTest();
        currentTest.setScope(runningScope);
        preProcessors.forEach(it -> it.preProcess(currentTest));
        log.info("Start recording of test: {}:{}", currentTest.getTestName(), currentTest.getTestCase());
    }

    void stopRecording() {
        recording = false;
        log.info("Stop recording of test: {}:{}", currentTest.getTestName(), currentTest.getTestCase());
        addEvents(backendRecorder.getRecordingBuffer());
        for (final ArcticRecorderPostProcessor postProcessor : postProcessors) {
            log.debug("Running post processor {}", postProcessor.getName());
            if (!postProcessor.postProcess(currentTest)) {
                log.warn("Aborting test as requested by {}", postProcessor.getName());
                discard();
                break;
            }
        }
    }

    void discard() {
        preProcessors.forEach(ArcticRecorderPreProcessor::discard);
    }

    void spawnShade() {
        shadeManager.spawnShade();
    }

    private void addEvents(final List<ArcticEvent> events) {
        log.debug("Recorded {} events", events.size());
        currentTest.getEvents().setMouseEvents(events.stream()
                .filter(it -> ArcticEvent.Type.MOUSE_EVENT.equals(it.getType()))
                .map(it -> (MouseEvent) it)
                .collect(Collectors.toList()));
        currentTest.getEvents().setKeyboardEvents(events.stream()
                .filter(it -> ArcticEvent.Type.KEYBOARD_EVENT.equals(it.getType()))
                .map(it -> (KeyboardEvent) it)
                .collect(Collectors.toList()));
        currentTest.setScreenChecks(events.stream()
                .filter(it -> ArcticEvent.Type.SCREENSHOT_CHECK.equals(it.getType()))
                .map(it -> (ScreenshotCheck) it)
                .collect(Collectors.toList()));
    }

    synchronized void startStop() {
        if (!recording) {
            start();
        } else {
            stop();
        }
    }

    synchronized void start() {
        if (!recording) {
            recording = true;
            startRecording();
            backendRecorder.acceptEvent(ArcticController.Event.START);
        }
    }

    synchronized void stop() {
        if (recording) {
            recording = false;
            backendRecorder.acceptEvent(ArcticController.Event.STOP);
            stopRecording();
        }
    }

    @Override
    public void acceptEvent(final ArcticController.Event event) {
        log.debug("Event: {}", event);
        switch (event) {
            case START_STOP:
                startStop();
                break;
            case START:
                start();
                backendRecorder.acceptEvent(event);
                break;
            case STOP:
                backendRecorder.acceptEvent(event);
                stop();
                break;
            case DISCARD:
                discard();
                backendRecorder.acceptEvent(event);
                break;
            case SPAWN_SHADE:
                spawnShade();
                break;
            default:
                backendRecorder.acceptEvent(event);
        }
    }

    @Override
    public void finishTestCase(String testGroup, String testClass, String testCase, boolean result) {
        if (autoStopRecording) {
            if (result) {
                stopRecording();
            } else {
                discard();
            }
        }
    }
}
