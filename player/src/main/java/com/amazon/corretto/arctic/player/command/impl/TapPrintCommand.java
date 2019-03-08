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
import com.amazon.corretto.arctic.player.results.impl.TapResultsConverter;

/**
 * A command that prints the test results in tap format to the console.
 */
public final class TapPrintCommand extends ArcticCommand {
    public static final String[] COMMAND_LINE = new String[]{"tap", "print"};
    private final TapResultsConverter tapResultsConverter;

    /**
     * Constructor for TapPrintCommand. Called by the dependency injection framework.
     * @param tapResultsConverter A converter that will transform the results into Tap format.
     */
    @Inject
    public TapPrintCommand(final TapResultsConverter tapResultsConverter) {
        this.tapResultsConverter = tapResultsConverter;
    }

    @Override
    public String run(final String... args) {
        try {
            return tapResultsConverter.getResults();
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
                + "  tap print";
    }

    @Override
    public String getDescription() {
        return "Print the session results to the console";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
