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
package com.amazon.corretto.arctic.common.backend;

import com.amazon.corretto.arctic.common.backend.impl.AwtRobotScreenRecorder;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import com.google.inject.ImplementedBy;

/**
 * Allows the capture of {@link ScreenshotCheck} based on {@link ScreenArea}.
 *
 * This class has a default implementation on {@link AwtRobotScreenRecorder}
 */
@ImplementedBy(AwtRobotScreenRecorder.class)
public interface ArcticScreenRecorder {
    /**
     * Generates a ScreenshotCheck based on the workbench position.
     * @return ScreenshotCheck with basic fields initialized (image, timestamp)
     */
    ScreenshotCheck capture();

    /**
     * Generates a ScreenshotCheck based on a specific area, ignoring the workbench position.
     * @param area Area we will capture relative to the base area.
     * @return ScreenshotCheck with basic fields initialized (image, timestamp)
     */
    ScreenshotCheck capture(ScreenArea area);
}
