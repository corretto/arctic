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
import javax.inject.Inject;

import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;

public final class JnhNativeMouseWheelEvent2ArcticEvent implements Function<NativeMouseWheelEvent, MouseEvent> {
    private final Function<NativeMouseEvent, MouseEvent> baseConverter;

    @Inject
    public JnhNativeMouseWheelEvent2ArcticEvent(final Function<NativeMouseEvent, MouseEvent> baseConverter) {
        this.baseConverter = baseConverter;
    }

    @Override
    public MouseEvent apply(final NativeMouseWheelEvent nativeMouseWheelEvent) {
        final MouseEvent ev = baseConverter.apply(nativeMouseWheelEvent);
        ev.setScrollAmount(nativeMouseWheelEvent.getScrollAmount());
        ev.setScrollType(nativeMouseWheelEvent.getScrollType());
        ev.setWheelDirection(nativeMouseWheelEvent.getWheelDirection());
        ev.setWheelRotation(nativeMouseWheelEvent.getWheelRotation());
        return ev;
    }
}
