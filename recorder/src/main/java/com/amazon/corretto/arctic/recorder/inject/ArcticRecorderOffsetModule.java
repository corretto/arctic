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
package com.amazon.corretto.arctic.recorder.inject;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.recorder.identification.ArcticTestWindowOffsetCalculator;
import com.amazon.corretto.arctic.recorder.identification.impl.AwtRobotOffsetCalculator;
import com.amazon.corretto.arctic.recorder.identification.impl.FixedPointOffsetCalculator;
import com.amazon.corretto.arctic.shared.exception.ArcticException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;

import static com.google.inject.name.Names.named;

/**
 * Offset calculation is done to find where the test window lies on the workbench.
 *
 * This module fixes problems with Color correction, when the color we want to display in the screen does not match the
 * color returned by AWT Robot.
 *
 * @see <a href="https://bugs.openjdk.java.net/browse/JDK-8181305>JDK-8181305</a>
 */
@Slf4j
public final class ArcticRecorderOffsetModule extends ArcticModule {
    private static final Map<String, Consumer<ArcticRecorderOffsetModule>> PROVIDERS = Map.of(
            "awt", ArcticRecorderOffsetModule::configureAwt,
            "fixed", ArcticRecorderOffsetModule::configureFixed
    );

    public ArcticRecorderOffsetModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {
        check(InjectionKeys.OFFSET_PROVIDER, PROVIDERS.keySet());
        final String offsetProvider = getConfig().getString(InjectionKeys.OFFSET_PROVIDER);
        if (!PROVIDERS.containsKey(offsetProvider)) {
            fail(InjectionKeys.OFFSET_PROVIDER, PROVIDERS.keySet());
        }
        PROVIDERS.get(offsetProvider).accept(this);


    }

    private void configureFixed() {
        bindFromConfig(Integer.class, InjectionKeys.OFFSET_FIXED_X, "a valid number of pixels");
        bind(ArcticTestWindowOffsetCalculator.class).to(FixedPointOffsetCalculator.class);
    }

    private void configureAwt() {
        bindFromConfig(Integer.class, InjectionKeys.OFFSET_AWT_EXTRA, "a valid number of pixels");
        bindFromConfig(Boolean.class, InjectionKeys.OFFSET_AWT_COLOR_CORRECTION, Arrays.asList(true, false));
        final boolean useColorCorrection = getConfig().getBoolean(InjectionKeys.OFFSET_AWT_COLOR_CORRECTION);

        check(CommonInjectionKeys.WORKBENCH_DEFAULT_COLOR, "any color");
        if (useColorCorrection) {
            final int baseColor = getConfig().getInt(CommonInjectionKeys.WORKBENCH_DEFAULT_COLOR);
            final int correctedColor = performColorCorrectionTest(baseColor);
            log.debug("Binding correctedColor to: {}", Integer.toHexString(correctedColor));
            bind(Integer.class).annotatedWith(named(InjectionKeys.OFFSET_AWT_CORRECTED_COLOR))
                    .toInstance(correctedColor);


        } else {
            bind(Integer.class).annotatedWith(named(InjectionKeys.OFFSET_AWT_CORRECTED_COLOR))
                    .toInstance(getConfig().getInt(CommonInjectionKeys.WORKBENCH_DEFAULT_COLOR));

        }
        bind(ArcticTestWindowOffsetCalculator.class).to(AwtRobotOffsetCalculator.class);
    }

    private int performColorCorrectionTest(final int baseColor) {
        try {
            final Robot r = new Robot();
            final JFrame testFrame = new JFrame("Color correction frame");
            final JPanel backgroundPane = new JPanel();
            backgroundPane.setBackground(new Color(baseColor));
            testFrame.setContentPane(backgroundPane);
            testFrame.setUndecorated(true);
            testFrame.setSize(100, 100);
            testFrame.setLocationRelativeTo(null);
            testFrame.setVisible(true);
            testFrame.toFront();
            Thread.sleep(50);
            int color = r.getPixelColor(testFrame.getX() + 50, testFrame.getY() + 50).getRGB();
            color &= 0xFFFFFF; // remove alpha channel
            testFrame.dispose();
            log.debug("Test concluded: {}", color);
            return color;
        } catch (final InterruptedException | AWTException e) {
            log.error("Unable to calculate the color correction value", e);
            throw new ArcticException("Unable to calculate the color correction value", e);
        }
    }
}
