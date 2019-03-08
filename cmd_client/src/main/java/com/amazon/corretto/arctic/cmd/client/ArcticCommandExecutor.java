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

package com.amazon.corretto.arctic.cmd.client;

import javax.inject.Inject;

import com.amazon.corretto.arctic.cmd.client.rmi.impl.ClientRmiInterceptor;

/**
 * Executes a single Arctic command over RMI and exits with the proper code.
 */
public final class ArcticCommandExecutor {
    private final ClientRmiInterceptor clientRmiInterceptor;

    /**
     * Creates a new ArcticCommandExecutor that will send the command to a {@link ClientRmiInterceptor}.
     * @param clientRmiInterceptor Interceptor that will receive and delegate the command appropriately.
     */
    @Inject
    public ArcticCommandExecutor(final ClientRmiInterceptor clientRmiInterceptor) {
        this.clientRmiInterceptor = clientRmiInterceptor;
    }

    /**
     * Execute the command. This requires shifting the main args to the left to remove the -c argument used to indicate.
     * we want to execute a single command, as well as exit with the proper return code
     * @param args Arguments received by the main method
     */
    public void start(final String[] args) {
        String[] shiftedArgs = new String[args.length - 1];
        System.arraycopy(args, 1, shiftedArgs, 0, args.length - 1);
        System.out.println(clientRmiInterceptor.runCommand(shiftedArgs));
        if (clientRmiInterceptor.getLastResult()) {
            System.exit(0);
        } else {
            System.exit(-1);
        }
    }
}
