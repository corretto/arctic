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

package com.amazon.corretto.arctic.player.command.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import javax.inject.Inject;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.player.exception.ArcticNoResultsException;
import com.amazon.corretto.arctic.player.results.impl.TapResultsConverter;

/**
 * A command to write the test results as a tap file.
 */
public final class TapSaveCommand extends ArcticCommand {

    public static final String[] COMMAND_LINE = new String[]{"tap", "save"};
    private final TapResultsConverter tapResultsConverter;

    /**
     * Constructor for TapSaveCommand. Called by the dependency injection framework.
     * @param tapResultsConverter A converter that will transform the results into Tap format.
     */
    @Inject
    public TapSaveCommand(final TapResultsConverter tapResultsConverter) {
        this.tapResultsConverter = tapResultsConverter;
    }

    @Override
    public String run(final String... args) {
        if (args.length < 3 || args[2].equalsIgnoreCase("help")) {
            return getHelp();
        }
        final Path path = Paths.get(args[2]);
        try {
            Files.writeString(path, tapResultsConverter.getResults(), StandardOpenOption.CREATE);
            return "Tap file saved as " + path.toAbsolutePath();
        } catch (final ArcticNoResultsException e) {
            return "No results to save";
        } catch (final Exception e) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Unable to save TAP file. Caused by:");
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            sb.append(sw);
            return sb.toString();
        }
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator()
                + "Usage:" + System.lineSeparator()
                + "  tap save FILENAME" + System.lineSeparator()
                + "Parameters:" + System.lineSeparator()
                + "  FILENAME: Destination filename";
    }

    @Override
    public String getDescription() {
        return "Saves the session results to a file";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
