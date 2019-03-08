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

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.recorder.backend.ArcticBackendRecorder;
import com.amazon.corretto.arctic.recorder.control.ArcticController;
import lombok.Getter;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;

public abstract class JnhRecorder implements ArcticBackendRecorder {
    private boolean registeredHook = false;
    @Getter private List<ArcticEvent> recordingBuffer;
    @Getter private final String name;

    public JnhRecorder(final String name) {
        this.name = name;
    }

    @Override
    public void acceptEvent(final ArcticController.Event event) {
        switch (event) {
            case START:
                recordingBuffer = new LinkedList<>();
                registerHook();
                start();
                break;
            case STOP:
            case DISCARD:
                stop();
                unregisterHook();
                break;
            default:
        }
    }

    protected abstract void start();
    protected abstract void stop();


    private void unregisterHook() {
        if (registeredHook) {
            try {
                GlobalScreen.unregisterNativeHook();
                registeredHook = false;
            } catch (final NativeHookException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void registerHook() {
        if (!GlobalScreen.isNativeHookRegistered()) {
            try {
                GlobalScreen.registerNativeHook();
                registeredHook = true;
            } catch (final NativeHookException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void recordEvent(final ArcticEvent ev) {
        recordingBuffer.add(ev);
    }

}
