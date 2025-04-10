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

package com.amazon.corretto.arctic.common.inject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.command.impl.LogSetCommand;
import com.amazon.corretto.arctic.common.command.impl.LogTestCommand;
import com.amazon.corretto.arctic.common.command.impl.QuitCommand;
import com.amazon.corretto.arctic.common.command.impl.SessionCommand;
import com.amazon.corretto.arctic.common.command.impl.ShadeCommand;
import com.amazon.corretto.arctic.common.command.impl.TerminateCommand;
import com.amazon.corretto.arctic.common.command.impl.TestCommand;
import com.amazon.corretto.arctic.common.command.impl.TestFinishCommand;
import com.amazon.corretto.arctic.common.command.impl.TestGroupFinishCommand;
import com.amazon.corretto.arctic.common.command.impl.TestGroupStartCommand;
import com.amazon.corretto.arctic.common.command.impl.TestStartCommand;
import com.amazon.corretto.arctic.common.command.impl.TweakCommand;
import com.amazon.corretto.arctic.common.command.impl.WaitCommand;
import com.amazon.corretto.arctic.common.command.impl.WorkbenchCommand;
import com.amazon.corretto.arctic.common.command.interpreter.impl.ArcticRmiInterpreter;
import com.amazon.corretto.arctic.common.serialization.ArcticTypeAdapter;
import com.amazon.corretto.arctic.common.serialization.GsonPathAdapter;
import com.amazon.corretto.arctic.common.session.ArcticSessionKeeper;
import com.amazon.corretto.arctic.common.tweak.ArcticTweakableComponent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.inject.name.Names.named;
import static java.util.Map.entry;


/**
 * Module to install ArcticCommands that can be used by different Arctic modes of operation, like player and recorder.
 * This module also includes the logic to resolve which commands are allowed, and whether we accept commands via console
 * or RMI.
 */
public final class ArcticCommandModule extends ArcticModule {
    private static final Logger log = LoggerFactory.getLogger(ArcticCommandModule.class);

    private static final Map<String[], Class<? extends ArcticCommand>> COMMANDS = Map.ofEntries(
            entry(QuitCommand.COMMAND_LINE, QuitCommand.class),
            entry(TerminateCommand.COMMAND_LINE, TerminateCommand.class),
            entry(WorkbenchCommand.COMMAND_LINE, WorkbenchCommand.class),
            entry(ShadeCommand.COMMAND_LINE, ShadeCommand.class),
            entry(WaitCommand.COMMAND_LINE, WaitCommand.class),
            entry(LogSetCommand.COMMAND_LINE, LogSetCommand.class),
            entry(SessionCommand.COMMAND_LINE, SessionCommand.class),
            entry(LogTestCommand.COMMAND_LINE, LogTestCommand.class),
            entry(TweakCommand.COMMAND_LINE, TweakCommand.class),
            entry(TestCommand.COMMAND_LINE, TestCommand.class),
            entry(TestGroupStartCommand.COMMAND_LINE, TestGroupStartCommand.class),
            entry(TestGroupFinishCommand.COMMAND_LINE, TestGroupFinishCommand.class),
            entry(TestStartCommand.COMMAND_LINE, TestStartCommand.class),
            entry(TestFinishCommand.COMMAND_LINE, TestFinishCommand.class)
    );

    private static final Map<Class<? extends ArcticCommand>, Consumer<ArcticCommandModule>>
            ADDITIONAL_CONFIGURATION = Map.of(
                    SessionCommand.class, ArcticCommandModule::configureSessionCommand,
                    TweakCommand.class, ArcticCommandModule::configureConfigCommand
    );



