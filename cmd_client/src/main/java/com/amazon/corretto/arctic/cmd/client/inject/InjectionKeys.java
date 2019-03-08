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

package com.amazon.corretto.arctic.cmd.client.inject;

/**
 * Injection keys that are used only by Arctic cmd_client.
 */
public final class InjectionKeys {
    private InjectionKeys() { }

    private static final String PREFIX = "arctic.cmd.client.";

    /**
     * Whether to enable rmi debug mode or not. When enabled, the client will print StackTraces for errors.
     */
    public static final String RMI_DEBUG = PREFIX + "rmi.debug";

    /**
     * Hostname to connect for the RMI connection
     */
    public static final String RMI_HOST = PREFIX + "rmi.host";
}
