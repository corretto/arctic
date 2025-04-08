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
package com.amazon.corretto.arctic.recorder.identification.impl;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.backend.ArcticScreenRecorder;
import com.amazon.corretto.arctic.common.gui.WorkbenchManager;
import com.amazon.corretto.arctic.common.model.gui.Point;
import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import com.amazon.corretto.arctic.recorder.identification.ArcticTestWindowOffsetCalculator;
import com.amazon.corretto.arctic.recorder.inject.InjectionKeys;
import com.amazon.corretto.arctic.api.exception.ArcticException;
import lombok.extern.slf4j.Slf4j;

/**
 * In order to record the test, we need to make sure the actual test window has the focus. For that, we will issue a
 * click to the window before the recording starts. That requires finding the positioning of the test window.
 *
 * This class uses the workbench background color to attempt to detect that point.
 */
@Slf4j
public final class AwtRobotOffsetCalculator implements ArcticTestWindowOffsetCalculator {
    private final WorkbenchManager wbManager;
    private final ArcticScreenRecorder screenRecorder;
    private final int extraOffset;
    private final int correctedColor;
    private final Point screenRes;

    @Inject
    public AwtRobotOffsetCalculator(final WorkbenchManager wbManager, final ArcticScreenRecorder screenRecorder,
                                    @Named(InjectionKeys.OFFSET_AWT_EXTRA) final int extraOffset,
                                    @Named(InjectionKeys.OFFSET_AWT_CORRECTED_COLOR) final int correctedColor) {
        this.wbManager = wbManager;
        this.screenRecorder = screenRecorder;
        this.extraOffset = extraOffset;
        this.correctedColor = correctedColor;
        this.screenRes = new Point();
        this.screenRes.setX(Toolkit.getDefaultToolkit().getScreenSize().width);
        this.screenRes.setY(Toolkit.getDefaultToolkit().getScreenSize().height);
    }

    @Override
    public Point getOffset() {
        final ScreenArea wb = wbManager.getScreenArea();
        checkValidScreenArea(wb);
        final int color = correctedColor;
        final BufferedImage image = screenRecorder.capture().getImage();
        log.debug("Color model: {}", image.getColorModel());
        log.debug("Looking for color: {}", color);

        // Our starting x will be the middle of the screen, as we know all the tests are placed there by default.
        // Our starting y will be the workbench y position.
        final Point point = new Point((screenRes.getX() / 2) - wb.getX(), 0);

        // This first scan should find the first point that matches the workbench background
        scanPixelsForColor(image, point, c -> c == color, p -> p.setY(p.getY() + 2));
        log.debug("Found Workbench at {}", point);
        // This scan will match the first point that is not the workbench background, meaning we've found something
        scanPixelsForColor(image, point, c -> c != color, p -> p.setY(p.getY() + 2));
        log.debug("Found TestWindow at {}", point);
        // To account for window decoration casting shadows on other components, we add some extra offset.
        point.setY(point.getY() + extraOffset);
        log.debug("Offset found at {}:{}", point.getX(), point.getY());
        point.setX(point.getX() + wb.getX());
        point.setY(point.getY() + wb.getY());
        return point;
    }

    private void checkValidScreenArea(final ScreenArea sa) {
        if (sa.getX() > screenRes.getX() / 2) {
            log.warn("Workbench is at position {} while screen width is {}", sa.getX(), screenRes.getX());
            throw new ArcticException("Invalid workbench position, it is to the right of the screen");
        } else if (sa.getX() + sa.getW() < screenRes.getX() / 2) {
            log.warn("Workbench is at position {} with width {} while screen width is {}", sa.getX(), sa.getW(), screenRes.getX());
            throw new ArcticException("Invalid workbench position, it is to the left of the screen");
        }
    }

    private void scanPixelsForColor(final BufferedImage image, final Point point, final Function<Integer, Boolean> matchFunction, final Consumer<Point> searchFunction) {
        while(point.getX() < image.getWidth() && point.getY() < image.getHeight()) {
            //log.debug("Point {}:{} yielded color: {}", point.getX(), point.getY(), image.getRGB(point.getX(), point.getY()) & 0xFFFFFF);
            if (matchFunction.apply(image.getRGB(point.getX(), point.getY()) & 0xFFFFFF)) {
                return;
            }
            searchFunction.accept(point);
        }
        log.warn("Unable to locate the test within the work area. Last searched point: {}", point);
        log.warn("Workbench area {}", wbManager.getWorkbench().getSa());
        throw new ArcticException("Workbench is not visible");
    }
}
