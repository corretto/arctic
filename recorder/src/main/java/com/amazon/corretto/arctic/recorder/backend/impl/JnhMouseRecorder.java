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

import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * This class records mouse clicks using <a href="https://github.com/kwhat/jnativehook">jnativehook</a>.
 * When working in fullRecordMode, mouse press and releases are recorder instead.
 */
@Slf4j
public final class JnhMouseRecorder extends JnhRecorder implements NativeMouseListener {
    public static final String NAME = "jnhMouse";
    private final Function<NativeMouseEvent, MouseEvent> converter;


    @Inject
    public JnhMouseRecorder(final Function<NativeMouseEvent, MouseEvent> converter) {
        super(NAME);
        log.debug("jnhMouseRecorder loaded");
        this.converter = converter;
    }

    @Override
    public void nativeMouseClicked(final NativeMouseEvent nativeMouseEvent) {
        recordEvent(converter.apply(nativeMouseEvent));
    }

    @Override
    public void nativeMousePressed(final NativeMouseEvent nativeMouseEvent) {
        recordEvent(converter.apply(nativeMouseEvent));
    }

    @Override
    public void nativeMouseReleased(final NativeMouseEvent nativeMouseEvent) {
        recordEvent(converter.apply(nativeMouseEvent));
    }

    @Override
    public void start() {
        GlobalScreen.addNativeMouseListener(this);
    }

    @Override
    public void stop() {
        GlobalScreen.removeNativeMouseListener(this);
    }
}
