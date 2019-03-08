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

import java.util.function.Function;

import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;

public final class JnhNativeMouseEvent2ArcticEvent implements Function<NativeMouseEvent, MouseEvent> {

    @Override
    public MouseEvent apply(final NativeMouseEvent nativeEvent) {
        final MouseEvent ev = new MouseEvent();
        ev.setTimestamp(System.nanoTime());
        ev.setSubType(Jnh2ArcticEventMappings.MOUSE_MAPPINGS.get(nativeEvent.getID()));
        ev.setButton(nativeEvent.getButton());
        ev.setX(nativeEvent.getX());
        ev.setY(nativeEvent.getY());
        ev.setModifiers(nativeEvent.getModifiers());
        ev.setClickCount(nativeEvent.getClickCount());

        return ev;
    }
}
