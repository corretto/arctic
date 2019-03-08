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
package com.amazon.corretto.arctic.player.backend.impl;

import javax.inject.Inject;

import com.amazon.corretto.arctic.common.backend.ArcticScreenRecorder;
import com.amazon.corretto.arctic.common.gui.ShadeManager;
import com.amazon.corretto.arctic.common.gui.WorkbenchManager;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.model.event.ScreenshotCheck;
import com.amazon.corretto.arctic.player.backend.ArcticBackendPlayer;
import com.amazon.corretto.arctic.player.backend.ImageComparator;
import com.amazon.corretto.arctic.player.control.TimeController;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.shared.exception.ArcticException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A backend player that validates ScreenChecks. Given a recorded screenChecks, this player will attempt to replicate
 * the screen state, take a screenshot and compare the recorded and current screenshots to see if the contents on the
 * screen are equivalent.
 */
public final class ArcticImageCheckPlayer implements ArcticBackendPlayer {
    private static final Logger log = LoggerFactory.getLogger(ArcticImageCheckPlayer.class);

    public static final String NAME = "sc";

    private final ImageComparator imgComparator;
    private final ArcticScreenRecorder recorder;
    private final WorkbenchManager wbManager;
    private final ShadeManager shadeManager;
    private final TimeController timeController;
    private TestId runningTestId;
    private String runningTestScope;

    /**
     * Creates a new instance of the player, used by the DI software.
     * @param imgComparator Used to compare both recorded and current images.
     * @param recorder Used to capture the contents of the screen.
     * @param wbManager To handle the Workbench and restore it to the original position.
     * @param shadeManager To handle the different shades and restore them to the original positions.
     * @param timeController Needed to control how much time we wait after the different screenshots to allow the system
     *                       time enough to redraw the screen.
     */
    @Inject
    public ArcticImageCheckPlayer(final ImageComparator imgComparator, final ArcticScreenRecorder recorder,
                                  final WorkbenchManager wbManager, final ShadeManager shadeManager,
                                  final TimeController timeController) {
        this.imgComparator = imgComparator;
        this.recorder = recorder;
        this.wbManager = wbManager;
        this.shadeManager = shadeManager;
        this.timeController = timeController;
    }

    @Override
    public int supportedSubTypes() {
        return ArcticEvent.SubType.SCREENSHOT_CHECK.getValue();
    }

    @Override
    public boolean processEvent(final ArcticEvent ev) {
        validate(ev);
        final ScreenshotCheck saved = (ScreenshotCheck) ev;
        wbManager.position(saved.getWorkbench());
        shadeManager.position(saved.getShades());
        timeController.waitForScreen();
        final ScreenshotCheck current = recorder.capture(saved.getSa());

        return imgComparator.compare(current, saved, runningTestId, runningTestScope);
    }

    private void validate(final ArcticEvent ev) {
        if (!(ev instanceof ScreenshotCheck)) {
            log.error("Received event with wrong class. Type was {}:{} while class was: {}", ev.getType(),
                    ev.getSubType(), ev.getClass().getSimpleName());
            throw new ArcticException("Received class " + ev.getClass().getSimpleName() + ". Expected: "
                    + ScreenshotCheck.class.getSimpleName());
        }
    }

    @Override
    public void init(final ArcticRunningTest test) {
        this.runningTestId = test.getTestId();
        this.runningTestScope = test.getRecording().getScope();
    }
}
