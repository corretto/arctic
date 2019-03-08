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

import lombok.Getter;

/**
 * Interface that represent a single event that is processed by Arctic.
 */
public interface ArcticEvent {
    /**
     * The amount of time that passed since the beginning of the recording of the test until the recording of this
     * event.
     * @return nanoseconds elapsed since the beginning of the recording until this event.
     */
    long getTimestamp();

    /**
     * Sets the timestamp of the event. This exists because initially the event has an absolute timestamp, but they are
     * later all stored as relative times from the recording start.
     * @param value value in nanoseconds to store.
     */
    void setTimestamp(long value);

    /**
     * What type of even this corresponds to. All events of the same type are represented by the same class.
     * @return The type this event corresponds to.
     */
    Type getType();

    /**
     * A specific subtype for the event. Subtypes are global (so no two different types should share the same subtype).
     * Two subtypes may be represented by the same class (for example, {@link SubType#PRESS} and {@link SubType#MOVE}
     * are both {@link Type#MOUSE_EVENT}. Each subtype has a value. These values are unique and powers of two, each one
     * representing a specific bit of the reproduction mode mask. They do not need to be consecutive. Each byte,
     * starting for the least significant byte represents: keyboard events, mouse button events, mouse motion events and
     * ScreenCheckEvents.
     * @return A subtype for this event.
     */
    SubType getSubType();

    /**
     * Represents the three major types of events, Mouse, Keyboard and ScreenshotChecks.
     */
    enum Type {
        MOUSE_EVENT(1 << 1),
        KEYBOARD_EVENT(1 << 2),
        SCREENSHOT_CHECK(1 << 3);

        @Getter private final int value;

        Type(final int value) {
            this.value = value;
        }
    }

    /**
     * Subtype is a shared enumeration that covers all the different events. This enum is often use to build recording
     * and reproduction masks, indicating which events should be ignored and which ones should be processed.
     */
    enum SubType {

        /**
         * This represents the abstract idea of a character being type on the screen, as the result of pressing a key.
         * Proper recording of this even requires knowing the current keymap. Although we record this event (in those
         * platforms that it is available), we don't reproduce it by default. Modifier keys are not stored independently
         * but as part of the other characters being typed. Reproduction of this event alongside KEY_PRESSED and
         * KEY_RELEASED can cause duplicates.
         */
        KEY_TYPED(1),

        /**
         * Represents the idea of a physical key being pressed on the keyboard. This event ignores the keymap and
         * captures modifier keys.
         */
        KEY_PRESSED(1 << 1),

        /**
         * Counterpart to KEY_PRESSED, represents the idea of a physical key being released on the keyboard. This event
         * ignores the keymap and captures modifier keys.
         */
        KEY_RELEASED(1 << 2),

        /**
         * Represents the virtual idea of clicking on the screen. This event is recorder but usually ignored as it
         * can lead to duplicates with other events.
         */
        CLICK(1 << 8),

        /**
         * Represents a button in the mouse being press.
         */
        PRESS(1 << 9),

        /**
         * Counterpart to Release. Represents a button being released.
         */
        RELEASE(1 << 10),

        /**
         * Represents the mouse wheel going up or down (and left right if supported). For clicking purposes, the wheel
         * is considered a regular button.
         */
        WHEEL(1 << 11),

        /**
         * Represents mouse movement while one or more buttons are being pressed.
         */
        DRAG(1 << 12),

        /**
         * Represents mouse movement when no buttons are being pressed.
         */
        MOVE(1 << 13),

        /**
         * An event that checks the current screen state matches the recording.
         */
        SCREENSHOT_CHECK(1 << 16);

        @Getter private final int value;

        SubType(final int value) {
            this.value = value;
        }

        /**
         * Checks if an specific integer representing a mask contains the bit for this specific enum value.
         * @param mask Integer representing the mask.
         * @return True if the relevant bit representing this enum value is set to 1.
         */
        public boolean inMask(final int mask) {
            return (value & mask) != 0;
        }
    }
}
