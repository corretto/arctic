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

package com.amazon.corretto.arctic.recorder.backend;

import java.util.List;

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.recorder.backend.impl.CompositeRecorder;
import com.amazon.corretto.arctic.recorder.control.ArcticController;
import com.google.inject.ImplementedBy;

/**
 * Defines a common interface for recording backends of Arctic Recorder. It extends the ArcticControlInterface to
 * include the getRecordingBuffer, where all the events that were recorded were stored.
 */
@ImplementedBy(CompositeRecorder.class)
public interface ArcticBackendRecorder extends ArcticController.Listener {
    /**
     * Returns a list with all the events that were capture since the last time a
     * {@link ArcticController.Event#START} was
     * received.
     * @return A list with the recorded events. Empty if
     * {@link ArcticController.Event#STOP} or
     * {@link ArcticController.Event#DISCARD}
     * was issued after the last {@link ArcticController.Event#START}.
     */
    List<ArcticEvent> getRecordingBuffer();

    /**
     * A name for the recorder.
     * @return The name of the recorder.
     */
    String getName();
}
