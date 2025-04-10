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

package com.amazon.corretto.arctic.common.command.impl;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.control.TestController;
import jakarta.inject.Inject;

public final class TestGroupStartCommand extends ArcticCommand {
    public static final String[] COMMAND_LINE = new String[]{"test", "group", "start"};
    private final TestController testController;

    @Inject
    public TestGroupStartCommand(final TestController testController) {
        this.testController = testController;
    }


    @Override
    public String run(final String... args) {
        if (args.length < 4) {
            return "Insufficient arguments." + System.lineSeparator() + getHelp();
        } else {
            testController.startTestGroup(args[3]);
            return "Starting test group " + args[3];
        }
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator() +
                "Usage:" + System.lineSeparator() +
                "  test group start TEST_GROUP" + System.lineSeparator() + System.lineSeparator() +
                "Parameters:" + System.lineSeparator() +
                "  TEST_GROUP: test group that starts" + System.lineSeparator();
    }

    @Override
    public String getDescription() {
        return "Notifies Arctic the start of a new test group";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
