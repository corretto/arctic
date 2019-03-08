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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.amazon.corretto.arctic.common.backend.ArcticImageSaver;
import com.amazon.corretto.arctic.common.backend.impl.JavaImageIoSaver;
import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.recorder.postprocessing.ArcticRecorderPostProcessor;
import com.amazon.corretto.arctic.recorder.postprocessing.impl.DuplicateMovementPostProcessor;
import com.amazon.corretto.arctic.recorder.postprocessing.impl.KeyboardCoherencePostProcessor;
import com.amazon.corretto.arctic.recorder.postprocessing.impl.MouseCoherencePostProcessor;
import com.amazon.corretto.arctic.recorder.postprocessing.impl.ScreenCheckHashPostProcessor;
import com.amazon.corretto.arctic.recorder.postprocessing.impl.ScreenCheckSavePostProcessor;
import com.amazon.corretto.arctic.recorder.postprocessing.impl.TailClearPostProcessor;
import com.amazon.corretto.arctic.recorder.postprocessing.impl.TestSavePostProcessor;
import com.amazon.corretto.arctic.recorder.postprocessing.impl.TimestampFixPostProcessor;
import com.amazon.corretto.arctic.recorder.postprocessing.impl.WorkbenchToBackPostProcessor;
import com.google.inject.multibindings.Multibinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;

@Slf4j
public final class ArcticRecorderPpModule extends ArcticModule {
    private static final Map<String, Class<? extends ArcticRecorderPostProcessor>> POST_PROCESSORS = Map.of(
                    TimestampFixPostProcessor.NAME, TimestampFixPostProcessor.class,
                    DuplicateMovementPostProcessor.NAME, DuplicateMovementPostProcessor.class,
                    TailClearPostProcessor.NAME, TailClearPostProcessor.class,
                    ScreenCheckHashPostProcessor.NAME, ScreenCheckHashPostProcessor.class,
                    ScreenCheckSavePostProcessor.NAME, ScreenCheckSavePostProcessor.class,
                    TestSavePostProcessor.NAME, TestSavePostProcessor.class,
                    WorkbenchToBackPostProcessor.NAME, WorkbenchToBackPostProcessor.class,
                    KeyboardCoherencePostProcessor.NAME, KeyboardCoherencePostProcessor.class,
                    MouseCoherencePostProcessor.NAME, MouseCoherencePostProcessor.class);

    private static final Map<String, Consumer<ArcticRecorderPpModule>> ADDITIONAL_CONFIGURATION = Map.of(
                ScreenCheckHashPostProcessor.NAME, ArcticRecorderPpModule::configureScreenCheckHash,
                ScreenCheckSavePostProcessor.NAME, ArcticRecorderPpModule::configureScreenCheckSave,
                TestSavePostProcessor.NAME, ArcticRecorderPpModule::configureTestSave);

    public ArcticRecorderPpModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {
        check(InjectionKeys.POST_ENABLED, POST_PROCESSORS.keySet());
        final List<String> processors = getConfig().getList(String.class, InjectionKeys.POST_ENABLED);
        processors.stream().filter(it -> !POST_PROCESSORS.containsKey(it)).forEach(it -> {
            log.error("Invalid key value {}", it);
            fail(InjectionKeys.POST_ENABLED, POST_PROCESSORS.keySet());
        });

        final Multibinder<ArcticRecorderPostProcessor> multiBinder = Multibinder.newSetBinder(binder(), ArcticRecorderPostProcessor.class);
        processors.stream()
                .map(POST_PROCESSORS::get)
                .forEach(it -> addToBinder(multiBinder, it));
        processors.stream()
                .filter(ADDITIONAL_CONFIGURATION::containsKey)
                .map(ADDITIONAL_CONFIGURATION::get)
                .forEach(it -> it.accept(this));
    }

    private void configureTestSave() {
        bindFromConfig(String.class, InjectionKeys.POST_SAVE_EVENTS_FILENAME, "any valid fileName");
        bindFromConfig(Boolean.class, InjectionKeys.POST_SAVE_TEST_ZIP, Arrays.asList(true, false));
    }

    private void configureScreenCheckHash() {
        bindFromConfig(String.class, InjectionKeys.POST_SC_HASH_ALGORITHM, "a valid java security digest algorithm");
    }

    private void configureScreenCheckSave() {
        check(InjectionKeys.POST_SC_SAVE_FORMAT, "a valid java ImageIO format");
        check(InjectionKeys.POST_SC_SAVE_EXTENSION, "a valid java ImageIO format extension");
        final String imageFormat = getConfig().getString(InjectionKeys.POST_SC_SAVE_FORMAT);
        final String imageExtension = getConfig().getString(InjectionKeys.POST_SC_SAVE_EXTENSION);
        bind(ArcticImageSaver.class).toInstance(new JavaImageIoSaver(imageFormat, imageExtension));
    }

    private void addToBinder(final Multibinder<ArcticRecorderPostProcessor> multiBinder,
                             final Class<? extends ArcticRecorderPostProcessor> clazz) {
        multiBinder.addBinding().to(clazz);
    }
}
