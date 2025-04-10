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

package com.amazon.corretto.arctic.cmd.client.rmi.impl;

import com.amazon.corretto.arctic.api.rmi.ArcticRmiCommandClient;
import com.amazon.corretto.arctic.common.command.impl.BaseCommand;
import jakarta.inject.Inject;

/**
 * Most commands are executed via RMI, contacting the ArcticPlayer or ArcticRecorder running. But some commands can be
 * executed locally. An example of this would be the wait command (we don't want to wait over an RMI connection) or the
 * quit command (to exit the current session). This class will determine which commands need to be execute locally and
 * which commands need to be executed remotely.
 */
public final class ClientRmiInterceptor {

    private final BaseCommand localBaseCommand;
    private final ArcticRmiCommandClient rmiCommandClient;

    /**
     * Creates a new ClientRmiInterceptor that will execute commands remotely or locally.
     * @param localBaseCommand Will execute the local commands.
     * @param rmiCommandClient Will execute the remote commands.
     */
    @Inject
    public ClientRmiInterceptor(final BaseCommand localBaseCommand, final ArcticRmiCommandClient rmiCommandClient) {
        this.localBaseCommand = localBaseCommand;
        this.rmiCommandClient = rmiCommandClient;
    }

    /**
     * Attempts to executed a line as an ArcticCommand.
     * @param command Line to be executed
     * @return Output of the executed command
     */
    public String runCommand(final String command) {
        return runCommand(command.split(" "));
    }

    /**
     * Attempts to execute a String[] as an ArcticCommand.
     * @param args Line to be executed
     * @return Output of the executed command
     */
    public String runCommand(final String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("debug")) {
            return "Debug mode: " + rmiCommandClient.toggleDebug();
        } else {
            if (localBaseCommand.hasLocalCommand(args)) {
                return localBaseCommand.run(args);
            } else {
                return rmiCommandClient.runCommand(args);
            }
        }
    }

    /**
     * Returns whether the last command was executed successfully. This is used to determine the exit code when running
     * a single command.
     * @return True if the command was executed successfully.
     */
    public boolean getLastResult() {
        return rmiCommandClient.getLastResult();
    }
}
