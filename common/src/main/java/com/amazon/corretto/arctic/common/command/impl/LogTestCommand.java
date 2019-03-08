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

package com.amazon.corretto.arctic.common.command.impl;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.logging.LogController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command to test a specific logger. This can be used to check the current level set for that logger.
 */
public final class LogTestCommand extends ArcticCommand {
    private static final Logger log = LoggerFactory.getLogger(LogTestCommand.class);
    private static final String PREFIX = LogController.DEFAULT_LOGGER + ".";

    public static final String[] COMMAND_LINE = new String[]{"log", "test"};

    @Override
    public String run(final String... args) {
        final Logger toTest;
        if (args.length <= COMMAND_LINE.length) {
            toTest = log;
        } else if (args[COMMAND_LINE.length].equals("root")) {
            toTest = LoggerFactory.getLogger("");
        } else if (args[COMMAND_LINE.length].startsWith("$")) {
            toTest = LoggerFactory.getLogger(PREFIX + args[COMMAND_LINE.length].substring(1));
        } else {
            toTest = LoggerFactory.getLogger(args[COMMAND_LINE.length]);
        }
        toTest.error("ERROR");
        toTest.warn("WARN");
        toTest.info("INFO");
        toTest.debug("DEBUG");
        toTest.trace("TRACE");

        return "";
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator()
                + "Usage:" + System.lineSeparator()
                + String.format("  %s [LOGGER]", String.join(" ", COMMAND_LINE))
                + System.lineSeparator() + System.lineSeparator()
                + "Parameters:" + System.lineSeparator()
                + String.format("  %-20s%s", "LOGGER", "Optional, logger to test instead of default for command")
                + System.lineSeparator() + System.lineSeparator()
                + "Special Loggers:" + System.lineSeparator()
                + String.format("  %-20s%s", "root", "The root logger") + System.lineSeparator()
                + String.format("  %-20s%s", "global", "The global logger") + System.lineSeparator()
                + String.format("  %-20s%s", "$<LOGGER>", "Shortcut for " + PREFIX + "<LOGGER>")
                + System.lineSeparator();
    }

    @Override
    public String getDescription() {
        return "Tests a specific logger";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
