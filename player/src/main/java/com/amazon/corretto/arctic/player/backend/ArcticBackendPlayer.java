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

package com.amazon.corretto.arctic.player.backend;

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;

/**
 * Basic interface for a BackendPlayer. A BackendPlayer is any class that is capable of replaying previously recorded
 * events.
 */
public interface ArcticBackendPlayer {
    /**
     * A mask that represent the different {@link ArcticEvent.SubType} the implementing backend player is capable of
     * replaying. The player will not receive events it does not support.
     * @return An integer representing the supported subtypes. Multiple subtypes can be returned using an or mask.
     */
    int supportedSubTypes();

    /**
     * Process an specific event. This is the method that will be called when we expect the implementing backend
     * player to replay a recorded event.
     * @param e The recorded event that needs to be replayed.
     * @return True if the event was replayed successfully. False if this event should cause the test to be failed.
     */
    boolean processEvent(ArcticEvent e);

    /**
     * A method that is called before we start reproducing all the events for an specific test. By default, an empty
     * implementation is supplied.
     * @param test Test that will be replayed.
     */
    default void init(final ArcticRunningTest test) { }

    /**
     * A method that is called after we finish reproducing all the events for an specific test. By default, an empty
     * implementation is supplied.
     */
    default void cleanup() { }

    /**
     * Method to check if the implementing backend player accepts an specific event type. A default implementation that
     * relies on the supportedSubTypes is supplied.
     * @param eventSubType Type of event to check
     * @return True if the player supports replaying that specific event.
     */
    default boolean acceptsEvent(final ArcticEvent.SubType eventSubType) {
        return eventSubType.inMask(supportedSubTypes());
    }

    /**
     * Consumes an event. This includes all events, even those the implementing backed player does not support. A
     * default implementation that filters based on supported types and calls processEvent is supplied.
     * @param e Event that needs to be consumed.
     * @return True if the event was skipped or replayed successfully. False if this event should cause the test to be
     * failed.
     */
    default boolean consumeEvent(final ArcticEvent e) {
        if (acceptsEvent(e.getSubType())) {
            return processEvent(e);
        }
        return true;
    }
}
