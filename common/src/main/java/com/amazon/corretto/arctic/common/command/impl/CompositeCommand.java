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

/**
 * A composite command represents a node in the command tree that does nothing by itself, but it contains children that
 * do. This command is still inserted into the tree, so it can capture requests for this node and print the help. For
 * example, if we have a command registered as "tap print" and another as "tap save", we may want to create a composite
 * command for "tap" so executions of "help tap" or just "tap" does not print the general help but a specialized one
 * that lists the children.
 */
public abstract class CompositeCommand extends ArcticCommand {

    /**
     * This command should not be executed directly, so calling run will always print the help.
     * @param args The command line that was introduced by the user. This includes the command registered words
     * @return The help associated with this command.
     */
    @Override
    public String run(final String... args) {
        return getHelp();
    }

    /**
     * Returns the help for this command, which is a combination of the description of the children.
     * @return Help for the command.
     */
    @Override
    public String getHelp() {
        final StringBuilder out = new StringBuilder();
        out.append("List of commands:").append(System.lineSeparator());
        for (final ArcticCommand command : getRegisteredNode()) {
            out.append(String.format("  %-20s%s", String.join(" ", command.getCommandLine()),
                    command.getDescription())).append(System.lineSeparator());
        }
        out.append(System.lineSeparator()).append("Use:").append(System.lineSeparator())
                .append("  help COMMAND").append(System.lineSeparator())
                .append("For more information about COMMAND");
        return out.toString();
    }
}
