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

import java.util.stream.Collectors;

import com.amazon.corretto.arctic.common.tweak.ArcticTweakManager;
import com.amazon.corretto.arctic.common.tweak.TweakKeys;
import jakarta.inject.Inject;

/**
 * This command allows to do some tweaks to arctic components without the need to restart the application. Different
 * component can be registered as {@link com.amazon.corretto.arctic.common.tweak.ArcticTweakableComponent} and they
 * will send the different key value combinations the user can set with this command.
 */
public final class TweakCommand extends CompositeCommand {
    public static final String[] COMMAND_LINE = new String[]{"tweak"};
    private final ArcticTweakManager tweakManager;

    /**
     * Creates a new instance of the command. Called by the dependency injection framework.
     * @param tweakManager To query and set different tweaks.
     */
    @Inject
    public TweakCommand(final ArcticTweakManager tweakManager) {
        this.tweakManager = tweakManager;
    }

    @Override
    public String run(final String... args) {
        if (args.length < COMMAND_LINE.length + 1) {
            return "Missing arguments" + System.lineSeparator() + getHelp();
        }
        switch (args[COMMAND_LINE.length]) {
            case "list":
                return runList();
            case "info":
                return runInfo(args);
            case "set":
                return runSet(args);
            default:
                return "Unknown option " + args[COMMAND_LINE.length] + System.lineSeparator() + getHelp();
        }
    }

    private String runList() {
        return "Valid keys: " + System.lineSeparator() + "  " + tweakManager.getTweakKeys().stream()
                .map(it -> it.substring(TweakKeys.PREFIX.length()))
                .collect(Collectors.joining(" "));
    }

    private String runInfo(final String[] args) {
        if (args.length < COMMAND_LINE.length + 2) {
            return "Missing arguments" + System.lineSeparator() + getHelp();
        }
        String key = TweakKeys.PREFIX + args[COMMAND_LINE.length + 1];
        String info = tweakManager.getTweakKeyDescription(key);
        if (info == null) {
            return "Tweak key " + args[COMMAND_LINE.length + 1] + " is not being used";
        }
        return info;
    }

    private String runSet(final String[] args) {
        if (args.length < COMMAND_LINE.length + 3) {
            return "Missing arguments" + System.lineSeparator() + getHelp();
        }
        String key = TweakKeys.PREFIX + args[COMMAND_LINE.length + 1];
        String value = args[COMMAND_LINE.length + 2];
        boolean result = tweakManager.setTweak(key, value);
        if (result) {
            return args[COMMAND_LINE.length + 1] + " has been set to " + value;
        }
        return "Tweak key " + args[COMMAND_LINE.length + 1] + " is not being used";
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getHelp() {
        return "List of options:" + System.lineSeparator()
                + String.format("  %-20s%s", String.join(" ", String.join(" ", COMMAND_LINE), "list"),
                        "list the valid tweak keys")
                + System.lineSeparator()
                + String.format("  %-20s%s", String.join(" ", String.join(" ", COMMAND_LINE), "info", "KEY"),
                        "gets information about a key")
                + System.lineSeparator()
                + String.format("  %-20s%s", String.join(" ", String.join(" ", COMMAND_LINE), "set", "KEY", "VALUE"),
                        "sets the value of a tweak key")
                + System.lineSeparator();
    }

    @Override
    public String getDescription() {
        return "Change arctic runtime tweaks";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
