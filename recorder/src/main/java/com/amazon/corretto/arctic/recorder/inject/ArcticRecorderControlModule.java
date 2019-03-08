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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.recorder.control.ArcticController;
import com.amazon.corretto.arctic.recorder.control.impl.JnhKeyCaptureController;
import com.google.common.collect.ImmutableList;
import com.google.inject.TypeLiteral;
import org.apache.commons.configuration2.Configuration;
import com.github.kwhat.jnativehook.GlobalScreen;

import static com.google.inject.name.Names.named;

public final class ArcticRecorderControlModule extends ArcticModule {
    private static final Map<String, Consumer<ArcticRecorderControlModule>> PROVIDERS = Map.of(
            "jnh", ArcticRecorderControlModule::configureJnh
    );


    public ArcticRecorderControlModule(final Configuration config) {
        super(config);
        final Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
    }

    @Override
    public void configure() {
        final Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

        check(InjectionKeys.CONTROL_PROVIDER, PROVIDERS.keySet());
        final String nameProvider = getConfig().getString(InjectionKeys.CONTROL_PROVIDER);
        if (!PROVIDERS.containsKey(nameProvider)) {
            fail(InjectionKeys.CONTROL_PROVIDER, PROVIDERS.keySet());
        }
        PROVIDERS.get(nameProvider).accept(this);
        bindFromConfig(Boolean.class, InjectionKeys.CONTROL_AUTO_STOP, "true to stop recording automatically");
    }

    private static final List<String> KEYCODE_LIST = ImmutableList.of(
            InjectionKeys.CONTROL_JNH_START_KEYCODE,
            InjectionKeys.CONTROL_JNH_STOP_KEYCODE,
            InjectionKeys.CONTROL_JNH_SCREEN_CHECK_KEYCODE,
            InjectionKeys.CONTROL_JNH_SPAWN_SHADE_KEYCODE,
            InjectionKeys.CONTROL_JNH_DISCARD_KEYCODE
    );

    private void configureJnh() {
        KEYCODE_LIST.forEach(it -> bindFromConfig(Integer.class, it, "any valid keycode"));
        check(InjectionKeys.CONTROL_JNH_MODIFIERS, "a list of keycode modifiers");
        final List<Integer> modifiersList = getConfig().getList(Integer.class, InjectionKeys.CONTROL_JNH_MODIFIERS);
        bind(new TypeLiteral<List<Integer>>(){}).annotatedWith(named(InjectionKeys.CONTROL_JNH_MODIFIERS))
                .toInstance(modifiersList);
        bind(ArcticController.class).to(JnhKeyCaptureController.class);
    }
}
