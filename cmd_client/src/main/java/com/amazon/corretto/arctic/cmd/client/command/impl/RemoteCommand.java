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

import com.amazon.corretto.arctic.api.rmi.ArcticRmiCommandClient;
import com.amazon.corretto.arctic.common.command.ArcticCommand;
import jakarta.inject.Inject;

/**
 * Forces a command to be executed remotely, even if it would be possible to execute that command locally.
 */
public final class RemoteCommand extends ArcticCommand {
    public static final String[] COMMAND_LINE = new String[]{"remote"};
    private final ArcticRmiCommandClient rmiCommandClient;

    /**
     * Creates a new RemoteCommand.
     * @param rmiCommandClient Will execute the chained commands.
     */
    @Inject
    public RemoteCommand(final ArcticRmiCommandClient rmiCommandClient) {
        this.rmiCommandClient = rmiCommandClient;
    }

    /**
     * Forces a command to be executed remotely, even if that command has an equivalent local version. This allows for
     * someone like "remote quit" to finish the remote session (although the command terminate will also do that)
     * @param args CommandLine arguments.
     * @return The output of the executed command.
     */
    @Override
    public String run(final String... args) {
        String[] shiftedArgs = new String[args.length - 1];
        System.arraycopy(args, 1, shiftedArgs, 0, args.length - 1);
        return rmiCommandClient.runCommand(shiftedArgs);
    }

    /**
     * Which command line elements this command will intercept.
     * @return The command line elements this command will intercept.
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
                + "  " + String.join(" ", COMMAND_LINE) + " <COMMAND>"
                + "Parameters:"
                + String.format("  %-20s%s", "COMMAND", "Any valid Arctic command plus arguments");
    }

    /**
     * Basic description for this command.
     * @return the description.
     */
    @Override
    public String getDescription() {
        return "Forces to execute a command remotely";
    }

    /**
     * Whether this command should be executed locally when possible. Always true, as this command needs to be executed
     * locally. It is the chained command the one that will be forced to be executed remotely.
     * @return Always true
     */
    @Override
    public boolean isLocal() {
        return true;
    }
}
