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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.Set;
import javax.inject.Inject;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import lombok.extern.slf4j.Slf4j;

/**
 * The BaseCommand represents the command that sits as the root of the command tree, capturing all the inputs for which
 * we have not been able to find a proper command. For that reason, this command also acts as the help command, as the
 * default option in those cases is to just print the help.
 */
@Slf4j
public final class BaseCommand extends CompositeCommand {
    private static final String[] COMMAND_LINE = new String[]{"help"};
    int commandCount = 0;

    /**
     * Constructor to be use by the dependency injector. It gets a set of commands that need to be registered into the
     * commandTree. More commands can be manually added later if needed.
     * @param commands Set of commands to initially register into the command tree.
     */
    @Inject
    public BaseCommand(final Set<ArcticCommand> commands) {
        final CommandTreeNode root = new CommandTreeNode();
        root.registerCommand(this, 0); // Register as help
        root.registerCommand(this, -1); // Register as empty
        commands.stream()
                .sorted(Comparator.comparing(it -> String.join(" ", it.getCommandLine())))
                .forEach(this::registerCommand);
    }

    /**
     * Allows for special commands to be manually registered. This can be commands that can't go through the regular
     * command registration. For example, the BatchCommand, that needs a reference to the base command to operate.
     * This would cause a circularity dependency injection
     * @param command Command to be registered
     */
    public void registerCommand(final ArcticCommand command) {
        getRegisteredNode().registerCommand(command, 0);
    }

    /**
     * Checks if there is a command that will catch a specific command line.
     * @param args Command line to check for a command.
     * @return True if the command exists.
     */
    public boolean hasCommand(final String[] args) {
        final ArcticCommand command = getRegisteredNode().getCommand(args, 0);
        return command != null;
    }

    /**
     * Checks if there is a local command that will catch a specific command line.
     * @param args Command line to check for a command.
     * @return True if the command exists.
     */
    public boolean hasLocalCommand(final String[] args) {
        final ArcticCommand command = getRegisteredNode().getCommand(args, 0);
        return command != null && command.isLocal();
    }

    /**
     * Runs the command corresponding to what the user executed. There are three options. Either we get something we
     * do not match to any command (we print the help), we get a explicit request for help, or we pass the execution
     * to the appropriate subcommand.
     * @param args The command line that was invoked
     * @return A String with the output, we get this from the command that ends up being executed.
     */
    @Override
    public String run(final String... args) {
        log.debug("{} : {}", commandCount, String.join(" ", args));
        try {
            final ArcticCommand command = getRegisteredNode().getCommand(args, 0);
            if (command == null) {
                // We did not find a proper command to execute this. As a catch all, the general help is printed.
                return "Unknown command: " + String.join(" ", args) + System.lineSeparator()
                + getHelp();
            } else if (command == this) {
                // We found ourselves. This means we have been asked for help directly
                return getRegisteredNode().getCommand(args, 1).getHelp();
            } else {
                return command.run(args);
            }
        } catch (final Exception e) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Unknown error. Caused by:");
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            sb.append(sw.toString());
            return sb.toString();
        }
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getDescription() {
        return "Print help about commands";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
