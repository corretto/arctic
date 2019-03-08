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

import com.amazon.corretto.arctic.recorder.postprocessing.impl.ScreenCheckHashPostProcessor;
import com.amazon.corretto.arctic.recorder.postprocessing.impl.ScreenCheckSavePostProcessor;
import com.amazon.corretto.arctic.recorder.postprocessing.impl.TestSavePostProcessor;
import com.amazon.corretto.arctic.recorder.preprocessing.impl.FirstScCheckPreProcessor;
import com.amazon.corretto.arctic.recorder.preprocessing.impl.InitialPreProcessor;

public class InjectionKeys {
    private static final String PREFIX = "arctic.recorder.";

    public static final String BACKEND_RECORDERS = PREFIX + "backend.recorders";
    public static final String BACKEND_RECORDING_MODE = PREFIX + "backend.recordingMode";
    public static final String OFFSET_PROVIDER = PREFIX + "offset.provider";
    public static final String OFFSET_FIXED_X = PREFIX + "offset.fixed.x";
    public static final String OFFSET_AWT_EXTRA = PREFIX + "offset.awt.extra";
    public static final String OFFSET_AWT_COLOR_CORRECTION = PREFIX + "offset.awt.colorCorrection";
    public static final String OFFSET_AWT_CORRECTED_COLOR = PREFIX + "offset.awt.correctedColor";
    public static final String CONTROL_PROVIDER = PREFIX + "control.provider";
    public static final String CONTROL_AUTO_STOP = PREFIX + "control.autoStopRecording";
    public static final String CONTROL_JNH_START_KEYCODE = PREFIX + "control.jnh.startKeyCode";
    public static final String CONTROL_JNH_STOP_KEYCODE = PREFIX + "control.jnh.stopKeyCode";
    public static final String CONTROL_JNH_SCREEN_CHECK_KEYCODE = PREFIX + "control.jnh.screenCheckKeyCode";
    public static final String CONTROL_JNH_SPAWN_SHADE_KEYCODE = PREFIX + "control.jnh.spawnShadeKeyCode";
    public static final String CONTROL_JNH_DISCARD_KEYCODE = PREFIX + "control.jnh.discardKeyCode";
    public static final String CONTROL_JNH_MODIFIERS = PREFIX + "control.jnh.modifiers";
    public static final String POST_ENABLED = PREFIX + "post.enabled";
    public static final String POST_SC_SAVE_FORMAT = PREFIX + "post." + ScreenCheckSavePostProcessor.NAME + ".format";
    public static final String POST_SC_SAVE_EXTENSION = PREFIX + "post." + ScreenCheckSavePostProcessor.NAME
            + ".extension";
    public static final String POST_SC_HASH_ALGORITHM = PREFIX + "post." + ScreenCheckHashPostProcessor.NAME
            + ".algorithm";
    public static final String POST_SAVE_TEST_ZIP = PREFIX + "post." + TestSavePostProcessor.NAME + ".zip";
    public static final String POST_SAVE_EVENTS_FILENAME = PREFIX + "post." + TestSavePostProcessor.NAME +
            ".eventsFile";
    public static final String PRE_ENABLED = PREFIX + "pre.enabled";
    public static final String PRE_FIRST_SC_MATCH = PREFIX + "pre." + FirstScCheckPreProcessor.NAME + ".match";
    public static final String PRE_FIRST_SC_DELAY = PREFIX + "pre." + FirstScCheckPreProcessor.NAME + ".delay";
    public static final String PRE_INIT_PREFERRED_PLAY_MODE = PREFIX + "pre." + InitialPreProcessor.NAME +
            ".preferredPlayMode";
    public static final String PRE_INIT_TRUNCATE_KB_START = PREFIX + "pre." + InitialPreProcessor.NAME +
            ".truncate.kb.start";
    public static final String PRE_INIT_TRUNCATE_KB_END = PREFIX + "pre." + InitialPreProcessor.NAME +
            ".truncate.kb.end";
    public static final String PRE_INIT_TRUNCATE_MOUSE_START = PREFIX + "pre." + InitialPreProcessor.NAME +
            ".truncate.mouse.start";
    public static final String PRE_INIT_TRUNCATE_MOUSE_END = PREFIX + "pre." + InitialPreProcessor.NAME +
            ".truncate.mouse.end";
}
