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

import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.common.model.ArcticTestTruncations;
import com.amazon.corretto.arctic.recorder.preprocessing.ArcticRecorderPreProcessor;
import com.amazon.corretto.arctic.recorder.preprocessing.impl.FirstScCheckPreProcessor;
import com.amazon.corretto.arctic.recorder.preprocessing.impl.FocusPreProcessor;
import com.amazon.corretto.arctic.recorder.preprocessing.impl.InitialPreProcessor;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;

@Slf4j
public final class ArcticRecorderPreModule extends ArcticModule {
    private static final Map<String, Class<? extends ArcticRecorderPreProcessor>> PRE_PROCESSORS = Map.of(
                    InitialPreProcessor.NAME, InitialPreProcessor.class,
                    FirstScCheckPreProcessor.NAME, FirstScCheckPreProcessor.class,
                    FocusPreProcessor.NAME, FocusPreProcessor.class);

    private static final Map<String, Consumer<ArcticRecorderPreModule>> ADDITIONAL_CONFIGURATION = Map.of(
                InitialPreProcessor.NAME, ArcticRecorderPreModule::configureInitial,
                FirstScCheckPreProcessor.NAME, ArcticRecorderPreModule::configureFirstSc);



    public ArcticRecorderPreModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {
        check(InjectionKeys.PRE_ENABLED, PRE_PROCESSORS.keySet());
        final List<String> processors = getConfig().getList(String.class, InjectionKeys.PRE_ENABLED);
        processors.stream().filter(it -> !PRE_PROCESSORS.containsKey(it)).forEach(it -> {
            log.error("Invalid key value {}", it);
            fail(InjectionKeys.PRE_ENABLED, PRE_PROCESSORS.keySet());
        });

        final Multibinder<ArcticRecorderPreProcessor> multiBinder = Multibinder.newSetBinder(binder(),
                ArcticRecorderPreProcessor.class);
        processors.stream()
                .map(PRE_PROCESSORS::get)
                .forEach(it -> addToBinder(multiBinder, it));
        processors.stream()
                .filter(ADDITIONAL_CONFIGURATION::containsKey)
                .map(ADDITIONAL_CONFIGURATION::get)
                .forEach(it -> it.accept(this));
    }

    private void configureInitial() {
        bindFromConfig(Integer.class, InjectionKeys.PRE_INIT_PREFERRED_PLAY_MODE,
                "a valid arctic play mode binary mask");
        bindFromConfig(Integer.class, InjectionKeys.PRE_INIT_TRUNCATE_KB_START,
                "amount of keyboard events to skip at the beginning");
        bindFromConfig(Integer.class, InjectionKeys.PRE_INIT_TRUNCATE_KB_END,
                "amount of keyboard events to skip at the end");
        bindFromConfig(Integer.class, InjectionKeys.PRE_INIT_TRUNCATE_MOUSE_START,
                "amount of mouse events to skip at the beginning");
        bindFromConfig(Integer.class, InjectionKeys.PRE_INIT_TRUNCATE_MOUSE_END,
                "amount of mouse events to skip at the end");
    }

    private void configureFirstSc() {
        bindFromConfig(Integer.class, InjectionKeys.PRE_FIRST_SC_MATCH, "a valid integer pixel height");
        bindFromConfig(Integer.class, InjectionKeys.PRE_FIRST_SC_DELAY, "a valid amount of time in ms");
    }


    private void addToBinder(final Multibinder<ArcticRecorderPreProcessor> multiBinder,
                             final Class<? extends ArcticRecorderPreProcessor> clazz) {
        multiBinder.addBinding().to(clazz);
    }

    @Provides
    @Singleton
    public ArcticTestTruncations defaultTestTruncations(
            @Named(InjectionKeys.PRE_INIT_TRUNCATE_KB_START) final int kbStart,
            @Named(InjectionKeys.PRE_INIT_TRUNCATE_KB_END) final int kbEnd,
            @Named(InjectionKeys.PRE_INIT_TRUNCATE_MOUSE_START) final int mouseStart,
            @Named(InjectionKeys.PRE_INIT_TRUNCATE_MOUSE_END) final int mouseEnd) {
        final ArcticTestTruncations truncations = new ArcticTestTruncations();
        truncations.setKbStart(kbStart);
        truncations.setKbEnd(kbEnd);
        truncations.setMouseStart(mouseStart);
        truncations.setMouseEnd(mouseEnd);
        return truncations;
    }
}
