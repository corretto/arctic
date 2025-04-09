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

import java.awt.Robot;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.amazon.corretto.arctic.player.backend.ArcticBackendPlayer;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.api.exception.ArcticException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This backend player will post mouse button and motion events using the internal java AWT robot.
 * This player does not support mouse wheel events.
 * This player does not support direct mouse click events, but will process mouse press and release events that can
 * cause a click to be interpreted.
 *
 * To ensure coherence when posting mouse events (and not leave the mouse clicked after the recording is over), state of
 * the mouse is persisted and undone during the cleanup phase.
 */
public final class AwtRobotMouseBackendPlayer implements ArcticBackendPlayer {
    public static final String NAME = "awtMouse";

    private static final Logger log = LoggerFactory.getLogger(AwtRobotMouseBackendPlayer.class);

    private final Map<Integer, Integer> mappings;
    private final Robot robot;
    private final int events;
    private final Map<Integer, MouseEvent> mouseState = new HashMap<>();

    private int lastKnownX = -1;
    private int lastKnownY = -1;

    private int offsetX = 0;
    private int offsetY = 0;

    /**
     * Constructor for the player. Used for dependency injection by Guice.
     *
     * Mouse mappings are based on the mask defined in {@link java.awt.event.InputEvent}. Usually these values should
     * be 10, 11 and 12, although order may change depending on the system and device.
     *
     * @param robot An instance of AWT robot used to post the different mouse event.
     * @param button1 Bit of the mask to use for button1 in JNH. Usually 10.
     * @param button2 Bit of the mask to use for button2 in JNH. Usually 11.
     * @param button3 Bit of the mask to use for button3 in JNH. Usually 12.
     * @param events Bitmask that represents the events that this player should reproduce.
     */
    @Inject
    public AwtRobotMouseBackendPlayer(final Robot robot,
                                      @Named(InjectionKeys.BACKEND_PLAYERS_AWT_MOUSE_BUTTON1) final int button1,
                                      @Named(InjectionKeys.BACKEND_PLAYERS_AWT_MOUSE_BUTTON2) final int button2,
                                      @Named(InjectionKeys.BACKEND_PLAYERS_AWT_MOUSE_BUTTON3) final int button3,
                                      @Named(InjectionKeys.BACKEND_PLAYERS_AWT_MOUSE_EVENTS) final int events) {
        this.robot = robot;
        this.events = events;
        mappings = Map.of(
                1, 1 << button1,
                2, 1 << button2,
                3, 1 << button3);
    }

    @Override
    public int supportedSubTypes() {
        return ArcticEvent.SubType.PRESS.getValue()
                | ArcticEvent.SubType.RELEASE.getValue()
                | ArcticEvent.SubType.MOVE.getValue()
                | ArcticEvent.SubType.DRAG.getValue();
    }

    @Override
    public boolean processEvent(final ArcticEvent ev) {
        validate(ev);
        final MouseEvent me = (MouseEvent) ev;
        if (me.getSubType().inMask(events)) {
            switch (me.getSubType()) {
                case PRESS:
                    if (!mouseState.containsKey(me.getButton())) {
                        mousePress(me);
                        mouseState.put(me.getButton(), me);
                    }
                    break;
                case RELEASE:
                    if (mouseState.containsKey(me.getButton())) {
                        mouseState.remove(me.getButton());
                        mouseRelease(me);
                    }
                    break;
                case MOVE:
                case DRAG:
                    mouseMove(me);
                default:
            }
        }
        lastKnownX = me.getX();
        lastKnownY = me.getY();
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
            mouseRelease(it);
        });
        mouseState.clear();
        offsetX = 0;
        offsetY = 0;
    }

    private void mouseMove(final MouseEvent me) {
        robot.mouseMove(me.getX() + offsetX, me.getY() + offsetY);
    }

    private void mousePress(final MouseEvent me) {
        final int awtButton = mappings.getOrDefault(me.getButton(), 0);
        if (awtButton != 0) {
            // There is no guarantee the mouse has previously been moved to the clicked position (arctic may be
            // configured to skip motion events), so we issue an extra move to ensure we click the correct place.
            robot.mouseMove(me.getX() + offsetX, me.getY() + offsetY);
            robot.mousePress(awtButton);
        }
    }

    private void mouseRelease(final MouseEvent me) {
        final int awtButton = mappings.getOrDefault(me.getButton(), 0);
        if (awtButton != 0) {
            if (lastKnownX != me.getX() || lastKnownY != me.getY()) {
                // We are not doing a regular click, but dragging the mouse, we need to ensure we release in the proper
                // position
                robot.mouseMove(me.getX() + offsetX, me.getY() + offsetY);
            }
            robot.mouseRelease(awtButton);
        }
    }

    private void validate(final ArcticEvent ev) {
        if (!(ev instanceof MouseEvent)) {
            log.error("Received event with wrong class. Type was {}:{} while class was: {}", ev.getType(),
                    ev.getSubType(), ev.getClass().getSimpleName());
            throw new ArcticException("Received class " + ev.getClass().getSimpleName() + ". Expected: "
                    + MouseEvent.class.getSimpleName());
        }
    }
}
