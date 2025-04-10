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

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.player.exception.ArcticNoResultsException;
import com.amazon.corretto.arctic.player.results.impl.XmlResultsConverter;
import jakarta.inject.Inject;

/**
 * Saves a test report compatible with junit xml file format.
 */
public final class XmlSaveCommand extends ArcticCommand {

    public static final String[] COMMAND_LINE = new String[]{"xml", "save"};
    private final XmlResultsConverter xmlResultsConverter;

    /**
     * Creates a new instance. Called by the dependency injection framework.
     * @param xmlResultsConverter A converter from the native test information to xml format.
     */
    @Inject
    public XmlSaveCommand(final XmlResultsConverter xmlResultsConverter) {
        this.xmlResultsConverter = xmlResultsConverter;
    }

    @Override
    public String run(final String... args) {
        if (args.length < 3 || args[2].equalsIgnoreCase("help")) {
            return getHelp();
        }
        final Path path = Paths.get(args[2]);
        try {
            Files.writeString(path, xmlResultsConverter.getResults(), StandardOpenOption.CREATE);
            return "Xml file saved as " + path.toAbsolutePath().toString();
        } catch (final ArcticNoResultsException e) {
            return "No results to save";
        } catch (final Exception e) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Unable to save xml file. Caused by:");
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
                + "  xml save FILENAME" + System.lineSeparator()
                + "Parameters:" + System.lineSeparator()
                + "  FILENAME: Destination filename";
    }

    @Override
    public String getDescription() {
        return "Saves the test results as a junit xml file";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
