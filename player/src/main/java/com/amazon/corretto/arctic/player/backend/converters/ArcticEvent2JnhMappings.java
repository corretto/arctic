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

import java.util.Map;

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;

/**
 * Class to map the different {@link ArcticEvent.SubType} to the equivalent {@link NativeMouseEvent} or
 * {@link NativeKeyEvent} from jNativeHook.
 */
public final class ArcticEvent2JnhMappings {

    private ArcticEvent2JnhMappings() { }

    public static final Map<ArcticEvent.SubType, Integer> MOUSE_MAPPINGS = Map.of(
            ArcticEvent.SubType.CLICK, NativeMouseEvent.NATIVE_MOUSE_CLICKED,
            ArcticEvent.SubType.DRAG, NativeMouseEvent.NATIVE_MOUSE_DRAGGED,
            ArcticEvent.SubType.MOVE, NativeMouseEvent.NATIVE_MOUSE_MOVED,
            ArcticEvent.SubType.PRESS, NativeMouseEvent.NATIVE_MOUSE_PRESSED,
            ArcticEvent.SubType.RELEASE, NativeMouseEvent.NATIVE_MOUSE_RELEASED,
            ArcticEvent.SubType.WHEEL, NativeMouseEvent.NATIVE_MOUSE_WHEEL);

    public static final Map<ArcticEvent.SubType, Integer> KEYBOARD_MAPPINGS = Map.of(
            ArcticEvent.SubType.KEY_TYPED, NativeKeyEvent.NATIVE_KEY_TYPED,
            ArcticEvent.SubType.KEY_PRESSED, NativeKeyEvent.NATIVE_KEY_PRESSED,
            ArcticEvent.SubType.KEY_RELEASED, NativeKeyEvent.NATIVE_KEY_RELEASED);
}


