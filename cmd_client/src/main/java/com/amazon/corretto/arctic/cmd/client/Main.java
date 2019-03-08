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

package com.amazon.corretto.arctic.cmd.client;

import java.io.IOException;

import com.amazon.corretto.arctic.cmd.client.inject.ArcticCmdModule;
import com.amazon.corretto.arctic.common.BaseMain;
import com.amazon.corretto.arctic.shared.exception.ArcticException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default entrypoint for Arctic cmd_client.
 */
public class Main extends BaseMain {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static final String CONFIG_FILE = "cmd_client.properties";
    private static final String USAGE = "Run Arctic commands.\n"
            + "Usage:\n";

    /**
     * Main method.
     * @param args CommandLine arguments
     */
    public static void main(final String[] args) {
        CommandLineOption option = BaseMain.CommandLineOption.parseArgs(args);
        main(option, args);
    }

    /**
     * Main method that already assumed the arguments have been parse to determine which
     * {@link com.amazon.corretto.arctic.common.BaseMain.CommandLineOption} will be executed. Arctic launcher will
     * call this method directly.
     * @param commandLineOption Which option to execute, console, command or print help
     * @param args CommandLine arguments
     */
    public static void main(final BaseMain.CommandLineOption commandLineOption, final String[] args) {
        if (commandLineOption.equals(CommandLineOption.HELP)) {
            printHelp();
            System.exit(0);
        }

        try {
            final Configuration config = getConfiguration(Main.class.getClassLoader().getResource(CONFIG_FILE)
                    .openStream());
            final Injector injector = Guice.createInjector(new ArcticCmdModule(config));
            switch (commandLineOption) {
                case COMMAND:
                    injector.getInstance(ArcticCommandExecutor.class).start(args);
                    break;
                case INTERACTIVE:
                    injector.getInstance(ArcticConsole.class).start();
                    break;
                default:
                    printHelp();
            }
        } catch (final ConfigurationException | IOException e) {
            log.error("Unable to read configuration");
            throw new ArcticException("Unable to read configuration file " + CONFIG_FILE, e);
        }
    }

    private static void printHelp() {
        System.out.print(USAGE);
        System.out.println(CommandLineOption.COMMAND);
        System.out.println(CommandLineOption.INTERACTIVE);
    }
}
