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

import java.util.Scanner;
import javax.inject.Inject;

import com.amazon.corretto.arctic.cmd.client.rmi.impl.ClientRmiInterceptor;

/**
 * An interactive console to execute multiple Arctic commands.
 */
public final class ArcticConsole {
    private final ClientRmiInterceptor clientRmiInterceptor;
    private final Scanner in = new Scanner(System.in);

    /**
     * Creates a new ArcticConsole that will send commands to a {@link ClientRmiInterceptor}.
     * @param clientRmiInterceptor Interceptor that will receive and delegate the command appropriately.
     */
    @Inject
    public ArcticConsole(final ClientRmiInterceptor clientRmiInterceptor) {
        this.clientRmiInterceptor = clientRmiInterceptor;
    }

    /**
     * Initiates an interactive Arctic shell to issue multiple commands using stdin/stdout.
     */
    public void start() {
        System.out.println("Arctic command interface ready");
        //noinspection InfiniteLoopStatement
        do {
            System.out.print("-> ");
            final String[] line = in.nextLine().split(" ");
            System.out.println(clientRmiInterceptor.runCommand(line));
        } while (true);
    }
}
