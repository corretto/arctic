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
package com.amazon.corretto.arctic.common.inject;

import java.util.Map;

import com.amazon.corretto.arctic.common.backend.ArcticTestWindowFocusManager;
import com.amazon.corretto.arctic.common.backend.impl.AwtRobotWindowFocusManager;
import com.amazon.corretto.arctic.common.backend.impl.DummyWindowFocusManager;
import org.apache.commons.configuration2.Configuration;

/**
 * This Guice module injects relevant keys for the common components. These are the default values for Workbench and
 * Shades.
 */
public final class ArcticCommonModule extends ArcticModule {
    private static final Map<String, Class<? extends ArcticTestWindowFocusManager>> FOCUS_MANAGERS = Map.of(
            AwtRobotWindowFocusManager.NAME, AwtRobotWindowFocusManager.class,
            DummyWindowFocusManager.NAME, DummyWindowFocusManager.class);

    /**
     * Creates a new instance of ArcticCommonModule.
     * @param config An apache configuration2 object used to read different keys from.
     */
    public ArcticCommonModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {
        configureFocusManager();
        configureScreenCapture();
        configureWorkbench();
        configureShades();
    }

    private void configureFocusManager() {
        check(CommonInjectionKeys.BACKEND_FOCUS_MANAGER, FOCUS_MANAGERS.keySet());
        final String focusManager = getConfig().getString(CommonInjectionKeys.BACKEND_FOCUS_MANAGER);
        bind(ArcticTestWindowFocusManager.class).to(FOCUS_MANAGERS.get(focusManager));
    }

    private void configureScreenCapture() {
        bindFromConfig(Integer.class, CommonInjectionKeys.SCREEN_CAPTURE_MARGIN_X, "any positive number");
        bindFromConfig(Integer.class, CommonInjectionKeys.SCREEN_CAPTURE_MARGIN_Y, "any positive number");
    }

    private void configureWorkbench() {
        bindFromConfig(String.class, CommonInjectionKeys.WORKBENCH_DEFAULT_TITLE, "any String");
        bindFromConfig(Integer.class, CommonInjectionKeys.WORKBENCH_DEFAULT_WIDTH, "any width in pixels");
        bindFromConfig(Integer.class, CommonInjectionKeys.WORKBENCH_DEFAULT_HEIGHT, "any height in pixels");
        bindFromConfig(Integer.class, CommonInjectionKeys.WORKBENCH_DEFAULT_COLOR, "any color in RGB");
    }

    private void configureShades() {
        bindFromConfig(String.class, CommonInjectionKeys.SHADE_DEFAULT_TITLE, "any String");
        bindFromConfig(Integer.class, CommonInjectionKeys.SHADE_DEFAULT_WIDTH, "any width in pixels");
        bindFromConfig(Integer.class, CommonInjectionKeys.SHADE_DEFAULT_HEIGHT, "any height in pixels");
        bindFromConfig(Integer.class, CommonInjectionKeys.SHADE_DEFAULT_COLOR, "any color in RGB");
    }
}
