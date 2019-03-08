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

import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.player.preprocessing.ArcticPlayerPreProcessor;
import com.amazon.corretto.arctic.player.preprocessing.impl.CleanUpPreProcessor;
import com.amazon.corretto.arctic.player.preprocessing.impl.EventsLoaderPreProcessor;
import com.amazon.corretto.arctic.player.preprocessing.impl.FirstTestDelayPreProcessor;
import com.amazon.corretto.arctic.player.preprocessing.impl.OverridesPreProcessor;
import com.amazon.corretto.arctic.player.preprocessing.impl.ScreenCheckValidatorPreProcessor;
import com.amazon.corretto.arctic.player.preprocessing.impl.TestDelayPreProcessor;
import com.amazon.corretto.arctic.player.preprocessing.impl.TimeControllerPreProcessor;
import com.amazon.corretto.arctic.player.preprocessing.impl.TruncationPreProcessor;
import com.google.inject.multibindings.Multibinder;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module to handle the injection of the different pre processors for Arctic Player. These pre-processors will form a
 * pipeline that will be executed before the test replay starts. Some of the functions of the pre-processors include
 * reading and modifying the test, prepare the screen for the replay or clean up Arctic status. During the execution of
 * the pre-processing pipeline the test is in status
 * {@link com.amazon.corretto.arctic.player.model.TestStatusCode#STARTING}. Any pre-processor can fail the test and
 * transition to {@link com.amazon.corretto.arctic.player.model.TestStatusCode#ABORTED}.
 * The order of execution of the pre-processors are defined by the value of
 * {@link ArcticPlayerPreProcessor#getPriority()}, not by the list order of the configuration key defined by
 * {@link InjectionKeys#PRE_PROCESSORS}.
 */
public final class ArcticPlayerPreModule extends ArcticModule {
    private static final Logger log = LoggerFactory.getLogger(ArcticPlayerPreModule.class);

    /**
     * Creates a new instance.
     * @param config An Apache Configuration object to read configuration from
     */
    public ArcticPlayerPreModule(final Configuration config) {
        super(config);
    }

    private static final Map<String, Class<? extends ArcticPlayerPreProcessor>> PRE_PROCESSORS = Map.of(
            FirstTestDelayPreProcessor.NAME, FirstTestDelayPreProcessor.class,
            OverridesPreProcessor.NAME, OverridesPreProcessor.class,
            TestDelayPreProcessor.NAME, TestDelayPreProcessor.class,
            ScreenCheckValidatorPreProcessor.NAME, ScreenCheckValidatorPreProcessor.class,
            EventsLoaderPreProcessor.NAME, EventsLoaderPreProcessor.class,
            TruncationPreProcessor.NAME, TruncationPreProcessor.class,
            TimeControllerPreProcessor.NAME, TimeControllerPreProcessor.class,
            CleanUpPreProcessor.NAME, CleanUpPreProcessor.class);

    private static final Map<String, Consumer<ArcticPlayerPreModule>> ADDITIONAL_CONFIGURATION = Map.of(
            FirstTestDelayPreProcessor.NAME, ArcticPlayerPreModule::configureFirstTestDelay,
            OverridesPreProcessor.NAME, ArcticPlayerPreModule::configureOverrides,
            ScreenCheckValidatorPreProcessor.NAME, ArcticPlayerPreModule::configureScreenCheckValidator
    );

    @Override
    public void configure() {
        check(InjectionKeys.PRE_PROCESSORS, PRE_PROCESSORS.keySet());
        final List<String> processors = getConfig().getList(String.class, InjectionKeys.PRE_PROCESSORS);
        processors.stream().filter(it -> !PRE_PROCESSORS.containsKey(it)).forEach(it -> {
            log.error("Invalid key value {}", it);
            fail(InjectionKeys.PRE_PROCESSORS, PRE_PROCESSORS.keySet());
        });

        final Multibinder<ArcticPlayerPreProcessor> multiBinder = Multibinder.newSetBinder(binder(),
                ArcticPlayerPreProcessor.class);
        processors.stream()
                .map(PRE_PROCESSORS::get)
                .forEach(it -> multiBinder.addBinding().to(it));
        processors.stream()
                .filter(ADDITIONAL_CONFIGURATION::containsKey)
                .map(ADDITIONAL_CONFIGURATION::get)
                .forEach(it -> it.accept(this));
        log.info("PreProcessors: {}", String.join(",", processors));

    }

    private void configureFirstTestDelay() {
        bindFromConfig(Long.class, InjectionKeys.PRE_FIRST_TEST_DELAY_WAIT, "A valid amount in ms");
    }

    private void configureScreenCheckValidator() {
        bindFromConfig(Long.class, InjectionKeys.PRE_SC_VALIDATOR_WAIT_FOCUS, "A valid amount in ms");
        bindFromConfig(Boolean.class, InjectionKeys.PRE_SC_VALIDATOR_BYPASS, "true to ignore the scValidator result");
    }

    private void configureOverrides() {
        install(new ArcticPlayerPreOverridesModule(getConfig()));
    }
}
