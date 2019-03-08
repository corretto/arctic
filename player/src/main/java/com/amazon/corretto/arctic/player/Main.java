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
package com.amazon.corretto.arctic.player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.amazon.corretto.arctic.common.BaseMain;
import com.amazon.corretto.arctic.common.command.interpreter.impl.ArcticRmiInterpreter;
import com.amazon.corretto.arctic.common.command.interpreter.impl.ConsoleCommandInterpreter;
import com.amazon.corretto.arctic.common.control.TestController;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.player.inject.ArcticPlayerModule;
import com.amazon.corretto.arctic.shared.exception.ArcticException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.inject.name.Names.named;

public class Main extends BaseMain {
    private final static Logger log = LoggerFactory.getLogger(Main.class);
    public static final String CONFIG_FILE = "player.properties";

    public static void main(final String[] args) {
        if (isWriteConfig(args) || !Files.exists(Paths.get(CONFIG_FILE))) {
            writeConfig(Main.class, CONFIG_FILE);
        } else {
            try {
                final Configuration config = getConfiguration(CONFIG_FILE);
                log.debug("Started");
                final Injector injector = Guice.createInjector(new ArcticPlayerModule(config));
                ArcticPlayer player = injector.getInstance(ArcticPlayer.class);
                TestController testController = injector.getInstance(TestController.class);
                testController.register(player);
                final boolean enabledRmi = injector.getInstance(Key.get(Boolean.class, named(CommonInjectionKeys.CMD_RMI_ENABLED)));
                if (enabledRmi) {
                    final ArcticRmiInterpreter interpreter = injector.getInstance(ArcticRmiInterpreter.class);
                    interpreter.start();
                }
                final boolean enabledCmd = injector.getInstance(Key.get(Boolean.class, named(CommonInjectionKeys.CMD_CONSOLE_ENABLED)));
                if (enabledCmd) {
                    final ConsoleCommandInterpreter interpreter = injector.getInstance(ConsoleCommandInterpreter.class);
                    interpreter.start();
                }
            } catch (final ConfigurationException | IOException e) {
                log.error("Unable to read configuration");
                throw new ArcticException("Unable to read configuration file " + CONFIG_FILE, e);
            }
        }
    }
}
