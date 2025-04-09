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
import javax.inject.Singleton;

public final class TestFinishCommand extends ArcticCommand {
    public static final String NAME = "command";

    public static final String[] COMMAND_LINE = new String[]{"test", "finish"};
    private final TestController testController;

    @Inject
    public TestFinishCommand(final TestController testController) {
        this.testController = testController;
    }

    @Override
    public String run(final String... args) {
        if (args.length < 5) {
            return "Insufficient arguments." + System.lineSeparator() + getHelp();
        } else {
            boolean result;
            boolean async = false;
            if (args[4].equals("code")) {
                if (args.length < 6) {
                    return "Insufficient arguments." + System.lineSeparator() + getHelp();
                }
                result = args[5].equals("0");
                if (args.length == 7) {
                    async = Boolean.parseBoolean(args[6]);
                }
            } else {
                result = Boolean.parseBoolean(args[4]);
                if (args.length == 6) {
                    async = Boolean.parseBoolean(args[5]);
                }
            }
            if (async) {
                spawnThread(() -> testController.finishTestCase(args[2], args[3], result));
            } else {
                testController.finishTestCase(args[2], args[3], result);
            }
            return "Finished test case: " + args[2] + ":" + args[3] + " with result " + result;
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
            "  test finish TEST_CLASS TEST_CASE RESULT <CODE> <ASYNC>" + System.lineSeparator() + System.lineSeparator() +
            "Parameters:" + System.lineSeparator() +
            "  TEST_CLASS: test to run" + System.lineSeparator() +
            "  TEST_CASE: test case to run" + System.lineSeparator() +
            "  RESULT: true or false, whether the test succeeded. If 'code' is sent, read a return code in <CODE>" + System.lineSeparator() +
            "  CODE: a return code for the test. 0 is interpreted as success, anything else as failure" + System.lineSeparator() +
            "  ASYNC: default false. Do not wait for arctic to finish processing" + System.lineSeparator();
    }

    @Override
    public String getDescription() {
        return "Finishes a test case";
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    private void spawnThread(final Runnable r) {
        new Thread(r).start();
    }
}
