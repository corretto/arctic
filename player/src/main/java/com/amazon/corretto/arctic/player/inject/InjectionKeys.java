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
package com.amazon.corretto.arctic.player.inject;

/**
 * List of keys used to inject different values. Each one maps to an entry in the properties file.
 */
public final class InjectionKeys {



    private InjectionKeys() { }

    private static final String PREFIX = "arctic.player.";

    public static final String CONFIRMATION_MODE = PREFIX + "confirmation.mode";
    public static final String FAST_MODE = PREFIX + "fast.mode";

    /**
     * Backend related keys.
     */
    public static final String BACKEND_PLAYERS = PREFIX + "backend.players";
    public static final String BACKEND_PLAYERS_AWT_MOUSE_BUTTON1 = PREFIX + "backend.awtMouse.button1";
    public static final String BACKEND_PLAYERS_AWT_MOUSE_BUTTON2 = PREFIX + "backend.awtMouse.button2";
    public static final String BACKEND_PLAYERS_AWT_MOUSE_BUTTON3 = PREFIX + "backend.awtMouse.button3";
    public static final String BACKEND_PLAYERS_AWT_MOUSE_EVENTS = PREFIX + "backend.awtMouse.events";
    public static final String BACKEND_PLAYERS_JNH_MOUSE_EVENTS = PREFIX + "backend.jnhMouse.events";
    public static final String BACKEND_PLAYERS_AWT_KB_KEYMAP_BUNDLED = PREFIX + "backend.awtKeyboard.keymap.bundled";
    public static final String BACKEND_PLAYERS_AWT_KB_KEYMAP = PREFIX + "backend.awtKeyboard.keymap";

    /**
     * Pixel sc comparator related keys.
     */
    public static final String BACKEND_SC_COMPARATOR = PREFIX + "backend.sc.comparator";
    public static final String BACKEND_SC_PIXEL_CHECKS = PREFIX + "backend.sc.pixel.checks";
    public static final String BACKEND_SC_PIXEL_SAVE = PREFIX + "backend.sc.pixel.save";
    public static final String BACKEND_SC_PIXEL_SAVE_FOLDER = PREFIX + "backend.sc.pixel.save.folder";
    public static final String BACKEND_SC_PIXEL_SAVE_CLEAR = PREFIX + "backend.sc.pixel.save.clear";
    public static final String BACKEND_SC_PIXEL_SAVE_FORMAT = PREFIX + "backend.sc.pixel.save.format";
    public static final String BACKEND_SC_PIXEL_SAVE_EXTENSION = PREFIX + "backend.sc.pixel.save.extension";
    public static final String BACKEND_SC_PIXEL_HINT_FAST = PREFIX + "backend.sc.pixel.hint.fast";
    public static final String BACKEND_SC_PIXEL_HINT_MASK = PREFIX + "backend.sc.pixel.hint.mask";
    public static final String BACKEND_SC_PIXEL_CONFIDENCE_MIN = PREFIX + "backend.sc.pixel.confidence.min";
    public static final String BACKEND_SC_PIXEL_CHECK_SHADES = PREFIX + "backend.sc.pixel.checkShades";
    public static final String BACKEND_SC_PIXEL_SHADE_MARGIN = PREFIX + "backend.sc.pixel.shadeMargin";
    public static final String BACKEND_SC_PIXEL_FUZZY_TOLERANCE = PREFIX + "backend.sc.pixel.fuzzy.tolerance";
    public static final String BACKEND_SC_PIXEL_CLUSTER_9 = PREFIX + "backend.sc.pixel.cluster.9";
    public static final String BACKEND_SC_PIXEL_CLUSTER_25 = PREFIX + "backend.sc.pixel.cluster.25";
    public static final String BACKEND_SC_PIXEL_CLUSTER_MAX_DRAW = PREFIX + "backend.sc.pixel.cluster.max.draw";
    public static final String BACKEND_SC_PIXEL_CLUSTER_SOURCE_FUZZY = PREFIX + "backend.sc.pixel.cluster.source.fuzzy";

    public static final String TIME_CONTROLLER = PREFIX + "time.controller";

    /**
     * Pre-processing related keys.
     */
    public static final String PRE_PROCESSORS = PREFIX + "pre.processors";
    public static final String PRE_FIRST_TEST_DELAY_WAIT = PREFIX + "pre.firstTestDelay.wait";
    public static final String PRE_SC_VALIDATOR_WAIT_FOCUS = PREFIX + "pre.scValidator.wait.focus";
    public static final String PRE_SC_VALIDATOR_BYPASS = PREFIX + "pre.scValidator.bypass";
    public static final String PRE_OVERRIDES_REPRODUCTION = PREFIX + "pre.overrides.reproduction";
    public static final String PRE_OVERRIDES_REPRODUCTION_MODE = PREFIX + "pre.overrides.reproduction.mode";
    public static final String PRE_OVERRIDES_TRUNCATIONS = PREFIX + "pre.overrides.truncations";
    public static final String PRE_OVERRIDES_TRUNCATIONS_MOUSE_END = PREFIX + "pre.overrides.truncations.mouse.start";
    public static final String PRE_OVERRIDES_TRUNCATIONS_MOUSE_START = PREFIX + "pre.overrides.truncations.mouse.end";
    public static final String PRE_OVERRIDES_TRUNCATIONS_KB_START = PREFIX + "pre.overrides.truncations.kb.start";
    public static final String PRE_OVERRIDES_TRUNCATIONS_KB_END = PREFIX + "pre.overrides.truncations.kb.end";
    public static final String PRE_OVERRIDES_TIMINGS = PREFIX + "pre.overrides.timings";
    public static final String PRE_OVERRIDES_TIMINGS_START = PREFIX + "pre.overrides.timings.start";
    public static final String PRE_OVERRIDES_TIMINGS_SC = PREFIX + "pre.overrides.timings.sc";
    public static final String PRE_OVERRIDES_TIMINGS_MIN = PREFIX + "pre.overrides.timings.min";
    public static final String PRE_OVERRIDES_TIMINGS_MAX = PREFIX + "pre.overrides.timings.max";
    public static final String PRE_OVERRIDES_MOUSE_OFFSETS = PREFIX + "pre.overrides.mouse.offsets";
    public static final String PRE_OVERRIDES_MOUSE_OFFSETS_X = PREFIX + "pre.overrides.mouse.offsets.x";
    public static final String PRE_OVERRIDES_MOUSE_OFFSETS_Y = PREFIX + "pre.overrides.mouse.offsets.y";

    /**
     * Post-processing related keys.
     */
    public static final String POST_PROCESSORS = PREFIX + "post.processors";
    public static final String POST_AUTO_UPDATER_SAVE = PREFIX + "post.auto.updater.save";
    public static final String POST_MIGRATE = PREFIX + "post.migrate";

    /**
     * Review related keys
     */
    public static final String GUI_REVIEW_ORDER = PREFIX + "gui.review.order";
}
