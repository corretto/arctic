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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.amazon.corretto.arctic.cmd.client.command.impl.RemoteCommand;
import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.inject.ArcticCommandModule;
import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.google.inject.multibindings.Multibinder;
import org.apache.commons.configuration2.Configuration;

import static java.util.Map.entry;

/**
 * Arctic Guice module to setup dependency injection for command line options that are exclusive to the Arctic
 * cmd_client. It will install an {@link ArcticCommandModule}.
 */
public final class ArcticCmdCommandModule extends ArcticModule {
    private static final Map<String[], Class<? extends ArcticCommand>> COMMANDS = Map.ofEntries(
            //entry(LocalCommand.COMMAND_LINE, LocalCommand.class),
            entry(RemoteCommand.COMMAND_LINE, RemoteCommand.class));

    private static final Map<Class<? extends ArcticCommand>, Consumer<ArcticCmdCommandModule>>
            ADDITIONAL_CONFIGURATION = Collections.emptyMap();

    private final ArcticCommandModule commonCommandModule;

    /**
     * Creates a new ArcticCmdCommandModule that will read injection keys from the provided configuration.
     * @param config Apache configuration object
     */
    public ArcticCmdCommandModule(final Configuration config) {
        super(config);
        commonCommandModule = new ArcticCommandModule(config);
    }

    /**
     * Used by guice to initialize the module. This will install the {@link ArcticCommandModule} and install cmd_client
     * specific module as long as they match the general allowed commands restrictions.
     */
    public void configure() {
        install(commonCommandModule);

        final Multibinder<ArcticCommand> multiBinder = Multibinder.newSetBinder(binder(), ArcticCommand.class);

        List<Class<? extends ArcticCommand>> commands = commonCommandModule.getAllowedCommands(COMMANDS);
        commands.forEach(it -> {
            multiBinder.addBinding().to(it);
            ADDITIONAL_CONFIGURATION.getOrDefault(it, k -> { }).accept(this);
        });
    }
}
