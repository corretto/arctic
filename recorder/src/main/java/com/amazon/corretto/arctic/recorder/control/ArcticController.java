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
package com.amazon.corretto.arctic.recorder.control;

import com.amazon.corretto.arctic.recorder.control.impl.JnhKeyCaptureController;
import com.google.inject.ImplementedBy;

/**
 * An ArcticController manages the flow of the recorder. It generates {@link Event} that will send to the
 * {@link Listener} so they can act accordingly. The default implementation is {@link JnhKeyCaptureController}, which
 * translates keyboard inputs into events.
 */
@ImplementedBy(JnhKeyCaptureController.class)
public interface ArcticController {
    /**
     * Register a listener into this controller so it will start receiving {@link Event} from it.
     * @param listener The listener we want to register.
     */
    void register(Listener listener);

    /**
     * A listener that can be registered into an {@link ArcticController} and will receive {@link Event} to process.
     */
    interface Listener {
        /**
         * Process a specific Event.
         * @param event The event to process.
         */
        void acceptEvent(Event event);
    }

    /**
     * Defines a set of events that control the recorder. An implementation of
     * {@link ArcticController} will generate this events, for example, when the
     * user presses specific key combinations.
     */
    enum Event {
        /**
         * Starts or stops the recording.
         */
        START_STOP,

        /**
         * Start the recording process.
         */
        START,

        /**
         * Stop the recording process. Attempt to save the recording.
         */
        STOP,

        /**
         * Stop the recording process, clear the buffers, but do not save the recording.
         */
        DISCARD,

        /**
         * Create a new window that can be used to hide areas of the screen we don't want to record.
         */
        SPAWN_SHADE,

        /**
         * Record a screen check. This will require to capture a screenshot and it will be validated again during
         * playback.
         */
        SCREEN_CHECK
    }


}
