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

/**
 * Injection keys that can be used by both, ArcticRecorder and ArcticPlayer.
 */
public final class CommonInjectionKeys {

    private CommonInjectionKeys() { }

    private static final String PREFIX = "arctic.common.";

    /**
     * Represents the test repository folder where the tests will be saved to or loaded from.
     */
    public static final String REPOSITORY_JSON_PATH = PREFIX + "repository.json.path";

    /**
     * Name where the test is saved.
     */
    public static final String REPOSITORY_JSON_NAME = PREFIX + "repository.json.file";

    public static final String SCOPE_MODE = PREFIX + "repository.scope.mode";
    public static final String REPOSITORY_SCOPE = PREFIX + "repository.scope";
    public static final String SCOPES = PREFIX + "repository.load.scopes";
    public static final String SCOPE_CUSTOM = PREFIX + "repository.scope.custom.";

    public static final String REPOSITORY_WINDOWS_LEGACY_MODE = PREFIX + "repository.windows.legacy.mode";

    public static final String CMD_ENABLED = PREFIX + "cmd.enabled";
    public static final String CMD_CONSOLE_ENABLED = PREFIX + "cmd.console.enabled";
    public static final String CMD_ALLOWED_ENABLED = PREFIX + "cmd.allowed.enabled";
    public static final String CMD_ALLOWED = PREFIX + "cmd.allowed";
    public static final String CMD_DISALLOWED_ENABLED = PREFIX + "cmd.disallowed.enabled";
    public static final String CMD_DISALLOWED = PREFIX + "cmd.disallowed";
    public static final String CMD_REGEX_ENABLED = PREFIX + "cmd.regex.enabled";
    public static final String CMD_REGEX = PREFIX + "cmd.regex";

    public static final String CMD_RMI_ENABLED = PREFIX + "cmd.rmi.enabled";
    public static final String CMD_RMI_NAME = PREFIX + "cmd.rmi.name";
    public static final String CMD_RMI_PORT = PREFIX + "cmd.rmi.port";
    public static final String CMD_RMI_SECURITY_LOCAL_ONLY = PREFIX + "cmd.rmi.security.local_only";

    public static final String SESSION_DEFAULT = PREFIX + "session.default";

    public static final String SCREEN_CAPTURE_MARGIN_X = PREFIX + "screen.capture.margin.x";
    public static final String SCREEN_CAPTURE_MARGIN_Y = PREFIX + "screen.capture.margin.y";

    /**
     * Which provider we use to give focus to the test.
     */
    public static final String BACKEND_FOCUS_MANAGER = "arctic.common.backend.focus.manager";

    /**
     * Represents the title of the workbench when it is opened for the first time.
     */
    public static final String WORKBENCH_DEFAULT_TITLE = "arctic.common.gui.wb.default.title";

    /**
     * Represents the color of the workbench when it is opened for the first time.
     */
    public static final String WORKBENCH_DEFAULT_COLOR = "arctic.common.arctic.gui.wb.default.color";

    /**
     * Represents the width of the workbench when it is opened for the first time.
     */
    public static final String WORKBENCH_DEFAULT_WIDTH = "arctic.common.arctic.gui.wb.default.width";

    /**
     * Represents the height of the workbench when it is opened for the first time.
     */
    public static final String WORKBENCH_DEFAULT_HEIGHT = "arctic.common.arctic.gui.wb.default.height";

    /**
     * Represents the title of the shades when they are opened for the first time.
     */
    public static final String SHADE_DEFAULT_TITLE = "arctic.common.gui.shade.default.title";

    /**
     * Represents the color of the shades when they are opened for the first time.
     */
    public static final String SHADE_DEFAULT_COLOR = "arctic.common.gui.shade.default.color";

    /**
     * Represents the width of the shades when they are opened for the first time.
     */
    public static final String SHADE_DEFAULT_WIDTH = "arctic.common.gui.shade.default.width";

    /**
     * Represents the height of the shades when they are opened for the first time.
     */
    public static final String SHADE_DEFAULT_HEIGHT = "arctic.common.gui.shade.default.height";


}
