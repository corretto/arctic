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

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * This represents all the keyboard and mouse events. To speed up processing, these events are stored in a different
 * file that the actual test, with {@link com.amazon.corretto.arctic.common.model.ArcticTest#getEvents()} being backed
 * by a transient property.
 * TODO: Convert to record when migrated to Java 17
 */
@Data
public class Events {
    /**
     * All mouse events in the test.
     */
    private List<MouseEvent> mouseEvents = new ArrayList<>();
    /**
     * All keyboard events in the test.
     */
    private List<KeyboardEvent> keyboardEvents = new ArrayList<>();
}
