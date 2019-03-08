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

import com.amazon.corretto.arctic.common.serialization.GsonIgnoreZeroIntAdapter;
import com.google.gson.annotations.JsonAdapter;
import lombok.Data;

@Data
public class MouseEvent implements ArcticEvent {
    /**
     * What type of mouse event this instance represents, like move or click.
     */
    private ArcticEvent.SubType subType;

    /**
     * When this event happened relative to the start of the recording
     */
    private long timestamp;

    /**
     * x coordinate on the screen.
     */
    private int x;

    /**
     * y coordinate on the screen.
     */
    private int y;

    /**
     * If relevant, which button was being pressed (will be 0 for move events).
     */
    @JsonAdapter(GsonIgnoreZeroIntAdapter.class)
    private int button;

    /**
     * If relevant, how many clicks were issued (for Click events, to represent double click).
     */
    @JsonAdapter(GsonIgnoreZeroIntAdapter.class)
    private int clickCount;

    /**
     * Keyboard modifiers that were pressed at the moment of the event.
     */
    private int modifiers;


    // wheel related properties
    @JsonAdapter(GsonIgnoreZeroIntAdapter.class)
    private int scrollAmount;
    @JsonAdapter(GsonIgnoreZeroIntAdapter.class)
    private int scrollType;
    @JsonAdapter(GsonIgnoreZeroIntAdapter.class)
    private int wheelRotation;
    @JsonAdapter(GsonIgnoreZeroIntAdapter.class)
    private int wheelDirection;

    @Override
    public ArcticEvent.Type getType() {
        return ArcticEvent.Type.MOUSE_EVENT;
    }
}


