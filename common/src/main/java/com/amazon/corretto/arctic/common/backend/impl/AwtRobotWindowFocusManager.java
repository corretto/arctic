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
package com.amazon.corretto.arctic.common.backend.impl;

import java.awt.Robot;
import java.awt.event.InputEvent;

import com.amazon.corretto.arctic.common.backend.ArcticTestWindowFocusManager;
import com.amazon.corretto.arctic.common.model.gui.Point;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Gives focus to a specific window by posting a Click event using {@link Robot}.
 */
@Slf4j
public class AwtRobotWindowFocusManager implements ArcticTestWindowFocusManager {
    public static final String NAME = "awt";
    private final Robot robot;

    /**
     * Creates a new instance. Called by the dependency injection framework.
     * @param robot An instance of AWT robot that is used to post the mouse press and release to simulate the click.
     */
    @Inject
    public AwtRobotWindowFocusManager(final Robot robot) {
        this.robot = robot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void giveFocus(final Point point) {
        log.debug("Attempting to get focus on point {}:{}", point.getX(), point.getY());
        postMouseClick(point.getX(), point.getY());
    }

    private void postMouseClick(final int x, final int y) {
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(30);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }
}
