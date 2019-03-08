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
import org.slf4j.event.Level;

/**
 * A command to control the log for specific loggers from the ArcticCmd.
 */
public final class LogSetCommand extends ArcticCommand {
    public static final String[] COMMAND_LINE = new String[]{"log", "set"};
    private static final String PREFIX = LogController.DEFAULT_LOGGER + ".";


    @Override
    public boolean registerInto(final CommandTreeNode node) {
        return super.registerInto(node);
    }

    @Override
    public String run(final String... args) {
        if (args.length < COMMAND_LINE.length + 1) {
            return "Missing arguments" + System.lineSeparator() + getHelp();
        }
        final Level lvl = LogController.parseLevel(args[COMMAND_LINE.length]);
        if (lvl == null) {
            return "Invalid level: " + args[COMMAND_LINE.length] + System.lineSeparator() + getHelp();
        }
        if (args.length == COMMAND_LINE.length + 1) {
            setLevel(LogController.DEFAULT_LOGGER, lvl);
            return "All loggers set to " + lvl;
        } else {
            String logName = args[COMMAND_LINE.length + 1];
            if (logName.equalsIgnoreCase("root")) {
                setLevel("", lvl);
                return "Root logger set to " + lvl;
            }
            if (logName.equalsIgnoreCase("all")) {
                setAllLevel(lvl);
                return "All loggers set to " + lvl;
            }
            if (logName.startsWith("$")) {
                logName = logName.substring(1);
            } else {
                logName = PREFIX + logName;
            }
            setLevel(logName, lvl);
            return logName + " set to " + lvl;
        }
    }

    private void setLevel(final String name, final Level lvl) {
        if ("".equals(name)) {
            LogController.setRootLevel(lvl);
        } else {
            LogController.setLevel(name, lvl);
        }
    }

    private void setAllLevel(final Level lvl) {
        LogController.setAllLevels(lvl);
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator()
                + "Usage:" + System.lineSeparator()
                + String.format("  %s LEVEL [LOGGER]", String.join(" ", COMMAND_LINE))
                + System.lineSeparator() + System.lineSeparator()
                + "Parameters:" + System.lineSeparator()
                + String.format("  %-20s%s", "LEVEL", "Accepted levels are: "
                    + String.join(" ", LogController.VALID_LEVELS)) + System.lineSeparator()
                + String.format("  %-20s%s", "LOGGER", "Optional, change that logger instead of the arctic base one")
                    + System.lineSeparator() + System.lineSeparator()
                + "Special Loggers:" + System.lineSeparator()
                + String.format("  %-20s%s", "root", "The root logger") + System.lineSeparator()
                + String.format("  %-20s%s", "global", "The global logger") + System.lineSeparator()
                + String.format("  %-20s%s", "all", "change all the existing loggers") + System.lineSeparator()
                + String.format("  %-20s%s", "$<LOGGER>", "Shortcut for " + PREFIX + "<LOGGER>")
                    + System.lineSeparator();
    }

    @Override
    public String getDescription() {
        return "Change Arctic log level";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
