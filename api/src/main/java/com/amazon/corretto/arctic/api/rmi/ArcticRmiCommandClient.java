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
package com.amazon.corretto.arctic.api.rmi;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;

public final class ArcticRmiCommandClient implements ArcticRmiCommandInterface {
    public static final String DEFAULT_RMI_HOST = "localhost";
    public static final String DEFAULT_RMI_NAME = "ArcticCmd";
    public static final int DEFAULT_RMI_PORT = 61099;
    public static final int MIN_RMI_PORT = 1024;
    public static final int MAX_RMI_PORT = 65535;
    private static final String HOST_PROPERTY = "ArcticCommandClient.RegistryHost";
    private static final String NAME_PROPERTY = "ArcticCommandClient.RegistryName";
    private static final String PORT_PROPERTY = "ArcticCommandClient.RegistryPort";
    private static final String DEBUG_PROPERTY = "ArcticCommandClient.Debug";
    private final String rmiHost;
    private final String rmiName;
    private final int rmiPort;
    private ArcticRmiCommandInterface rmiClient;
    private boolean debug = false;
    private boolean lastResult = true;

    public ArcticRmiCommandClient() {
        this.rmiName = getName();
        this.rmiPort = getPort();
        this.debug = getDebug();
        this.rmiHost = getHost();
    }

    public ArcticRmiCommandClient(String parameter) {
        this.rmiName = getName(parameter);
        this.rmiPort = getPort(parameter);
        this.debug = getDebug(parameter);
        this.rmiHost = getHost();
        if (debug) {
            System.err.println("Single String:" + parameter);
        }
    }


    public ArcticRmiCommandClient(String... parameters) {
        this.rmiName = getName(parameters);
        this.rmiPort = getPort(parameters);
        this.debug = getDebug(parameters);
        this.rmiHost = getHost();
        if (debug) {
            System.out.println("Parameter array");
            Arrays.stream(parameters).forEach(System.err::println);
        }
    }


    public ArcticRmiCommandClient(final String rmiName, final int rmiPort, final boolean debug, final String rmiHost) {
        this(rmiName, String.valueOf(rmiPort), String.valueOf(debug), rmiHost);
    }

    private String getHost(String... parameters) {
        String host = DEFAULT_RMI_HOST;
        if (parameters.length > 3 && !parameters[3].isEmpty()) {
            host =  parameters[3];
        }
        if (System.getProperties().containsKey(HOST_PROPERTY)) {
            host = System.getProperty(HOST_PROPERTY);
        }
        return host;
    }

    private String getName(String... parameters) {
        String name = DEFAULT_RMI_NAME;
        if (parameters.length > 0 && parameters[0] != null && !parameters[0].isEmpty()) {
            name =  parameters[0];
        }
        if (System.getProperties().containsKey(NAME_PROPERTY)) {
            name = System.getProperty(NAME_PROPERTY);
        }
        if (name == null || name.isEmpty()) {
            System.out.println("Invalid RMI Name");
            System.out.println("Using default name: " + DEFAULT_RMI_NAME);
            name = DEFAULT_RMI_NAME;
        }
        return name;
    }

    private int getPort(String... parameters) {
        int port;
        String portString = String.valueOf(DEFAULT_RMI_PORT);
        if (parameters.length > 1) {
            portString = parameters[1];
        }
        if (System.getProperties().containsKey(PORT_PROPERTY)) {
            portString = System.getProperty(PORT_PROPERTY, String.valueOf(DEFAULT_RMI_PORT));
        }
        try {
            port = Integer.parseInt(portString);
            if (port < MIN_RMI_PORT || port > MAX_RMI_PORT) {
                throw new NumberFormatException("Invalid port");
            }
        } catch (final NumberFormatException e) {
            System.out.println(portString + " is not a valid port between " + MIN_RMI_PORT + " and " + MAX_RMI_PORT);
            System.out.println("Using default port: " + DEFAULT_RMI_PORT);
            return DEFAULT_RMI_PORT;
        }
        return port;
    }

    private boolean getDebug(String... parameters) {
        String debugString = "false";
        if (parameters.length > 2) {
            debugString = parameters[2];
        }
        if (System.getProperties().containsKey(DEBUG_PROPERTY)) {
            debugString = System.getProperty(DEBUG_PROPERTY);
        }
        boolean debug = Boolean.parseBoolean(debugString);
        if (debug) {
            System.out.println("Debug enabled");
        }
        return debug;
    }

    public boolean toggleDebug() {
        debug = !debug;
        return debug;
    }

    private void initialize() {
        try {
            if (debug) {
                System.err.println("RmiHost:" + rmiHost);
                System.err.println("RmiName:" + rmiName);
                System.err.println("RmiPort:" + rmiPort);
                System.err.println("Debug:" + debug);
            }
            final Registry registry = LocateRegistry.getRegistry(rmiHost, rmiPort);
            rmiClient = (ArcticRmiCommandInterface) registry.lookup(rmiName);
            if (debug) {
                System.out.println("Connected to " + rmiName + " on port " + rmiPort);
            }
        } catch (final NotBoundException e) {
            System.err.printf("%s:%d does not seem to be bounded. Make sure the observer is registered%n", rmiName,
                    rmiPort);
            lastResult = false;
            if (debug) {
                e.printStackTrace(System.err);
            }
        } catch (final RemoteException e) {
            System.err.printf("Unable to initiate RMI connection to %s:%d%n", rmiName, rmiPort);
            lastResult = false;
            if (debug) {
                e.printStackTrace(System.err);
            }
        }
    }

    @Override
    public String runCommand(final String... command) {
        if (rmiClient == null) {
            initialize();
        }
        if (rmiClient != null) {
            try {
                lastResult = true;
                return rmiClient.runCommand(command);
            } catch (final RemoteException e) {
                lastResult = false;
                rmiClient = null;
                e.printStackTrace();
            }
        }
        return "Error when communicating to rmi server to send command";
    }

    public boolean getLastResult() {
        return lastResult;
    }
}
