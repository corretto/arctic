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
package com.amazon.corretto.arctic.recorder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazon.corretto.arctic.common.BaseMain;
import com.amazon.corretto.arctic.common.command.interpreter.impl.ArcticRmiInterpreter;
import com.amazon.corretto.arctic.common.command.interpreter.impl.ConsoleCommandInterpreter;
import com.amazon.corretto.arctic.common.control.TestController;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.recorder.control.ArcticController;
import com.amazon.corretto.arctic.recorder.inject.ArcticRecorderModule;
import com.amazon.corretto.arctic.shared.exception.ArcticException;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;

import static com.google.inject.name.Names.named;

@Slf4j
public class Main extends BaseMain {
    public static final String CONFIG_FILE = "recorder.properties";

    private static final String USAGE = "Record actions for later playback.\n\n" +
            "Usage:\n" +
            "--help|-h             - Shows this message\n" +
            "--writeConfig         - Write an new config file to recorder.properties.\n";

    public static void main(final String[] args) {
        final Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        if (isUsage(args)) {
            usage();
        } else if (isWriteConfig(args) || !Files.exists(Paths.get(CONFIG_FILE))) {
            writeConfig(Main.class, CONFIG_FILE);
        } else {
            try {
                final Configuration config = getConfiguration(CONFIG_FILE);
                config.getKeys("org.slf4j.simpleLogger").forEachRemaining(it -> System.setProperty(it, config.getString(it)));
                log.debug("Started");
                final Injector injector = Guice.createInjector(new ArcticRecorderModule(config));
                final ArcticRecorder recorder = injector.getInstance(ArcticRecorder.class);
                final TestController testController = injector.getInstance(TestController.class);
                testController.register(recorder);
                final ArcticController controller = injector.getInstance(ArcticController.class);
                controller.register(recorder);
                final boolean enabledCmd = injector.getInstance(Key.get(Boolean.class,
                        named(CommonInjectionKeys.CMD_ENABLED)));
                final boolean enabledRmi = injector.getInstance(Key.get(Boolean.class,
                        named(CommonInjectionKeys.CMD_RMI_ENABLED)));

                if (enabledRmi) {
                    final ArcticRmiInterpreter interpreter = injector.getInstance(ArcticRmiInterpreter.class);
                    interpreter.start();
                }
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


    private static void usage() {
        System.out.print(USAGE);
    }

    private static boolean isUsage(final String[] args) {
        return Arrays.asList(args).contains("--help") || Arrays.asList(args).contains("-h");
    }

}
