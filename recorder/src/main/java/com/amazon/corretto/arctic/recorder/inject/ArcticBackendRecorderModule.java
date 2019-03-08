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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.common.model.event.KeyboardEvent;
import com.amazon.corretto.arctic.common.model.event.MouseEvent;
import com.amazon.corretto.arctic.recorder.backend.ArcticBackendRecorder;
import com.amazon.corretto.arctic.recorder.backend.converters.JnhNativeKeyEvent2ArcticEvent;
import com.amazon.corretto.arctic.recorder.backend.converters.JnhNativeMouseEvent2ArcticEvent;
import com.amazon.corretto.arctic.recorder.backend.converters.JnhNativeMouseWheelEvent2ArcticEvent;
import com.amazon.corretto.arctic.recorder.backend.impl.JnhKeyboardRecorder;
import com.amazon.corretto.arctic.recorder.backend.impl.JnhMouseMoveRecorder;
import com.amazon.corretto.arctic.recorder.backend.impl.JnhMouseRecorder;
import com.amazon.corretto.arctic.recorder.backend.impl.JnhMouseWheelRecorder;
import com.amazon.corretto.arctic.recorder.backend.impl.ScreenCheckBackendRecorder;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseWheelEvent;

/**
 * This class handles the injection of different recorders. A map called RECORDERS holds all the possible associations
 * of strings to the actual recorder class. Every recorder listed in {@link InjectionKeys#BACKEND_RECORDERS} will be
 * added to a Guice MultiBinder.
 *
 * The ADDITIONAL_CONFIGURATION map allows for extra configuration to be done if a specific recorder is injected. It
 * maps each injector with a method that will be called. This can be done to ensure all dependencies of the recorder are
 * also injected.
 *
 * This class also injects the value for the {@link InjectionKeys#BACKEND_RECORDING_MODE}, which establishes which
 * events should we listen to. By default, it is recommended to capture all the events, and then use the playback mode
 * to filter those we do not want (like mouse CLICK causing duplicates with mouse PRESS and RELEASE).
 */
@Slf4j
public final class ArcticBackendRecorderModule extends ArcticModule {
    private static final Map<String, Class<? extends ArcticBackendRecorder>> RECORDERS = Map.of(
                    ScreenCheckBackendRecorder.NAME, ScreenCheckBackendRecorder.class,
                    JnhMouseRecorder.NAME, JnhMouseRecorder.class,
                    JnhMouseMoveRecorder.NAME, JnhMouseMoveRecorder.class,
                    JnhMouseWheelRecorder.NAME, JnhMouseWheelRecorder.class,
                    JnhKeyboardRecorder.NAME, JnhKeyboardRecorder.class);

    private static final Map<String, Consumer<ArcticBackendRecorderModule>> ADDITIONAL_CONFIGURATION = Map.of(
            JnhMouseRecorder.NAME, ArcticBackendRecorderModule::registerJnhMouse,
            JnhMouseMoveRecorder.NAME, ArcticBackendRecorderModule::registerJnhMouseMove,
            JnhMouseWheelRecorder.NAME, ArcticBackendRecorderModule::registerJnhMouseWheel,
            JnhKeyboardRecorder.NAME, ArcticBackendRecorderModule::registerJnhKeyboard);

    /**
     * Constructor for the module.
     * @param config A copy of the configuration as read by Arctic.
     */
    public ArcticBackendRecorderModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {
        bindFromConfig(Integer.class, InjectionKeys.BACKEND_RECORDING_MODE, "a valid binary mask");
        check(InjectionKeys.BACKEND_RECORDERS, RECORDERS.keySet());
        final List<String> recorders = getConfig().getList(String.class, InjectionKeys.BACKEND_RECORDERS);
        recorders.stream().filter(it -> !RECORDERS.containsKey(it)).forEach(it -> {
            log.error("Invalid key value {}", it);
            fail(InjectionKeys.BACKEND_RECORDERS, RECORDERS.keySet());
        });

        final Multibinder<ArcticBackendRecorder> multiBinder = Multibinder.newSetBinder(binder(),
                ArcticBackendRecorder.class);
        recorders.stream()
                .map(RECORDERS::get)
                .forEach(it -> multiBinder.addBinding().to(it));
        recorders.stream()
                .filter(ADDITIONAL_CONFIGURATION::containsKey)
                .map(ADDITIONAL_CONFIGURATION::get)
                .forEach(it -> it.accept(this));
    }

    private boolean jnhMouseEventConverterBound = false;
    private void registerJnhMouse() {
        if (!jnhMouseEventConverterBound) {
            jnhMouseEventConverterBound = true;
            bind(new TypeLiteral<Function<NativeMouseEvent, MouseEvent>>(){}).to(JnhNativeMouseEvent2ArcticEvent.class);
        }
    }

    private void registerJnhMouseMove() {
        if (!jnhMouseEventConverterBound) {
            jnhMouseEventConverterBound = true;
            bind(new TypeLiteral<Function<NativeMouseEvent, MouseEvent>>(){}).to(JnhNativeMouseEvent2ArcticEvent.class);
        }
    }

    private void registerJnhMouseWheel() {
        bind(new TypeLiteral<Function<NativeMouseWheelEvent, MouseEvent>>(){})
                .to(JnhNativeMouseWheelEvent2ArcticEvent.class);
    }

    private void registerJnhKeyboard() {
        bind(new TypeLiteral<Function<NativeKeyEvent, KeyboardEvent>>(){}).to(JnhNativeKeyEvent2ArcticEvent.class);
    }
}
