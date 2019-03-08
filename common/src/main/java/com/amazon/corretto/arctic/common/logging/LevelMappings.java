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

package com.amazon.corretto.arctic.common.logging;

import java.util.Map;

import org.slf4j.event.Level;

import static java.util.logging.Level.ALL;
import static java.util.logging.Level.CONFIG;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.OFF;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * A class with mapping for the level names between our logging API (Slf4j) and our current logging implementation
 * (java.util.Logging).
 */
public final class LevelMappings {
    private LevelMappings() {
        // No instantiation required.
    }

    // Map from java.util.Logging to Slf4j
    public static final Map<java.util.logging.Level, Level> JUL_SLF4J = Map.of(
            OFF, Level.ERROR,
            SEVERE, Level.ERROR,
            WARNING, Level.WARN,
            CONFIG, Level.INFO,
            INFO, Level.INFO,
            FINE, Level.DEBUG,
            FINER, Level.DEBUG,
            FINEST, Level.TRACE,
            ALL, Level.TRACE);

    // Map from Slf4j to java.util.Logging
    public static final Map<Level, java.util.logging.Level> SLF4J_JUL = Map.of(
            Level.ERROR, SEVERE,
            Level.WARN, WARNING,
            Level.INFO, INFO,
            Level.DEBUG, FINER,
            Level.TRACE, ALL);
}
