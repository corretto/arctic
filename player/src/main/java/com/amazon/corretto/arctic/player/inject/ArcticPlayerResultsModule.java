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

import com.amazon.corretto.arctic.common.command.impl.SessionCommand;
import com.amazon.corretto.arctic.common.inject.ArcticModule;
import com.amazon.corretto.arctic.common.serialization.ArcticTypeAdapter;
import com.amazon.corretto.arctic.common.serialization.TestIdTypeAdapter;
import com.amazon.corretto.arctic.common.session.ArcticSessionKeeper;
import com.amazon.corretto.arctic.player.results.ArcticScFailureKeeper;
import com.amazon.corretto.arctic.player.results.ArcticTestResultsKeeper;
import com.amazon.corretto.arctic.player.results.impl.InMemoryScFailureKeeper;
import com.amazon.corretto.arctic.player.results.impl.InMemoryTestResultsKeeper;
import com.amazon.corretto.arctic.player.serialization.FailureIdTypeAdapter;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import jakarta.inject.Singleton;
import org.apache.commons.configuration2.Configuration;

import static com.google.inject.name.Names.named;

/**
 * Module for Dependency Injection of results keepers. These objects keep track of the different results and failures.
 * Due to their nature, they are injected as Singleton, as we want one single copy of each keeper to receive all data.
 */
public final class ArcticPlayerResultsModule extends ArcticModule {

    /**
     * Creates a new instance.
     * @param config An Apache Configuration object.
     */
    public ArcticPlayerResultsModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {
        bind(ArcticTestResultsKeeper.class).to(InMemoryTestResultsKeeper.class).in(Singleton.class);
        bind(ArcticScFailureKeeper.class).to(InMemoryScFailureKeeper.class).in(Singleton.class);

        final Multibinder<ArcticSessionKeeper<?, ?>> keepers =
                Multibinder.newSetBinder(binder(), new TypeLiteral<>() {});
        keepers.addBinding().to(ArcticTestResultsKeeper.class);
        keepers.addBinding().to(ArcticScFailureKeeper.class);

        // Register specific TypeAdapters needed for session serialization of Player objects
        final Multibinder<ArcticTypeAdapter<?>> typeAdapters = Multibinder.newSetBinder(binder(),
                new TypeLiteral<>(){}, named(SessionCommand.SESSION_GSON));
        typeAdapters.addBinding().to(FailureIdTypeAdapter.class);
        typeAdapters.addBinding().to(TestIdTypeAdapter.class);
    }
}
