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

package com.amazon.corretto.arctic.cmd.client.command.impl;

import javax.inject.Inject;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.command.impl.BaseCommand;

/**
 * Forces a command to be executed locally. This is done by executing "local COMMAND".
 * todo: Inject with command without causing an injection loop with {@link BaseCommand}
 */
public final class LocalCommand extends ArcticCommand {
    public static final String[] COMMAND_LINE = new String[]{"local"};
    private final BaseCommand baseCommand;

    /**
     * Creates a new LocalCommand.
     * @param baseCommand The baseCommand that will execute the chained commands
     */
    @Inject
    public LocalCommand(final BaseCommand baseCommand) {
        this.baseCommand = baseCommand;
    }

    /**
     * Forces a command to be executed locally, instead of remotely. An example would be to run "local help" to attempt
     * to display the help of the commands loaded in cmd_client, instead of printing the remote help
     * @param args Arctic command that was executed.
     * @return The result of the execution of the local command.
     */
    @Override
    public String run(final String... args) {
        String[] shiftedArgs = new String[args.length - 1];
        System.arraycopy(args, 1, shiftedArgs, 0, args.length - 1);
        return baseCommand.run(shiftedArgs);
    }

    /**
     * Command line elements that will be captured by this command.
     * @return The String[] that will be captured by this command
     */
    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    /**
     * Basic help on how to use this command.
     * @return Formatted help for the command
     */
    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator()
                + "Usage:" + System.lineSeparator()
                + "  local <COMMAND>";
    }

    /**
     * Basic description for this command.
     * @return the description.
     */
    @Override
    public String getDescription() {
        return "Forces to execute a command locally";
    }

    /**
     * Whether this command should be executed locally when possible.
     * @return Always true
     */
    @Override
    public boolean isLocal() {
        return true;
    }
}
