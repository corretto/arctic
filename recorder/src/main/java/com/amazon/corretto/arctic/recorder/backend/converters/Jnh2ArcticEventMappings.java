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

package com.amazon.corretto.arctic.recorder.backend.converters;

import java.util.Map;

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;

/**
 * JNH uses it's own event identifiers for Keyboard and Mouse. This class contains mappings to the proper
 * {@link ArcticEvent.SubType}.
 */
public final class Jnh2ArcticEventMappings {
    private Jnh2ArcticEventMappings() { }

    /**
     * Mappings for mouse related events between JNH and Arctic.
     */
    public static final Map<Integer, ArcticEvent.SubType> MOUSE_MAPPINGS = Map.of(
            NativeMouseEvent.NATIVE_MOUSE_CLICKED, ArcticEvent.SubType.CLICK,
            NativeMouseEvent.NATIVE_MOUSE_DRAGGED, ArcticEvent.SubType.DRAG,
            NativeMouseEvent.NATIVE_MOUSE_MOVED, ArcticEvent.SubType.MOVE,
            NativeMouseEvent.NATIVE_MOUSE_PRESSED, ArcticEvent.SubType.PRESS,
            NativeMouseEvent.NATIVE_MOUSE_RELEASED, ArcticEvent.SubType.RELEASE,
            NativeMouseEvent.NATIVE_MOUSE_WHEEL, ArcticEvent.SubType.WHEEL);

    /**
     * Mappings for keyboard related events between JNH and Arctic.
     */
    public static final Map<Integer, ArcticEvent.SubType> KEYBOARD_MAPPINGS = Map.of(
            NativeKeyEvent.NATIVE_KEY_TYPED, ArcticEvent.SubType.KEY_TYPED,
            NativeKeyEvent.NATIVE_KEY_PRESSED, ArcticEvent.SubType.KEY_PRESSED,
            NativeKeyEvent.NATIVE_KEY_RELEASED, ArcticEvent.SubType.KEY_RELEASED);
}


