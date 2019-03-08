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

package com.amazon.corretto.arctic.common;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import com.amazon.corretto.arctic.shared.exception.ArcticException;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseMain {
    private static final Logger log = LoggerFactory.getLogger(BaseMain.class);

    protected static Configuration getConfiguration(final String propertiesFile) throws ConfigurationException,
            IOException {
        return getConfiguration(new FileReader(propertiesFile));
    }

    protected static Configuration getConfiguration(final InputStream propertiesFile) throws ConfigurationException,
            IOException {
        return getConfiguration(new InputStreamReader(propertiesFile));
    }

    protected static Configuration getConfiguration(final Reader propertiesFile) throws ConfigurationException,
            IOException {
        final CombinedConfiguration config = new CombinedConfiguration(new OverrideCombiner());
        config.addConfiguration(new SystemConfiguration());
        final PropertiesConfiguration propertiesConfig = new PropertiesConfiguration();
        propertiesConfig.setListDelimiterHandler(new DefaultListDelimiterHandler(','));
        propertiesConfig.read(propertiesFile);
        config.addConfiguration(propertiesConfig);
        return config;
    }

    protected static void writeConfig(final Class<? extends BaseMain> klass, final String propertiesFile) {
        log.info("Writing configuration file into: {}", propertiesFile);
        //Get file from resources folder
        final ClassLoader classLoader = klass.getClassLoader();
        try {
            final InputStream input = classLoader.getResource(propertiesFile).openStream();
            Files.copy(input, Paths.get(propertiesFile), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new ArcticException("Unable to copy properties file", e);
        }
    }

    protected static boolean isWriteConfig(final String[] args) {
        return Arrays.asList(args).contains("--writeConfig");
    }

    protected enum CommandLineOption {
        HELP("h", "help", "Shows this message"),
        VERSION("v", "version", "Show the version"),
        RECORDER("r", "recorder", "Run Arctic Recorder"),
        PLAYER("p", "player", "Run Arctic Player"),
        COMMAND("c", "command", "Issue an Arctic command via RMI"),
        INTERACTIVE("i", "interactive", "Start an interactive Arctic CLI"),
        DUMPER("d", "dumpkeys", "Dumps the awt/jnh table for this system");

        private static final int WIDTH = 25;
        private final String shortOption;
        private final String longOption;
        private final String help;

        public static final List<CommandLineOption> OPTIONS = List.of(
                HELP,
                VERSION,
                RECORDER,
                PLAYER,
                COMMAND,
                INTERACTIVE,
                DUMPER);

        CommandLineOption(final String shortOption, final String longOption, final String help) {
            this.shortOption = shortOption;
            this.longOption = longOption;
            this.help = help;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (shortOption != null && !shortOption.equals("")) {
                sb.append("-").append(shortOption).append(", ");
            }
            sb.append("--").append(longOption);
            sb.append(String.format("%1$" + (sb.length() < (WIDTH - 2) ? WIDTH - sb.length() : 2) + "s", "- "))
                    .append(help);
            return sb.toString();
        }

        public static CommandLineOption parseArgs(final String[] args) {
            if (args.length > 0) {
                return BaseMain.CommandLineOption.OPTIONS.stream()
                        .filter(it -> it.isOption(args[0]))
                        .findAny()
                        .orElse(BaseMain.CommandLineOption.HELP);
            }
            return BaseMain.CommandLineOption.HELP;
        }

        private boolean isOption(final String arg) {
            if (arg == null || arg.equals("")) {
                return false;
            }
            if (arg.startsWith("--")) {
                return arg.equalsIgnoreCase("--" + longOption);
            }
            if (arg.startsWith("-")) {
                return arg.equalsIgnoreCase("-" + shortOption);
            }
            return false;
        }
    }
}