    /**
     * Creates a new instance of ArcticCommandModule. This command is usually not installed directly, but installed by
     * another Command module that will include other more specific commands for that arctic mode.
     * @param config An apache configuration2 object used to read different keys from.
     */
    public ArcticCommandModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {
        final boolean enabled = getConfig().getBoolean(CommonInjectionKeys.CMD_ENABLED, true);
        bind(Boolean.class).annotatedWith(named(CommonInjectionKeys.CMD_ENABLED)).toInstance(enabled);

        final boolean consoleEnabled = enabled && getConfig().getBoolean(CommonInjectionKeys.CMD_CONSOLE_ENABLED, true);
        bind(Boolean.class).annotatedWith(named(CommonInjectionKeys.CMD_CONSOLE_ENABLED)).toInstance(consoleEnabled);
        final String rmiEnabledString = getConfig().getString(CommonInjectionKeys.CMD_RMI_ENABLED, String.valueOf(false));
        final boolean rmiLocalOnly = rmiEnabledString.equalsIgnoreCase(ArcticRmiInterpreter.LOCAL_ONLY_VALUE);
        final boolean rmiEnabled = enabled && (rmiLocalOnly || Boolean.parseBoolean(rmiEnabledString));
        bind(Boolean.class).annotatedWith(named(CommonInjectionKeys.CMD_RMI_ENABLED)).toInstance(rmiEnabled);
        if (rmiEnabled) {
            bindFromConfig(String.class, CommonInjectionKeys.CMD_RMI_NAME, "Name for the RMI server");
            bindFromConfig(Integer.class, CommonInjectionKeys.CMD_RMI_PORT, "Port for the RMI server");
            bind(Boolean.class).annotatedWith(named(CommonInjectionKeys.CMD_RMI_SECURITY_LOCAL_ONLY)).toInstance(rmiLocalOnly);
        }

        final Multibinder<ArcticCommand> multiBinder = Multibinder.newSetBinder(binder(), ArcticCommand.class);
        if (consoleEnabled || rmiEnabled) {
            final List<Class<? extends ArcticCommand>> commands = getAllowedCommands(COMMANDS);
            commands.forEach(it -> {
                multiBinder.addBinding().to(it);
                ADDITIONAL_CONFIGURATION.getOrDefault(it, k -> { }).accept(this);
            });
        }

        final Multibinder<ArcticTypeAdapter<?>> typeAdapterMultibinder = Multibinder.newSetBinder(binder(),
                new TypeLiteral<>(){}, named(SessionCommand.SESSION_GSON));
    }

    /**
     * Filters a collection of commands, applying the rules based on allowed and disabled lists. This is done to filter
     * which of the common commands are allowed, but it is also used by other, more specific, command modules to filter
     * their own list.
     * @param commands A map of String to command classes
     * @param <T> A subclass of {@link ArcticCommand}
     * @return A list with the commands that are allowed
     */
    public <T extends ArcticCommand> List<Class<? extends T>> getAllowedCommands(final Map<String[],
            Class<? extends T>> commands) {
        final List<String[]> allowedList = getCommandList(CommonInjectionKeys.CMD_ALLOWED_ENABLED,
                CommonInjectionKeys.CMD_ALLOWED, () -> List.of(new String[][]{new String[]{"*"}}));
        final List<String[]> disallowed = getCommandList(CommonInjectionKeys.CMD_DISALLOWED_ENABLED,
                CommonInjectionKeys.CMD_DISALLOWED, Collections::emptyList);

        final boolean regexEnabled = getConfig().getBoolean(CommonInjectionKeys.CMD_REGEX_ENABLED);
        final Pattern regex = regexEnabled ? Pattern.compile(getConfig().getString(CommonInjectionKeys.CMD_REGEX))
                : null;

        final List<Class<? extends T>> allowedCommands = new ArrayList<>();
        for (final Map.Entry<String[], Class<? extends T>> entry : commands.entrySet()) {
            final String[] cmdline = entry.getKey();
            final boolean isAllowed = matchesList(cmdline, allowedList);
            final boolean isDisallowed = matchesList(cmdline, disallowed);
            final boolean filterMatch = !regexEnabled || matchesFilter(cmdline, regex);
            if (isAllowed && !isDisallowed && filterMatch) {
                log.debug("Adding {}", String.join("_", cmdline));
                allowedCommands.add(entry.getValue());
            } else {
                log.debug("Not adding {}: {}, {}, {}", String.join("_", cmdline), isAllowed, isDisallowed, filterMatch);
            }
        }
        return allowedCommands;
    }

