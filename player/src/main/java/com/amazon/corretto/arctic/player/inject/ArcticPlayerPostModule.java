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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.player.postprocessing.ArcticPlayerPostProcessor;
import com.amazon.corretto.arctic.player.postprocessing.impl.ResultUpdater;
import com.amazon.corretto.arctic.player.postprocessing.impl.TestAutoUpdater;
import com.amazon.corretto.arctic.player.postprocessing.impl.RecordingMigrator;
import com.google.inject.multibindings.Multibinder;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This module handles the injection of the post-processing pipeline and all the different post-processors
 * Current post-processors are:
 * - {@link ResultUpdater}: A postprocessor that will record the result of running the test. It will later feed this
 *   information to the different reporting components.
 * - {@link TestAutoUpdater}: A postprocessor that will save the test json file to disk again, persisting the different
 *   overrides.
 * - {@link RecordingMigrator}: A postprocessor that will completely replace the existing recorder with the results of
 *   the current run. This is used to migrate tests between platforms.
 */
public final class ArcticPlayerPostModule extends ArcticModule {
    private static final Logger log = LoggerFactory.getLogger(ArcticPlayerPostModule.class);

    /**
     * Creates a new instance of the module.
     * @param config An Apache Configuration2 object with all our configuration.
     */
    public ArcticPlayerPostModule(final Configuration config) {
        super(config);
    }

    private static final Map<String, Class<? extends ArcticPlayerPostProcessor>> POST_PROCESSORS = Map.of(
            ResultUpdater.NAME, ResultUpdater.class,
            TestAutoUpdater.NAME, TestAutoUpdater.class,
            RecordingMigrator.NAME, RecordingMigrator.class
    );

    private static final Map<String, Consumer<ArcticPlayerPostModule>> ADDITIONAL_CONFIGURATION = Map.of(
            TestAutoUpdater.NAME, ArcticPlayerPostModule::configureAutoUpdater,
            RecordingMigrator.NAME, ArcticPlayerPostModule::configureMigrator

    );

    @Override
    public void configure() {
        check(InjectionKeys.POST_PROCESSORS, POST_PROCESSORS.keySet());
        final List<String> processors = getConfig().getList(String.class, InjectionKeys.POST_PROCESSORS);

        processors.stream().filter(it -> !POST_PROCESSORS.containsKey(it)).forEach(it -> {
            log.error("Invalid key value {}", it);
            fail(InjectionKeys.PRE_PROCESSORS, POST_PROCESSORS.keySet());
        });

        final Multibinder<ArcticPlayerPostProcessor> multiBinder = Multibinder.newSetBinder(binder(),
                ArcticPlayerPostProcessor.class);
        processors.stream()
                .map(POST_PROCESSORS::get)
                .forEach(it -> multiBinder.addBinding().to(it));
        processors.stream()
                .filter(ADDITIONAL_CONFIGURATION::containsKey)
                .map(ADDITIONAL_CONFIGURATION::get)
                .forEach(it -> it.accept(this));
        log.info("PostProcessors: {}", String.join(",", processors));
    }

    private void configureAutoUpdater() {
        bindFromConfig(Boolean.class, InjectionKeys.POST_AUTO_UPDATER_SAVE, Arrays.asList(true, false));
    }

    private void configureMigrator() {
        bindFromConfig(Boolean.class, InjectionKeys.POST_MIGRATE, Arrays.asList(true, false));
    }
}
