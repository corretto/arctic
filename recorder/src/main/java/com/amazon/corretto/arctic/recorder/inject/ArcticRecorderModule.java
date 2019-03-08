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

import com.amazon.corretto.arctic.common.inject.ArcticCommonModule;
import com.amazon.corretto.arctic.common.inject.ArcticCommonRepositoryModule;
import com.amazon.corretto.arctic.common.inject.ArcticModule;
import org.apache.commons.configuration2.Configuration;

/**
 * This module is the main module for Arctic when running in recorder mode.
 */
public final class ArcticRecorderModule extends ArcticModule {

    /**
     * Constructor for the module. It receives a configuration that will be made available to other submodules.
     * @param config A reference to the configuration.
     */
    public ArcticRecorderModule(final Configuration config) {
        super(config);
    }

    @Override
    public void configure() {
        install(new ArcticCommonRepositoryModule(getConfig(), false));
        install(new ArcticCommonModule(getConfig()));
        install(new ArcticRecorderOffsetModule(getConfig()));
        install(new ArcticBackendRecorderModule(getConfig()));
        install(new ArcticRecorderControlModule(getConfig()));
        install(new ArcticRecorderPpModule(getConfig()));
        install(new ArcticRecorderPreModule(getConfig()));
        install(new ArcticRecorderCommandModule(getConfig()));
    }
}
