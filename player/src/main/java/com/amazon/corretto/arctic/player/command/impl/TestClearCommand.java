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

import java.util.Set;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.model.TestId;
import com.amazon.corretto.arctic.common.session.ArcticSessionKeeper;
import jakarta.inject.Inject;

/**
 * A command to clear information about specific test results.
 */
public final class TestClearCommand extends ArcticCommand {
    public static final String[] COMMAND_LINE = new String[]{"test", "clear"};

    private final Set<ArcticSessionKeeper<?, ?>> resultsKeeper;

    /**
     * Creates a new instance.
     * @param resultsKeeper All the different results keeper what might contain test execution related information.
     */
    @Inject
    public TestClearCommand(final Set<ArcticSessionKeeper<?, ?>> resultsKeeper) {
        this.resultsKeeper = resultsKeeper;
    }

    @Override
    public String run(final String... args) {
        if (args.length == 2) {
            resultsKeeper.forEach(ArcticSessionKeeper::clear);
            return "All test results have been cleared";
        } else if (args.length == 3) {
            resultsKeeper.forEach(it -> it.clear(args[2]));
            return "Cleared all test case results for test: " + args[2];
        } else if (args.length >= 4) {
            resultsKeeper.forEach(it -> it.clear(new TestId(args[2], args[3])));
            return "Cleared results for test case: " + args[2] + " " + args[3];
        }
        return "No results cleared" + System.lineSeparator() + getHelp();
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator()
                + "Usage:" + System.lineSeparator()
                + "  test clear [TEST_NAME [TEST_CASE]]" + System.lineSeparator() + System.lineSeparator()
                + "Parameters:" + System.lineSeparator()
                + "  TEST_NAME: Only clear results that match TEST_NAME" + System.lineSeparator()
                + "  TEST_CASE: Only clear results that match TEST_NAME and TEST_CASE" + System.lineSeparator();
    }

    @Override
    public String getDescription() {
        return "Clears test results";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
