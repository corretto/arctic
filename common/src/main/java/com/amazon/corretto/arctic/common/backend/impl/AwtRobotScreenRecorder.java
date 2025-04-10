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
import java.awt.image.BufferedImage;

import com.amazon.corretto.arctic.common.backend.ArcticScreenRecorder;
import com.amazon.corretto.arctic.common.gui.ShadeManager;
import com.amazon.corretto.arctic.common.gui.WorkbenchManager;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Captures a {@link ScreenshotCheck} using {@link Robot}. The ScreenshotCheck contains not only the image with the
 * captured screen, but also de position of the workbench and all the shades, so they can be positioned in the same
 * location during playback.
 */
public class AwtRobotScreenRecorder implements ArcticScreenRecorder {
    private final Robot robot;
    private final WorkbenchManager wbManager;
    private final ShadeManager shadeManager;
    private final int xMargin;
    private final int yMargin;

    /**
     * Creates a new instance of an AwtRobotScreenRecorder.
     * @param robot The AWS Robot used to get the screen data.
     * @param wbManager The workbench manager, used to record the position of the workbench during the capture.
     * @param shadeManager The shade manager, used to record the position of the shades during the capture
     * @param xMargin A margin for the x coordinate. If the workbench is positioned inside the margin, we assume the
     *                position we want to capture starts at 0.
     * @param yMargin A margin for the y coordinate. If the workbench is positioned inside the margin, we assume the
     *                position we want to capture starts at 0.
     */
    @Inject
    public AwtRobotScreenRecorder(final Robot robot, final WorkbenchManager wbManager, final ShadeManager shadeManager,
                                  final @Named(CommonInjectionKeys.SCREEN_CAPTURE_MARGIN_X) int xMargin,
                                  final @Named(CommonInjectionKeys.SCREEN_CAPTURE_MARGIN_Y) int yMargin) {
        this.robot = robot;
        this.wbManager = wbManager;
        this.shadeManager = shadeManager;
        this.xMargin = xMargin;
        this.yMargin = yMargin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScreenshotCheck capture() {
        ScreenArea wbsa = wbManager.getScreenArea();
        ScreenArea targetsa = new ScreenArea(
                wbsa.getX() < xMargin ? 0 : wbsa.getX(),
                wbsa.getY() < yMargin ? 0 : wbsa.getY(),
                wbsa.getX() < xMargin ? wbsa.getW() + wbsa.getX() : wbsa.getW(),
                wbsa.getY() < yMargin ? wbsa.getH() + wbsa.getY() : wbsa.getH());
        return capture(targetsa);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScreenshotCheck capture(final ScreenArea area) {
        final BufferedImage image = robot.createScreenCapture(area.asRectangle());
        final ScreenshotCheck sc = new ScreenshotCheck();
        sc.setImage(image);
        sc.setWorkbench(wbManager.getWorkbench());
        sc.setShades(shadeManager.getShades());
        sc.setSa(area);
        sc.setTimestamp(System.nanoTime());
        return sc;
    }
}
