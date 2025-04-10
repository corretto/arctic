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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.common.exception.ArcticConfigurationException;
import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.common.model.event.KeyboardEvent;
import com.amazon.corretto.arctic.common.tweak.ArcticTweakableComponent;
import com.amazon.corretto.arctic.player.backend.ArcticBackendPlayer;
import com.amazon.corretto.arctic.player.backend.MultiBackendPlayer;
import com.amazon.corretto.arctic.player.backend.converters.ArcticKeyboardEvent2JnhKeyEvent;
import com.amazon.corretto.arctic.player.backend.converters.JnhKeyCode2AwtKeyCode;
import com.amazon.corretto.arctic.player.backend.impl.ArcticImageCheckPlayer;
import com.amazon.corretto.arctic.player.backend.impl.AwtRobotKeyboardBackendPlayer;
import com.amazon.corretto.arctic.player.backend.impl.AwtRobotMouseBackendPlayer;
import com.amazon.corretto.arctic.player.backend.impl.JnhKeyboardBackendPlayer;
import com.amazon.corretto.arctic.player.backend.impl.JnhMouseBackendPlayer;
import com.amazon.corretto.arctic.player.backend.impl.JnhMouseWheelBackendPlayer;
import com.github.kwhat.jnativehook.DefaultLibraryLocator;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import jakarta.inject.Singleton;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.inject.name.Names.named;
import static java.util.function.Predicate.not;

/**
 * A module to handle the injection of the different BackendPlayers.
 */
public final class ArcticBackendPlayerModule extends ArcticModule {
    private static final Logger log = LoggerFactory.getLogger(ArcticBackendPlayerModule.class);

    private static final Map<String, Class<? extends ArcticBackendPlayer>> PLAYERS = Map.of(
            ArcticImageCheckPlayer.NAME, ArcticImageCheckPlayer.class,
            AwtRobotKeyboardBackendPlayer.NAME, AwtRobotKeyboardBackendPlayer.class,
            AwtRobotMouseBackendPlayer.NAME, AwtRobotMouseBackendPlayer.class,
            JnhKeyboardBackendPlayer.NAME, JnhKeyboardBackendPlayer.class,
            JnhMouseBackendPlayer.NAME, JnhMouseBackendPlayer.class,
            JnhMouseWheelBackendPlayer.NAME, JnhMouseWheelBackendPlayer.class);

    private static final Map<String, Consumer<ArcticBackendPlayerModule>> ADDITIONAL_CONFIGURATION = Map.of(
            ArcticImageCheckPlayer.NAME, ArcticBackendPlayerModule::configureSc,
            JnhMouseBackendPlayer.NAME, ArcticBackendPlayerModule::configureJnhMouse,
            JnhKeyboardBackendPlayer.NAME, ArcticBackendPlayerModule::configureJnhKeyboard,
            AwtRobotMouseBackendPlayer.NAME, ArcticBackendPlayerModule::configureAwtRobotMouse,
            AwtRobotKeyboardBackendPlayer.NAME, ArcticBackendPlayerModule::configureAwtRobotKeyboard);

    /**
     * Creates a new instance of the module for a provided configuration.
     * @param config Configuration used to create the module.
     */
    public ArcticBackendPlayerModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {
        java.util.logging.Logger.getLogger(GlobalScreen.class.getName()).setLevel(Level.OFF);
        java.util.logging.Logger.getLogger(DefaultLibraryLocator.class.getName()).setLevel(Level.OFF);
        Multibinder.newSetBinder(binder(), new TypeLiteral<ArcticTweakableComponent>() {})
                .addBinding().to(MultiBackendPlayer.class).in(Singleton.class);

        check(InjectionKeys.BACKEND_PLAYERS, PLAYERS.keySet());
        final List<String> recorders = getConfig().getList(String.class, InjectionKeys.BACKEND_PLAYERS);
        recorders.stream().filter(not(PLAYERS::containsKey)).forEach(it -> {
            log.error("Invalid key value {}", it);
            fail(InjectionKeys.BACKEND_PLAYERS, PLAYERS.keySet());
        });

        final Multibinder<ArcticBackendPlayer> multiBinder =
                Multibinder.newSetBinder(binder(), ArcticBackendPlayer.class);
        recorders.stream()
                .map(PLAYERS::get)
                .forEach(it -> multiBinder.addBinding().to(it));
        recorders.stream()
                .filter(ADDITIONAL_CONFIGURATION::containsKey)
                .map(ADDITIONAL_CONFIGURATION::get)
                .forEach(it -> it.accept(this));
    }

    private void configureSc() {
        install(new ArcticBackendPlayerScModule(getConfig()));
    }

    private void configureJnhKeyboard() {
        bind(new TypeLiteral<Function<KeyboardEvent, NativeKeyEvent>>(){}).to(ArcticKeyboardEvent2JnhKeyEvent.class);
    }

    private void configureJnhMouse() {
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_PLAYERS_JNH_MOUSE_EVENTS,
                "a mask of events to play");
    }

    private void configureAwtRobotMouse() {
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_PLAYERS_AWT_MOUSE_BUTTON1,
                "a value from java.awt.event.InputEvent");
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_PLAYERS_AWT_MOUSE_BUTTON2,
                "a value from java.awt.event.InputEvent");
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_PLAYERS_AWT_MOUSE_BUTTON3,
                "a value from java.awt.event.InputEvent");
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_PLAYERS_AWT_MOUSE_EVENTS,
                "a mask of events to play");
    }

    private void configureAwtRobotKeyboard() {
        boolean bundled = getConfig().getBoolean(InjectionKeys.BACKEND_PLAYERS_AWT_KB_KEYMAP_BUNDLED);
        String name = getConfig().getString(InjectionKeys.BACKEND_PLAYERS_AWT_KB_KEYMAP);
        List<String> keymapData;

        if (bundled) {
            URL resourceUrl = ArcticBackendPlayerModule.class.getClassLoader().getResource(name);
            if (resourceUrl == null) {
                throw new ArcticConfigurationException(InjectionKeys.BACKEND_PLAYERS_AWT_KB_KEYMAP,
                        "Unknown bundled keymap " + name);
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8))) {
                keymapData = br.lines().collect(Collectors.toList());
            } catch (IOException ioe) {
                throw new ArcticConfigurationException(InjectionKeys.BACKEND_PLAYERS_AWT_KB_KEYMAP,
                        "Unable to load bundled keymap " + name, ioe);
            }
        } else {
            try {
                Path path = Paths.get(name);
                keymapData = Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (IOException ioe) {
                throw new ArcticConfigurationException(InjectionKeys.BACKEND_PLAYERS_AWT_KB_KEYMAP,
                        "Unable to load keymap " + name + " from disk", ioe);
            }
        }
        bind(new TypeLiteral<List<String>>(){})
                .annotatedWith(named(InjectionKeys.BACKEND_PLAYERS_AWT_KB_KEYMAP))
                .toInstance(keymapData);
        bind(new TypeLiteral<Function<KeyboardEvent, Integer>>(){})
                .annotatedWith(named(InjectionKeys.BACKEND_PLAYERS_AWT_KB_KEYMAP))
                .to(JnhKeyCode2AwtKeyCode.class);
    }
}
