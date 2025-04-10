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

package com.amazon.corretto.arctic.recorder.control.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazon.corretto.arctic.recorder.control.ArcticController;
import com.amazon.corretto.arctic.recorder.inject.InjectionKeys;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * This class allows the operator to interact with arctic during the recording of tests. It does so by listening to
 * keyboard events and execute some predetermine actions for specific combinations, like start/stop the recording or do
 * a ScreenshotCheck. It can also be used to spawn new shades if needed.
 */
@Slf4j
@Singleton
public final class JnhKeyCaptureController implements ArcticController, NativeKeyListener {
    private final int startKeyCode;
    private final int stopKeyCode;
    private final int screenCheckKeyCode;
    private final int spawnShadeKeyCode;
    private final int discardKeyCode;
    private final Map<Integer, Boolean> modifiers = new HashMap<>();

    private boolean isRecording = false;
    private Listener controlInterface;

    /**
     * Constructor to be used by Guice for dependency injection.
     * @param startKeyCode KeyCode corresponding to the start recording event.
     * @param stopKeyCode KeyCode corresponding to the stop recording event.
     * @param screenCheckKeyCode Keycode to request a new ScreenshotCheck to be added to the recording.
     * @param spawnShadeKeyCode Keycode to spawn a new shade.
     * @param discardKeyCode Keycode to discard the current recording. The pipeline is flushed without saving the test.
     * @param modifiers A set of modifiers that need to be present (Ctrl/Alt/...) in order for the keycodes to trigger.
     */
    @Inject
    public JnhKeyCaptureController(@Named(InjectionKeys.CONTROL_JNH_START_KEYCODE) final int startKeyCode,
            @Named(InjectionKeys.CONTROL_JNH_STOP_KEYCODE) final int stopKeyCode,
            @Named(InjectionKeys.CONTROL_JNH_SCREEN_CHECK_KEYCODE) final int screenCheckKeyCode,
            @Named(InjectionKeys.CONTROL_JNH_SPAWN_SHADE_KEYCODE) final int spawnShadeKeyCode,
            @Named(InjectionKeys.CONTROL_JNH_DISCARD_KEYCODE) final int discardKeyCode,
            @Named(InjectionKeys.CONTROL_JNH_MODIFIERS) final List<Integer> modifiers) {
        this.startKeyCode = startKeyCode;
        this.stopKeyCode = stopKeyCode;
        this.screenCheckKeyCode = screenCheckKeyCode;
        this.spawnShadeKeyCode = spawnShadeKeyCode;
        this.discardKeyCode = discardKeyCode;
        modifiers.forEach(it -> this.modifiers.put(it, false));
    }

    @Override
    public void register(final Listener listener) {
        log.debug("Control registered");
        this.controlInterface = listener;
        listener.acceptEvent(Event.SPAWN_SHADE);
        registerHook();
        GlobalScreen.addNativeKeyListener(this);
    }

    @Override
    public void nativeKeyPressed(final NativeKeyEvent nativeEvent) {
        processModifiers(nativeEvent.getKeyCode(), true);
        if (checkModifiers()) {
            if (startKeyCode == nativeEvent.getKeyCode() && stopKeyCode == nativeEvent.getKeyCode()) {
                startStop();
            } else if (startKeyCode == nativeEvent.getKeyCode()) {
                startRecording();
            } else if (stopKeyCode == nativeEvent.getKeyCode()) {
                stopRecording();
            } else if (screenCheckKeyCode == nativeEvent.getKeyCode()) {
                controlInterface.acceptEvent(Event.SCREEN_CHECK);
            } else if (spawnShadeKeyCode == nativeEvent.getKeyCode()) {
                controlInterface.acceptEvent(Event.SPAWN_SHADE);
            } else if (discardKeyCode == nativeEvent.getKeyCode()) {
                controlInterface.acceptEvent(Event.DISCARD);
            }
        }
    }

    @Override
    public void nativeKeyReleased(final NativeKeyEvent nativeEvent) {
        processModifiers(nativeEvent.getKeyCode(), false);
    }

    private void startStop() {
        log.debug("Issue start/stop");
        controlInterface.acceptEvent(Event.START_STOP);
    }

    private void startRecording() {
        log.debug("Issue start");
        controlInterface.acceptEvent(Event.START);
    }

    private void stopRecording() {
        log.debug("Issue stop");
        controlInterface.acceptEvent(Event.STOP);
        isRecording = false;
    }

    private void discardRecording() {
        log.debug("Issue discard");
        controlInterface.acceptEvent(Event.DISCARD);
    }

    private void processModifiers(final int keyCode, final boolean value) {
        if (modifiers.containsKey(keyCode)) {
            modifiers.put(keyCode, value);
        }
    }

    private boolean checkModifiers() {
        return modifiers.values().stream().allMatch(it -> it);
    }

    private void registerHook() {
        if (!GlobalScreen.isNativeHookRegistered()) {
            try {
                GlobalScreen.registerNativeHook();
            } catch (final NativeHookException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
