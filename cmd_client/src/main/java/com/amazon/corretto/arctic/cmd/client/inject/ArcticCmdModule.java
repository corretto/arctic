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

import java.util.List;

import com.amazon.corretto.arctic.api.rmi.ArcticRmiCommandClient;
import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.google.inject.Provides;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.configuration2.Configuration;

/**
 * Main guice module used to configure dependencies of the Arctic cmd_client.
 */
public final class ArcticCmdModule extends ArcticModule {

    /**
     * Creates a new ArcticCmdCommandModule that will read injection keys from the provided configuration.
     * @param config Apache configuration object
     */
    public ArcticCmdModule(final Configuration config) {
        super(config);
    }

    /**
     * Used by guice to initialize the module. This will install {@link ArcticCmdCommandModule} and install cmd_client
     * specific module as long as they match the general allowed commands restrictions.
     */
    public void configure() {
        install(new ArcticCmdCommandModule(getConfig()));
        bindFromConfig(Boolean.class, InjectionKeys.RMI_DEBUG, List.of(true, false));
        bindFromConfig(String.class, InjectionKeys.RMI_HOST, "a valid hostname");
    }

    /**
     * Provider for the ArcticRmiCommandClient.
     * @param rmiName Name to register in RMI
     * @param rmiPort Port for the RMI local registry
     * @param debug Whether we want to print extra debug information about the rmi connection
     * @return An ArcticRmiCommandClient that can be used by Arctic cmd_client
     */
    @Provides
    @Singleton
    public ArcticRmiCommandClient getArcticRmiClient(@Named(CommonInjectionKeys.CMD_RMI_NAME) final String rmiName,
                                                     @Named(CommonInjectionKeys.CMD_RMI_PORT) final int rmiPort,
                                                     @Named(InjectionKeys.RMI_DEBUG) final boolean debug,
                                                     @Named(InjectionKeys.RMI_HOST) final String rmiHost) {
        return new ArcticRmiCommandClient(rmiName, rmiPort, debug, rmiHost);
    }
}
