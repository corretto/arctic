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
package com.amazon.corretto.arctic.player.backend.converters;

import java.util.function.Function;

import com.amazon.corretto.arctic.common.model.event.KeyboardEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

/**
 * This class transforms Arctic representations of Keyboard events to the ones used by jNativeHook, so they can be
 * posted during replay.
 */
public final class ArcticKeyboardEvent2JnhKeyEvent implements Function<KeyboardEvent, NativeKeyEvent> {
    @Override
    public NativeKeyEvent apply(final KeyboardEvent keyboardEvent) {
        final char keyChar = keyboardEvent.getKeyChar() != null ? keyboardEvent.getKeyChar()
                : NativeKeyEvent.CHAR_UNDEFINED;
        return new NativeKeyEvent(ArcticEvent2JnhMappings.KEYBOARD_MAPPINGS.get(keyboardEvent.getSubType()),
                keyboardEvent.getModifiers(),
                keyboardEvent.getRawCode(),
                keyboardEvent.getKeyCode(),
                keyChar,
                keyboardEvent.getKeyLocation());
    }
}
