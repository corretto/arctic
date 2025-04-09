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

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.player.backend.ArcticBackendPlayer;
import com.amazon.corretto.arctic.player.backend.converters.ArcticMouseEvent2JnhMouseEvent;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.api.exception.ArcticException;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This backend player will post mouse button and motion events using the jNativeHook library.
 * This player does not support mouse wheel events, {@see {@link JnhMouseWheelBackendPlayer}}
 *
 * To ensure coherence when posting mouse events (and not leave the mouse clicked after the recording is over), state of
 * the mouse is persisted and undone during the cleanup phase.
 */
public final class JnhMouseBackendPlayer implements ArcticBackendPlayer {
    private static final Logger log = LoggerFactory.getLogger(JnhMouseBackendPlayer.class);

    public static final String NAME = "jnhMouse";

    private final ArcticMouseEvent2JnhMouseEvent converter;
    private final int events;
    private final Map<Integer, MouseEvent> mouseState = new HashMap<>();

    private int offsetX = 0;
    private int offsetY = 0;

    /**
     * Creates a new instance. Called by the Dependency Injection framework.
     * @param converter A converter able to transform ArcticMouse events into jNativeHook {@link NativeMouseEvent}
     * @param events Bitmask that represents the events that this player should reproduce.
     */
    @Inject
    public JnhMouseBackendPlayer(final ArcticMouseEvent2JnhMouseEvent converter,
                                 @Named(InjectionKeys.BACKEND_PLAYERS_JNH_MOUSE_EVENTS) final int events) {
        this.converter = converter;
        this.events = events;
    }

    @Override
    public int supportedSubTypes() {
        return ArcticEvent.SubType.PRESS.getValue()
                | ArcticEvent.SubType.RELEASE.getValue()
                | ArcticEvent.SubType.CLICK.getValue()
                | ArcticEvent.SubType.MOVE.getValue()
                | ArcticEvent.SubType.DRAG.getValue();
    }

    @Override
    public boolean processEvent(final ArcticEvent ev) {
        if (!ev.getSubType().inMask(events)) {
            return true;
        }

        validate(ev);
        final MouseEvent me = (MouseEvent) ev;

        if (ArcticEvent.SubType.PRESS.equals(me.getSubType())) {
            if (mouseState.containsKey(me.getButton())) {
                // We ignore duplicate mouse press events
                return true;
            }
            mouseState.put(me.getButton(), me);
        }

        if (ArcticEvent.SubType.RELEASE.equals(me.getSubType())) {
            if (!mouseState.containsKey(me.getButton())) {
                // We ignore release events for unpressed mouse buttons
                return true;
            }
            mouseState.remove(me.getButton());
        }

        final NativeMouseEvent nme = converter.convert(me, offsetX, offsetY);
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
        mouseState.values().forEach(it -> {
            it.setSubType(ArcticEvent.SubType.RELEASE);
            final NativeMouseEvent nme = converter.convert(it, offsetX, offsetY);
            GlobalScreen.postNativeEvent(nme);
        });
        mouseState.clear();
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
