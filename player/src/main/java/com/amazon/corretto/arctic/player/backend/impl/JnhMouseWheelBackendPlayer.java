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
package com.amazon.corretto.arctic.player.backend.impl;

import javax.inject.Inject;

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.player.backend.ArcticBackendPlayer;
import com.amazon.corretto.arctic.player.backend.converters.ArcticMouseEvent2JnhMouseWheelEvent;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.api.exception.ArcticException;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This backend player will post mouse wheel events using the jNativeHook library.
 * This player does not support mouse click or motion events, {@see {@link JnhMouseBackendPlayer}}
 */
public final class JnhMouseWheelBackendPlayer implements ArcticBackendPlayer {
    private static final Logger log = LoggerFactory.getLogger(JnhMouseWheelBackendPlayer.class);

    public static final String NAME = "jnhMouseWheel";

    private final ArcticMouseEvent2JnhMouseWheelEvent converter;

    private int offsetX = 0;
    private int offsetY = 0;

    /**
     * Creates a new instance. Called by the Dependency Injection framework.
     * @param converter A converter able to transform ArcticMouse events into jNativeHook {@link NativeMouseWheelEvent}
     */
    @Inject
    public JnhMouseWheelBackendPlayer(final ArcticMouseEvent2JnhMouseWheelEvent converter) {
        this.converter = converter;
    }

    @Override
    public int supportedSubTypes() {
        return ArcticEvent.SubType.WHEEL.getValue();
    }

    @Override
    public boolean processEvent(final ArcticEvent ev) {
        validate(ev);
        final MouseEvent me = (MouseEvent) ev;
        final NativeMouseWheelEvent nme = converter.convert(me, offsetX, offsetY);
        GlobalScreen.postNativeEvent(nme);
        return true;
    }

    @Override
    public void init(final ArcticRunningTest test) {
        offsetX = test.getRecording().getMouseOffsets().getX();
        offsetY = test.getRecording().getMouseOffsets().getY();
    }

    @Override
    public void cleanup() {
        offsetX = 0;
        offsetY = 0;
    }

    private void validate(final ArcticEvent ev) {
        if (!(ev instanceof MouseEvent)) {
            log.error("Received event with wrong class. Type was {}:{} while class was: {}", ev.getType(),
                    ev.getSubType(), ev.getClass().getSimpleName());
            throw new ArcticException("Received class " + ev.getClass().getSimpleName() + ". Expected: "
                    + ScreenshotCheck.class.getSimpleName());
        }
    }
}