    private boolean matchesFilter(final String[] cmdline, final Pattern regex) {
        return regex.matcher(String.join("_", cmdline)).matches();
    }

    private List<String[]> getCommandList(final String enabledKey, final String key,
                                          final Supplier<List<String[]>> defaultListSupplier) {
        final boolean enabled = getConfig().getBoolean(enabledKey);
        if (!enabled) {
            return defaultListSupplier.get();
        }
        return getConfig().getList(String.class, key)
                .stream()
                .map(String::toLowerCase)
                .map(it -> it.split("_"))
                .collect(Collectors.toList());
    }

    private boolean matchesList(final String[] command, final List<String[]> commandList) {
        cmdCheck: for (final String[] commandListEntry : commandList) {
            if (commandListEntry.length > command.length) {
                continue;
            }
            for (int i = 0; i < commandListEntry.length; i++) {
                if (!commandListEntry[i].equals("*") && !commandListEntry[i].equals(command[i])) {
                    continue cmdCheck;
                }
            }
            return true;
        }
        return false;
    }

    private void configureSessionCommand() {
        bind(Gson.class).annotatedWith(named(SessionCommand.SESSION_GSON)).toProvider(GsonSessionProvider.class)
                .in(Singleton.class);
        bindFromConfig(String.class, CommonInjectionKeys.SESSION_DEFAULT, "Name of the default session");
        Multibinder.newSetBinder(binder(), new TypeLiteral<ArcticSessionKeeper<?, ?>>() {});
    }

    private void configureConfigCommand() {
        Multibinder.newSetBinder(binder(), new TypeLiteral<ArcticTweakableComponent>() {});
    }

    /**
     * Specialized gson instance to be used for serializing and deserializing Arctic sessions.
     */
    public static final class GsonSessionProvider implements Provider<Gson> {
        private final Set<ArcticSessionKeeper<?, ?>> keepers;
        private final Set<ArcticTypeAdapter<?>> adapters;

        /**
         * Prepares a provider for Gson tailored to serialize and deserialize Arctic sessions.
         * @param keepers All the SessionKeepers installed. This is required for the gson deserialization of lists with
         *                polymorphic objects.
         * @param adapters A set of adapters used to fine tune the gson formatting. They can allow for hex encoding,
         *                 clean path serialization or internal test keys
         */
        @Inject
        public GsonSessionProvider(final Set<ArcticSessionKeeper<?, ?>> keepers,
                                   @Named(SessionCommand.SESSION_GSON) final Set<ArcticTypeAdapter<?>> adapters) {
            this.keepers = keepers;
            this.adapters = adapters;
        }

        @Override
        public Gson get() {
            final RuntimeTypeAdapterFactory<ArcticSessionKeeper.SessionObject> typeAdapter =
                    RuntimeTypeAdapterFactory.of(ArcticSessionKeeper.SessionObject.class, "type", false);
            typeAdapter.registerSubtype(SessionCommand.BasicSessionObject.class, SessionCommand.BASIC_SESSION_INFO);
            keepers.forEach(it -> typeAdapter.registerSubtype(it.getSessionObjectClass(), it.getClass().getName()));
            GsonBuilder builder = new GsonBuilder().setPrettyPrinting()
                    .registerTypeHierarchyAdapter(Path.class, new GsonPathAdapter())
                    .registerTypeAdapterFactory(typeAdapter)
                    .enableComplexMapKeySerialization();
            adapters.forEach(it -> builder.registerTypeAdapter(it.getAdaptedClass(), it));
            return builder.create();
        }
    }
}
