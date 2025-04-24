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

import java.util.Map;
import java.util.function.Consumer;

import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.common.tweak.ArcticTweakableComponent;
import com.amazon.corretto.arctic.player.control.TimeController;
import com.amazon.corretto.arctic.player.control.impl.AdvancedTimeController;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import jakarta.inject.Singleton;
import org.apache.commons.configuration2.Configuration;

/**
 * Module to inject the proper time controller and its required properties. The time controller is responsible for
 * deciding at which moment each event needs to be replayed. This might mimic the original timeline or delay/advance it.
 */
public final class ArcticTimeControllerModule extends ArcticModule {
    private static final Map<String, Class<? extends TimeController>> CONTROLLERS = Map.of(
            AdvancedTimeController.NAME, AdvancedTimeController.class);

    private static final Map<String, Consumer<ArcticTimeControllerModule>> ADDITIONAL_CONFIGURATION = Map.of(
            AdvancedTimeController.NAME, ArcticTimeControllerModule::configureAdvTimeController);



    /**
     * Creates a new instance of the module, using a proper configuration.
     * @param config A valid Apache configuration.
     */
    public ArcticTimeControllerModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {

        check(InjectionKeys.TIME_CONTROLLER, CONTROLLERS.keySet());
        final String controller = getConfig().getString(InjectionKeys.TIME_CONTROLLER);
        if (!CONTROLLERS.containsKey(controller)) {
            fail(InjectionKeys.TIME_CONTROLLER, CONTROLLERS.keySet());
        }
        Class<? extends TimeController> tc = CONTROLLERS.get(controller);
        if (ADDITIONAL_CONFIGURATION.containsKey(controller)) {
            ADDITIONAL_CONFIGURATION.get(controller).accept(this);
        }
        bind(TimeController.class).to(tc).in(Singleton.class);
    }

    private void configureAdvTimeController() {
        Multibinder.newSetBinder(binder(), new TypeLiteral<ArcticTweakableComponent>(){})
                .addBinding().to(AdvancedTimeController.class).in(Singleton.class);
    }
}
