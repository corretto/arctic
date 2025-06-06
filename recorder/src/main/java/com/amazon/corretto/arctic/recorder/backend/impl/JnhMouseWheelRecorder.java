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
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelListener;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JnhMouseWheelRecorder extends JnhRecorder implements NativeMouseWheelListener {
    public static final String NAME = "jnhMouseWheel";
    private final Function<NativeMouseWheelEvent, MouseEvent> converter;

    @Inject
    public JnhMouseWheelRecorder(final Function<NativeMouseWheelEvent, MouseEvent> converter) {
        super(NAME);
        log.debug("jnhMouseWheelRecorder loaded");
        this.converter = converter;
    }

    @Override
    public void nativeMouseWheelMoved(final NativeMouseWheelEvent nativeMouseWheelEvent) {
        recordEvent(converter.apply(nativeMouseWheelEvent));
    }

    @Override
    public void start() {
        GlobalScreen.addNativeMouseWheelListener(this);
    }

    @Override
    public void stop() {
        GlobalScreen.removeNativeMouseWheelListener(this);
    }
}
