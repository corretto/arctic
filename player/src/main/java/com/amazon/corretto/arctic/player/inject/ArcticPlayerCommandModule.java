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

package com.amazon.corretto.arctic.player.inject;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.inject.ArcticCommandModule;
import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;
import com.amazon.corretto.arctic.player.command.impl.XmlCommand;
import com.amazon.corretto.arctic.player.command.impl.XmlPrintCommand;
import com.amazon.corretto.arctic.player.command.impl.XmlSaveCommand;
import com.amazon.corretto.arctic.player.command.impl.JtxCommand;
import com.amazon.corretto.arctic.player.command.impl.JtxPrintCommand;
import com.amazon.corretto.arctic.player.command.impl.JtxSaveCommand;
import com.amazon.corretto.arctic.player.command.impl.ScCommand;
import com.amazon.corretto.arctic.player.command.impl.TapCommand;
import com.amazon.corretto.arctic.player.command.impl.TapPrintCommand;
import com.amazon.corretto.arctic.player.command.impl.TapSaveCommand;
import com.amazon.corretto.arctic.player.command.impl.TestClearCommand;
import com.amazon.corretto.arctic.common.command.impl.TestCommand;
import com.amazon.corretto.arctic.player.command.impl.TestListCommand;
import com.amazon.corretto.arctic.common.command.impl.TestStartCommand;
import com.google.inject.multibindings.Multibinder;
import org.apache.commons.configuration2.Configuration;

import static com.google.inject.name.Names.named;
import static java.util.Map.entry;

/**
 * Main module for Arctic Player commands. It installs an {@link ArcticCommandModule} and relies on its logic for the
 * command filtering.
 */
public final class ArcticPlayerCommandModule extends ArcticModule {
    private static final Map<String[], Class<? extends ArcticCommand>> COMMANDS = Map.ofEntries(
            entry(JtxCommand.COMMAND_LINE, JtxCommand.class),
            entry(JtxPrintCommand.COMMAND_LINE, JtxPrintCommand.class),
            entry(JtxSaveCommand.COMMAND_LINE, JtxSaveCommand.class),
            entry(XmlCommand.COMMAND_LINE, XmlCommand.class),
            entry(XmlPrintCommand.COMMAND_LINE, XmlPrintCommand.class),
            entry(XmlSaveCommand.COMMAND_LINE, XmlSaveCommand.class),
            entry(TapCommand.COMMAND_LINE, TapCommand.class),
            entry(TapPrintCommand.COMMAND_LINE, TapPrintCommand.class),
            entry(TapSaveCommand.COMMAND_LINE, TapSaveCommand.class),
            entry(TestClearCommand.COMMAND_LINE, TestClearCommand.class),
            entry(TestStartCommand.COMMAND_LINE, TestStartCommand.class),
            entry(TestListCommand.COMMAND_LINE, TestListCommand.class),
            entry(TestCommand.COMMAND_LINE, TestCommand.class),
            entry(ScCommand.COMMAND_LINE, ScCommand.class));

    private static final Map<Class<? extends ArcticCommand>, Consumer<ArcticPlayerCommandModule>>
            ADDITIONAL_CONFIGURATION = Map.of(
                    ScCommand.class, ArcticPlayerCommandModule::configureScCommand);

    private final ArcticCommandModule commonCommandModule;

    /**
     * Creates a new instance.
     * @param config An Apache Configuration object to read configuration from
     */
    public ArcticPlayerCommandModule(final Configuration config) {
        super(config);
        commonCommandModule = new ArcticCommandModule(config);
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

    private void configureScCommand() {
        String reviewOrder = getConfig().getString(InjectionKeys.GUI_REVIEW_ORDER);
        PixelCheck.Type type = PixelCheck.Type.fromString(reviewOrder);
        bind(PixelCheck.Type.class).annotatedWith(named(InjectionKeys.GUI_REVIEW_ORDER)).toInstance(type);
    }

}
