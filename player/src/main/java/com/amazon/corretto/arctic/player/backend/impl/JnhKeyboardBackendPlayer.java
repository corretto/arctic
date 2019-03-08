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
import java.util.function.Function;
import javax.inject.Inject;

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.model.event.KeyboardEvent;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.player.backend.ArcticBackendPlayer;
import com.amazon.corretto.arctic.shared.exception.ArcticException;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JnhKeyboardBackendPlayer implements ArcticBackendPlayer {
    private static final Logger log = LoggerFactory.getLogger(JnhKeyboardBackendPlayer.class);
    public static final String NAME = "jnhKeyboard";

    private final Function<KeyboardEvent, NativeKeyEvent> converter;
    private final Map<Integer, KeyboardEvent> keyState = new HashMap<>();

    @Inject
    public JnhKeyboardBackendPlayer(final Function<KeyboardEvent, NativeKeyEvent> converter) {
        this.converter = converter;
    }

    @Override
    public int supportedSubTypes() {
        return ArcticEvent.SubType.KEY_PRESSED.getValue()
                | ArcticEvent.SubType.KEY_RELEASED.getValue()
                | ArcticEvent.SubType.KEY_TYPED.getValue();
    }

    @Override
    public boolean processEvent(final ArcticEvent ev) {
        validate(ev);
        final KeyboardEvent ke = (KeyboardEvent) ev;

        if (ArcticEvent.SubType.KEY_PRESSED.equals(ke.getSubType())) {
            if (keyState.containsKey(ke.getKeyCode())) {
                // We ignore duplicate key press events
                return true;
            }
            keyState.put(ke.getKeyCode(), ke);
        }

        if (ArcticEvent.SubType.KEY_RELEASED.equals(ke.getSubType())) {
            if (!keyState.containsKey(ke.getKeyCode())) {
                // We ignore release events for unpressed keys
                return true;
            }
            keyState.remove(ke.getKeyCode());
        }

        postEvent(ke);
        return true;
    }

    @Override
    public void cleanup() {
        keyState.values().forEach(it -> {
            it.setSubType(ArcticEvent.SubType.KEY_RELEASED);
            postEvent(it);
        });
        keyState.clear();
    }

    private void postEvent(final KeyboardEvent ke) {
        final NativeKeyEvent nke = converter.apply(ke);
        GlobalScreen.postNativeEvent(nke);
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
