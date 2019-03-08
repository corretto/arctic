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

import com.amazon.corretto.arctic.common.backend.impl.AwtRobotWindowFocusManager;
import com.amazon.corretto.arctic.common.model.gui.Point;
import com.google.inject.ImplementedBy;

/**
 * Ensures the desire window has focus when recording and reproducing tests.
 *
 * This interface has a default implementation on {@link AwtRobotWindowFocusManager}.
 */
@ImplementedBy(AwtRobotWindowFocusManager.class)
public interface ArcticTestWindowFocusManager {

    /**
     * Gives focus to a window by performing a single mouse click on the selected coordinates. Coordinates are relative
     * to the workbench.
     * @param focusPoint Point with coordinates for the click.
     */
    void giveFocus(Point focusPoint);
}
