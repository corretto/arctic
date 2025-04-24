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
import java.awt.Component;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.common.model.gui.ArcticFrame;
import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Handles the concept of shades in the screen. A shade is a small window that will always be on top and can be used to
 * hide parts of a test that are known to change, like timers and dates.
 */
@Singleton
public class ShadeManager {
    private final String defaultTitle;
    private final int defaultWidth;
    private final int defaultHeight;
    private final int defaultColor;

    private final List<JFrame> shades = new LinkedList<>();

    @Inject
    public ShadeManager(@Named(CommonInjectionKeys.SHADE_DEFAULT_TITLE) final String defaultTitle,
            @Named(CommonInjectionKeys.SHADE_DEFAULT_WIDTH) final int defaultWidth,
            @Named(CommonInjectionKeys.SHADE_DEFAULT_HEIGHT) final int defaultHeight,
            @Named(CommonInjectionKeys.SHADE_DEFAULT_COLOR) final int defaultColor) {
        this.defaultTitle = defaultTitle;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
        this.defaultColor = defaultColor;
    }

    /**
     * Retrieve a list of all currently viewable shades on the screen. This information can be used later to restore the
     * state calling {@link ShadeManager#position(List)}
     * @return List containing the state of all visible shades.
     */
    public List<ArcticFrame> getShades() {
        return shades.stream()
                .filter(Component::isVisible)
                .map(it -> new ArcticFrame(it.getTitle(), it.getBackground().getRGB(), new ScreenArea(it.getBounds())))
                .collect(Collectors.toList());
    }

    /**
     * Ensure that all shades in the screen match a configuration we saved before. This includes the number of shades,
     * size, position, title and color.
     * @param newShades Information listing the different shades we want to display. Previously obtained with
     * {@link ShadeManager#getShades()}.
     */
    public void position(final List<ArcticFrame> newShades) {
        spawnShade(newShades.size() - shades.size());
        final Iterator<JFrame> shadeFrameIterator = shades.iterator();

        for (final ArcticFrame s: newShades) {
            position(s, shadeFrameIterator.next());
        }
        while(shadeFrameIterator.hasNext()) {
            shadeFrameIterator.next().setVisible(false);
        }
    }

    public void spawnShade() {
        shades.stream().filter(it -> !it.isVisible()).findAny().orElseGet(this::createShade).setVisible(true);
    }

    /**
     * Creates a new shade.
     */
    private JFrame createShade() {
        final JFrame shade = new JFrame(defaultTitle);
        shade.setVisible(false);
        shades.add(shade);

        final JPanel bgPanel = new JPanel();
        bgPanel.setBackground(new Color(defaultColor));
        shade.setContentPane(bgPanel);
        shade.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        shade.setSize(defaultWidth, defaultHeight);
        shade.setLocationRelativeTo(null);
        shade.setBackground(new Color(defaultColor));
        shade.setAlwaysOnTop(true);
        return shade;
    }

    public void hideAll() {
        shades.forEach(it -> it.setVisible(false));
    }

    private void spawnShade(final int numberOfShades) {
        for (int i = 0; i < numberOfShades; i++) {
            spawnShade();
        }
    }

    private void position(final ArcticFrame shade, final JFrame shadeFrame) {
        if (!Objects.equals(shade.getSa().asRectangle(), shadeFrame.getBounds())) {
            shadeFrame.setBounds(shade.getSa().asRectangle());
        }
        final Color color = new Color(shade.getColor());
        if (!Objects.equals(color, shadeFrame.getBackground())) {
            shadeFrame.setBackground(new Color(shade.getColor()));
            shadeFrame.getContentPane().setBackground(new Color(shade.getColor()));
        }
        if (!Objects.equals(shade.getTitle(), shadeFrame.getTitle())) {
            shadeFrame.setTitle(shade.getTitle());
        }
        if (!shadeFrame.isVisible()) {
            shadeFrame.setVisible(true);
        }
    }
}
