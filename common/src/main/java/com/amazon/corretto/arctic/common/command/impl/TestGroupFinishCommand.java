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

import javax.inject.Inject;

public final class TestGroupFinishCommand extends ArcticCommand {
    public static final String[] COMMAND_LINE = new String[]{"test", "group", "finish"};
    private final TestController testController;

    @Inject
    public TestGroupFinishCommand(final TestController testController) {
        this.testController = testController;
    }


    @Override
    public String run(final String... args) {
        if (args.length < 4) {
            return "Insufficient arguments." + System.lineSeparator() + getHelp();
        } else {
            boolean result = true;
            if (args.length > 4) {
                result = Boolean.parseBoolean(args[4]);
            }
            testController.finishTestGroup(args[3], result);
            return "Finished test group " + args[3] + " with result: " + result;
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
                "  test group finish TEST_GROUP <RESULT>" + System.lineSeparator() + System.lineSeparator() +
                "Parameters:" + System.lineSeparator() +
                "  TEST_GROUP: test group that has finished" + System.lineSeparator() +
                "  RESULT:     optional. A boolean indicating the group result" + System.lineSeparator();
    }

    @Override
    public String getDescription() {
        return "Notifies Arctic a test group has finished";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
