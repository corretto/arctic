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

import java.nio.file.Files;
import java.nio.file.Path;

import com.amazon.corretto.arctic.common.command.ArcticCommand;

public final class BatchCommand extends ArcticCommand {
    private static final String[] COMMAND_LINE = new String[]{"batch run"};

    private final BaseCommand baseCommand;

    public BatchCommand(final BaseCommand baseCommand) {
        this.baseCommand = baseCommand;
        baseCommand.registerCommand(this);
    }

    @Override
    public String run(final String... args) {
        if (args.length < 3) {
            return "Insufficient number of parameters" + System.lineSeparator() + getHelp();
        } else if (Files.exists(Path.of(args[args.length - 1]))) {
            return "File " + args[args.length - 1] + " does not exist" + System.lineSeparator() + getHelp();
        } else if (Files.isDirectory(Path.of(args[args.length - 1]))) {
            return args[args.length - 1] + " is a directory" + System.lineSeparator() + getHelp();
        } else if (Files.isReadable(Path.of(args[args.length - 1]))) {
            return "File " + args[args.length - 1] + " cannot be read" + System.lineSeparator() + getHelp();
        } else {
            return "Not Implemented yet";
        }
    }

    // TODO: Implement this as async command
    public String batchRun(final String... args) {
        return "Not implemented yet";
    }

    @Override
    public String[] getCommandLine() {
        return new String[0];
    }

    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator()
                + "Usage:" + System.lineSeparator()
                + String.format("  %s AMOUNT", String.join(" ", COMMAND_LINE))
                + System.lineSeparator() + System.lineSeparator()
                + "Parameters:" + System.lineSeparator()
                + String.format("  %-20s%s", "AMOUNT", "Time to wait in ms");    }

    @Override
    public String getDescription() {
        return "Run multiple commands from a file";
    }

    @Override
    public boolean isLocal() {
        return true;
    }
}
