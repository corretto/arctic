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

import javax.inject.Inject;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.player.exception.ArcticNoResultsException;
import com.amazon.corretto.arctic.player.results.impl.XmlResultsConverter;

/**
 * Prints a junit compatible xml report.
 */
public final class XmlPrintCommand extends ArcticCommand {
    public static final String[] COMMAND_LINE = new String[]{"xml", "print"};
    private final XmlResultsConverter xmlResultsConverter;

    /**
     * Creates a new instance. Called by the dependency injection framework.
     * @param xmlResultsConverter A converter from the native test information to xml format.
     */
    @Inject
    public XmlPrintCommand(final XmlResultsConverter xmlResultsConverter) {
        this.xmlResultsConverter = xmlResultsConverter;
    }

    @Override
    public String run(final String... args) {
        try {
            return xmlResultsConverter.getResults();
        } catch (final ArcticNoResultsException e) {
            return "No results to print";
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
                + "  xml print";
    }

    @Override
    public String getDescription() {
        return "Print the xml results to the console";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
