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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.slf4j.event.Level;

/**
 * A class used to control the logging implementation, in this case, java.util.Logging.
 */
public final class LogController {
    public static final String DEFAULT_LOGGER = "com.amazon.corretto.arctic";
    public static final Set<String> VALID_LEVELS = Set.of("ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF");

    private LogController() {
        // No instantiation.
    }

    /**
     * Reads the configuration for java.util.Logging from a resource file. Should be called on application startup.
     * The dependency injection process does assume the logging is working, so it needs to happen before that.
     */
    public static void init() {
        try (InputStream is = LogController.class.getClassLoader().getResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            System.err.println("Unable to initialize logging");
            e.printStackTrace();
            System.exit(-1);
        }

        // A quick override of the level for the com.amazon.corretto.arctic logger based on a system property
        String logOverride = System.getProperty("arctic.logLevel");
        if (logOverride != null && !logOverride.equals("")) {
            Level lvl = parseLevel(logOverride);
            if (lvl != null) {
                setLevel(DEFAULT_LOGGER, lvl);
            }
        }
    }

    /**
     * Parse a String into a valid Slf4j level. Common aliases like all and off (case insensitive) are accepted.
     * @param lvl A string to parse into an Slf4j level.
     * @return The level that was parsed. This returns null if the String can't be parsed.
     */
    public static Level parseLevel(final String lvl) {
        if (lvl.equalsIgnoreCase("all")) {
            return Level.TRACE;
        }
        if (lvl.equalsIgnoreCase("off")) {
            return Level.ERROR;
        }
        if (VALID_LEVELS.contains(lvl.toUpperCase())) {
            return Level.valueOf(lvl.toUpperCase());
        }
        return null;
    }

    /**
     * Changes the level for a specific logger.
     * @param logName Name of the logger to change
     * @param level Desired level (Slf4j)
     */
    public static void setLevel(final String logName, final Level level) {
        Logger logger = Logger.getLogger(logName);
        logger.setLevel(LevelMappings.SLF4J_JUL.get(level));
    }

    /**
     * Changes the level for the root logger.
     * @param level Desired level (Slf4j)
     */
    public static void setRootLevel(final Level level) {
        Logger root = Logger.getLogger("");
        root.setLevel(LevelMappings.SLF4J_JUL.get(level));
    }

    /**
     * Changes the level for all loggers.
     * @param level Desired level (Slf4j)
     */
    public static void setAllLevels(final Level level) {
        for (String logName : Collections.list(LogManager.getLogManager().getLoggerNames())) {
            Logger.getLogger(logName).setLevel(LevelMappings.SLF4J_JUL.get(level));
        }
    }
}
