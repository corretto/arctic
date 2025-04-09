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
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.model.event.KeyboardEvent;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.player.backend.ArcticBackendPlayer;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.api.exception.ArcticException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backed playback for keyboard events that uses AWT robot to post the events.
 */
public final class AwtRobotKeyboardBackendPlayer implements ArcticBackendPlayer {
    private static final Logger log = LoggerFactory.getLogger(AwtRobotKeyboardBackendPlayer.class);

    public static final String NAME = "awtKeyboard";

    private final Robot robot;
    private final Function<KeyboardEvent, Integer> keyCodeConverter;
    private final Set<Integer> keyState = new HashSet<>();

    /**
     * Creates a new instance, usually called by the dependency injection software.
     * @param robot Used to post the events.
     * @param keyCodeConverter Transforms the codes uses by jnh into valid AWT keycodes.
     */
    @Inject
    public AwtRobotKeyboardBackendPlayer(final Robot robot,
         final @Named(InjectionKeys.BACKEND_PLAYERS_AWT_KB_KEYMAP) Function<KeyboardEvent, Integer> keyCodeConverter) {
        this.robot = robot;
        this.keyCodeConverter = keyCodeConverter;
    }

    @Override
    public int supportedSubTypes() {
        return ArcticEvent.SubType.KEY_PRESSED.getValue()
                | ArcticEvent.SubType.KEY_RELEASED.getValue();
    }

    @Override
    public boolean processEvent(final ArcticEvent ev) {
        validate(ev);
        final KeyboardEvent ke = (KeyboardEvent) ev;
        int awtCode = keyCodeConverter.apply(ke);
        if (awtCode == KeyEvent.VK_UNDEFINED) {
            log.warn("Skipping event with rawCode:{} keyCode{} as it was not found in the mapping",
                    ke.getRawCode(), ke.getKeyCode());
            // Even if we miss this even, we don't want to fail the test because of it.
            return true;
        }
        switch (ke.getSubType()) {
            case KEY_PRESSED:
                if (!keyState.contains(awtCode)) {
                    keyPressed(awtCode);
                    keyState.add(awtCode);
                }
                break;
            case KEY_RELEASED:
                if (keyState.contains(awtCode)) {
                    keyState.remove(awtCode);
                    keyReleased(awtCode);
                }
                break;
            default:
        }
        return true;
    }

    @Override
    public void cleanup() {
        keyState.forEach(robot::keyRelease);
        keyState.clear();
    }

    private void keyReleased(final int awtKeyCode) {
        robot.keyRelease(awtKeyCode);
    }

    private void keyPressed(final int awtKeyCode) {
        robot.keyPress(awtKeyCode);
    }

    private void validate(final ArcticEvent ev) {
        if (!(ev instanceof KeyboardEvent)) {
            log.error("Received event with wrong class. Type was {}:{} while class was: {}", ev.getType(),
                    ev.getSubType(), ev.getClass().getSimpleName());
            throw new ArcticException("Received class " + ev.getClass().getSimpleName() + ". Expected: "
                    + ScreenshotCheck.class.getSimpleName());
        }
    }

}
