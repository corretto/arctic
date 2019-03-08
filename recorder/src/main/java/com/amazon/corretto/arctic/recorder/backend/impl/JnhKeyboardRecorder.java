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

import java.util.function.Function;
import javax.inject.Inject;

import com.amazon.corretto.arctic.common.model.event.KeyboardEvent;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JnhKeyboardRecorder extends JnhRecorder implements NativeKeyListener {
    public static final String NAME = "jnhKeyboard";
    private final Function<NativeKeyEvent, KeyboardEvent> converter;

    @Inject
    public JnhKeyboardRecorder(final Function<NativeKeyEvent, KeyboardEvent> converter) {
        super(NAME);
        log.debug("jnhKeyboardRecorder loaded");
        this.converter = converter;
    }

    @Override
    public void nativeKeyTyped(final NativeKeyEvent nativeKeyEvent) {
        recordEvent(converter.apply(nativeKeyEvent));
    }

    @Override
    public void nativeKeyPressed(final NativeKeyEvent nativeKeyEvent) {
        recordEvent(converter.apply(nativeKeyEvent));
    }

    @Override
    public void nativeKeyReleased(final NativeKeyEvent nativeKeyEvent) {
        recordEvent(converter.apply(nativeKeyEvent));
    }

    @Override
    public void start() {
        GlobalScreen.addNativeKeyListener(this);
    }

    @Override
    public void stop() {
        GlobalScreen.removeNativeKeyListener(this);
    }
}
