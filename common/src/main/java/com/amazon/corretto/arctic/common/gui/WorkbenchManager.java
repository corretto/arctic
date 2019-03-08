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

package com.amazon.corretto.arctic.common.gui;

import java.awt.Color;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.common.model.gui.ArcticFrame;
import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles the position of the Workbench. The workbench is a window that usually sits at the back that defines the area
 * we will use to capture screenshots. Anything outside the workbench bounds is not captured.
 */
@Slf4j
@Singleton
public final class WorkbenchManager {
    private JFrame wb;
    private final String defaultTitle;
    private final int defaultWidth;
    private final int defaultHeight;
    private final int defaultColor;

    @Inject
    public WorkbenchManager(@Named(CommonInjectionKeys.WORKBENCH_DEFAULT_TITLE) final String defaultTitle,
            @Named(CommonInjectionKeys.WORKBENCH_DEFAULT_WIDTH) final int defaultWidth,
            @Named(CommonInjectionKeys.WORKBENCH_DEFAULT_HEIGHT) final int defaultHeight,
            @Named(CommonInjectionKeys.WORKBENCH_DEFAULT_COLOR) final int defaultColor) {
        this.defaultTitle = defaultTitle;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
        this.defaultColor = defaultColor;
        initializeWorkbench();
    }

    /**
     * Get the bounds of the workbench.
     * @return ScreenArea object representing the position and size of the workbench.
     */
    public ScreenArea getScreenArea() {
        initializeWorkbench();
        return new ScreenArea(wb.getBounds());
    }

    /**
     * Capture all relevant details of the workbench, like position, size, title and color. These can be useful to
     * restore the state of the workbench by calling {@link WorkbenchManager#position(ArcticFrame)}
     * @return An {@link ArcticFrame} that represents the current state of the workbench.
     */
    public ArcticFrame getWorkbench() {
        initializeWorkbench();
        return new ArcticFrame(wb.getTitle(), wb.getBackground().getRGB(), new ScreenArea(wb.getBounds()));
    }

    /**
     * Restores a previously stored state of the Workbench. Workbench state can be stored using
     * {@link WorkbenchManager#getWorkbench()}
     * @param workbench An {@link ArcticFrame} that represents the desired state of the workbench.
     */
    public void position(final ArcticFrame workbench) {
        initializeWorkbench();
        if (!Objects.equals(workbench.getTitle(), wb.getTitle())) {
            wb.setTitle(workbench.getTitle());
        }
        if (!Objects.equals(wb.getBounds(), workbench.getSa().asRectangle())) {
            wb.setBounds(workbench.getSa().asRectangle());
        }
        final Color color = new Color(workbench.getColor());
        if (!Objects.equals(wb.getBackground(), color)) {
            wb.getContentPane().setBackground(color);
            wb.setBackground(color);
        }
        if (!wb.isVisible()) {
            wb.setVisible(true);
        }
        wb.toBack();
    }

    /**
     * Ensure the workbench is at the back of the screen.
     */
    public void toBack() {
        wb.toBack();
    }

    private void initializeWorkbench() {
        if (wb == null) {
            wb = new JFrame(defaultTitle);
            wb.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            wb.setFocusableWindowState(false);
            wb.setSize(defaultWidth, defaultHeight);
            wb.setLocationRelativeTo(null);
            final JPanel bgPanel = new JPanel();
            bgPanel.setBackground(new Color(defaultColor));
            wb.setBackground(new Color(defaultColor));
            wb.setContentPane(bgPanel);
            log.debug("Workbench background {}", defaultColor);
            wb.setVisible(true);
            wb.toBack();
        }
    }

    public void hide() {
        wb.setVisible(false);
    }

    public void show() {
        wb.setVisible(true);
    }
}
