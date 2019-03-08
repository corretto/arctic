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

package com.amazon.corretto.arctic.recorder.inject;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.inject.ArcticCommandModule;
import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.google.inject.multibindings.Multibinder;
import org.apache.commons.configuration2.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ArcticRecorderCommandModule extends ArcticModule {
    private static final Map<String[], Class<? extends ArcticCommand>> COMMANDS = Collections.emptyMap();

    private static final Map<Class<? extends ArcticCommand>, Consumer<ArcticRecorderCommandModule>>
            ADDITIONAL_CONFIGURATION = Collections.emptyMap();

    private final ArcticCommandModule commonCommandModule;

    public ArcticRecorderCommandModule(final Configuration config) {
        super(config);
        this.commonCommandModule = new ArcticCommandModule(config);
    }

    @Override
    public void configure() {
        install(commonCommandModule);

        boolean enabled = getConfig().getBoolean(CommonInjectionKeys.CMD_ENABLED, true);
        final Multibinder<ArcticCommand> multiBinder = Multibinder.newSetBinder(binder(), ArcticCommand.class);

        if (enabled) {
            List<Class<? extends ArcticCommand>> commands = commonCommandModule.getAllowedCommands(COMMANDS);
            commands.forEach(it -> {
                multiBinder.addBinding().to(it);
                ADDITIONAL_CONFIGURATION.getOrDefault(it, k -> { }).accept(this);
            });
        }
    }
}