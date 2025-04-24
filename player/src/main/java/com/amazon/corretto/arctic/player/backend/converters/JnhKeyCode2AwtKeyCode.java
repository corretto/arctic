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

package com.amazon.corretto.arctic.player.backend.converters;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.amazon.corretto.arctic.common.model.event.KeyboardEvent;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * This class converts the rawCodes and keyCodes from jnh into awt codes. It receives the mapping as a list of Strings
 * with the same format that Arctic can generate when running with the -d option.
 */
public final class JnhKeyCode2AwtKeyCode implements Function<KeyboardEvent, Integer> {
    private final Map<Integer, Integer> keyCodeMap = new HashMap<>();
    private final Map<Integer, Integer> rawCodeMap = new HashMap<>();

    /**
     * Constructor to use by the Dependency Injection tool.
     * @param keyMapLines A list of lines with the format AWT_CODE,JNH_KEYCODE,JNH_RAWCODE. Lines starting with # are
     *                    ignored.
     */
    @Inject
    public JnhKeyCode2AwtKeyCode(final @Named(InjectionKeys.BACKEND_PLAYERS_AWT_KB_KEYMAP) List<String> keyMapLines) {
        keyMapLines.stream()
                .filter(it -> !it.startsWith("#"))
                .map(it -> it.split(","))
                .forEach(it -> {
                    int awtCode = Integer.parseInt(it[0]);
                    int jnhCode = Integer.parseInt(it[1]);
                    int rawCode = Integer.parseInt(it[2]);
                    if (jnhCode != -1) {
                        keyCodeMap.put(jnhCode, awtCode);
                    }
                    if (rawCode != -1) {
                        rawCodeMap.put(rawCode, awtCode);
                    }
                });
    }

    /**
     * Extracts a valid awt code from a Keyboard Event. It tries first to match the rawCode and uses the keyCode as a
     * fallback.
     * @param ke Keyboard event we want to convert.
     * @return And integer that is a valid AWT key event.
     */
    @Override
    public Integer apply(final KeyboardEvent ke) {
        return rawCodeMap.getOrDefault(ke.getRawCode(),
                keyCodeMap.getOrDefault(ke.getKeyCode(),
                        KeyEvent.VK_UNDEFINED));
    }
}
