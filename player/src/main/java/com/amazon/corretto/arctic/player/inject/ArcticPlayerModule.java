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

import com.amazon.corretto.arctic.common.inject.ArcticCommonModule;
import com.amazon.corretto.arctic.common.inject.ArcticCommonRepositoryModule;
import com.amazon.corretto.arctic.common.inject.ArcticModule;
import org.apache.commons.configuration2.Configuration;

/**
 * Main guice module for Arctic Player. It installs other, more specialized modules, as well as handling the injection
 * of some global values like ConfirmationMode and FastMode.
 */
public final class ArcticPlayerModule extends ArcticModule {

    /**
     * Creates a new instance.
     * @param config An Apache Configuration object to read configuration from
     */
    public ArcticPlayerModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {
        install(new ArcticCommonModule(getConfig()));
        install(new ArcticCommonRepositoryModule(getConfig(), true));
        install(new ArcticBackendPlayerModule(getConfig()));
        install(new ArcticTimeControllerModule(getConfig()));
        install(new ArcticPlayerPreModule(getConfig()));
        install(new ArcticPlayerPostModule(getConfig()));
        install(new ArcticPlayerCommandModule(getConfig()));
        install(new ArcticPlayerResultsModule(getConfig()));

        bindFromConfig(Boolean.class, InjectionKeys.CONFIRMATION_MODE, Arrays.asList(true, false));
        bindFromConfig(Boolean.class, InjectionKeys.FAST_MODE, Arrays.asList(true, false));
    }
}
