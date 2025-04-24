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

package com.amazon.corretto.arctic.common.command.interpreter.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;

import com.amazon.corretto.arctic.api.exception.ArcticException;
import com.amazon.corretto.arctic.api.rmi.ArcticRmiCommandInterface;
import com.amazon.corretto.arctic.common.command.impl.BaseCommand;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a command interpreter that registers and receives command orders via RMI. This is useful when arctic is
 * already running, but we do not have direct access to the process stdin, for example during scripts.
 */
@Singleton
public final class ArcticRmiInterpreter implements ArcticRmiCommandInterface {
    public static final String LOCAL_ONLY_VALUE = "local_only";
    private static final Logger log = LoggerFactory.getLogger(ArcticRmiInterpreter.class);
    private static ArcticRmiCommandInterface rmiCommandInterface;

    private final BaseCommand baseCommand;
    private final boolean enabled;
    private final String rmiName;
    private final int port;
    private final boolean localOnly;
    private boolean started = false;

    /**
     * Creates a new instance of the interpreter. Called by the dependency injection framework.
     * @param baseCommand The command to send the line we received via rmi.
     * @param enabled Whether rmi support is enabled in arctic. If false, do not registry via rmi.
     * @param rmiName The name of the binding in the rmi local registry.
     * @param port The port on which to set up the local registry.
     */
    @Inject
    public ArcticRmiInterpreter(final BaseCommand baseCommand,
                                @Named(CommonInjectionKeys.CMD_RMI_ENABLED) final boolean enabled,
                                @Named(CommonInjectionKeys.CMD_RMI_NAME) final String rmiName,
                                @Named(CommonInjectionKeys.CMD_RMI_PORT) final int port,
                                @Named(CommonInjectionKeys.CMD_RMI_SECURITY_LOCAL_ONLY) final boolean localOnly) {
        this.baseCommand = baseCommand;
        this.enabled = enabled;
        this.rmiName = rmiName;
        this.port = port;
        this.localOnly = localOnly;
    }

    /**
     * Start this interpreter, so it can receive commands via rmi.
     */
    public void start() {
        if (enabled && !started) {
            try {
                // Avoid java.rmi.NoSuchObjectException: no such object in table by preventing "this" from being gc'ed
                // Example: https://bugs.openjdk.java.net/browse/JDK-8203026
                rmiCommandInterface = this;
                final var stub = (ArcticRmiCommandInterface) UnicastRemoteObject.exportObject(this, 0);
                final RMIServerSocketFactory serverSocketFactory = localOnly ? RMISocketFactory.getDefaultSocketFactory() : new LocalhostRMIRegistry();
                final Registry registry = LocateRegistry.createRegistry(port, RMISocketFactory.getDefaultSocketFactory(), serverSocketFactory);
                registry.bind(rmiName, stub);
                log.info("{} server is online on port {}", rmiName, port);
                started = true;
            } catch (final RemoteException e) {
                log.error("Unable to start RMI server {} on {}", rmiName, port, e);
                throw new ArcticException("Unable to start RMI server", e);
            } catch (final AlreadyBoundException e) {
                log.error("Unable to start RMI server as {} is already bounded in {}", rmiName, port, e);
                throw new ArcticException("RMI Server is already bound", e);
            }
        }
    }

    @Override
    public String runCommand(final String[] command) {
        if (enabled) {
            return baseCommand.run(command);
        } else {
            return CommonInjectionKeys.CMD_RMI_ENABLED + " is not true";
        }
    }

    private static class LocalhostRMIRegistry implements RMIServerSocketFactory {
        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            return new ServerSocket(port, 0, InetAddress.getLoopbackAddress());
        }
    }
}
