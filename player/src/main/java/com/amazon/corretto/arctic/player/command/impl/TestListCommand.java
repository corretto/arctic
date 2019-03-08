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

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.util.Pair;
import com.amazon.corretto.arctic.player.results.ArcticTestResultsKeeper;

/**
 * ArcticCommand to print tests results.
 */
public final class TestListCommand extends ArcticCommand {
    public static final String[] COMMAND_LINE = new String[]{"test", "list"};

    private final ArcticTestResultsKeeper resultsKeeper;

    /**
     * Creates a new TestListCommand.
     * @param resultsKeeper Results keeper used to query the results from
     */
    @Inject
    public TestListCommand(final ArcticTestResultsKeeper resultsKeeper) {
        this.resultsKeeper = resultsKeeper;
    }

    @Override
    public String run(final String... args) {
        switch (args.length) {
            case 2:
                return getResults(null);
            case 3:
                return getResults(args[2]);
            default:
                return getHelp();
        }
    }

    private String getResults(@Nullable final String regex) {
        final Pattern p = regex == null ? null : Pattern.compile(regex);

        final String result = resultsKeeper.getResults().stream()
                .map(it -> Pair.of(it.getId(), it.getValue()))
                .filter(it -> p == null || p.matcher(it.getLeft().toString()).matches())
                .map(it -> String.format("%-50s%s", it.getLeft(), it.getRight()))
                .collect(Collectors.joining(System.lineSeparator()));

        if (result.isBlank()) {
            return "No result data found";
        }

        return result;
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator()
                + "Usage:" + System.lineSeparator()
                + "  test list [REGEX]" + System.lineSeparator() + System.lineSeparator()
                + "Parameters:" + System.lineSeparator()
                + "  REGEX: Display only the tests that match the regular expression" + System.lineSeparator();
    }

    @Override
    public String getDescription() {
        return "Prints test results";
    }

    /**
     * TestListCommand is not a local command.
     * @return Always false
     */
    @Override
    public boolean isLocal() {
        return false;
    }
}
