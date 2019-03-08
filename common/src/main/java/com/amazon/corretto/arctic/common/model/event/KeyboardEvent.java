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

package com.amazon.corretto.arctic.common.model.event;

import lombok.Data;

/**
 * Representation of a keyboard event.
 */
@Data
public final class KeyboardEvent implements ArcticEvent {
    /**
     * Relative to the start of the test, when this event happened.
     */
    private long timestamp;

    /**
     * What type of keyboard event this instance represents, like KeyPress, KeyRelease or KeyType.
     */
    private ArcticEvent.SubType subType;

    /**
     * If relevant, modifiers that were active when the event happened.
     */
    private int modifiers;

    /**
     * For events that support it, the translation into a valid Character.
     */
    private Character keyChar;

    /**
     * Actual keyCode of the event.
     */
    private int keyCode;

    /**
     * Raw keyCode of the event.
     */
    private int rawCode;

    /**
     * Location (Left/Right) for those keys that appear more than once in the keyboard.
     */
    private int keyLocation;

    @Override
    public ArcticEvent.Type getType() {
        return ArcticEvent.Type.KEYBOARD_EVENT;
    }
}
