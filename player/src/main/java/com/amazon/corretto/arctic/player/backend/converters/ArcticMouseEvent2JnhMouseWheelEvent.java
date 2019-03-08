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

import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;

/**
 * This class transforms Arctic representations of mouse wheel events to the ones used by jNativeHook, so they can be
 * posted during replay.
 */
public final class ArcticMouseEvent2JnhMouseWheelEvent {

    /**
     * Converts from an Arctic event to a NativeMouseWheelEvent to use with jNativeHook.
     * @param mouseEvent Arctic mouse event to convert.
     * @param offsetX An offset to apply to the X position.
     * @param offsetY An offset to apply to the Y position.
     * @return NativeMouseWheelEvent equivalent to the arctic event.
     */
    public NativeMouseWheelEvent convert(final MouseEvent mouseEvent, final int offsetX, final int offsetY) {
        return new NativeMouseWheelEvent(
                ArcticEvent2JnhMappings.MOUSE_MAPPINGS.get(mouseEvent.getSubType()),
                mouseEvent.getModifiers(),
                mouseEvent.getX() + offsetX,
                mouseEvent.getY() + offsetY,
                mouseEvent.getClickCount(),
                mouseEvent.getScrollType(),
                mouseEvent.getScrollAmount(),
                mouseEvent.getWheelRotation(),
                mouseEvent.getWheelRotation()
        );
    }
}
