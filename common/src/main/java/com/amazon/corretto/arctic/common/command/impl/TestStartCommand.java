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

import javax.inject.Inject;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.control.TestController;

public final class TestStartCommand extends ArcticCommand {
    public static final String NAME = "cmd";

    public static final String[] COMMAND_LINE = new String[]{"test", "start"};
    private final TestController testController;

    @Inject
    public TestStartCommand(final TestController testController) {
        this.testController = testController;
    }


    @Override
    public String run(final String... args) {
        if (args.length < 4) {
            return "Insufficient arguments." + System.lineSeparator() + getHelp();
        } else {
            boolean async = true;
            if (args.length == 5) {
                async = Boolean.parseBoolean(args[4]);
            }
            if (async) {

                spawnThread(() -> testController.startTestCase(args[2], args[3]));
            } else {
                testController.startTestCase(args[2], args[3]);
            }
            return "Started test case: " + args[2] + ":" + args[3];
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
                "  test run TEST_CLASS TEST_CASE <ASYNC>" + System.lineSeparator() + System.lineSeparator() +
                "Parameters:" + System.lineSeparator() +
                "  TEST_CLASS: test to run" + System.lineSeparator() +
                "  TEST_CASE: test case to run" + System.lineSeparator() +
                "  ASYNC: default true. Do not wait for arctic to run the recording" + System.lineSeparator();
    }

    @Override
    public String getDescription() {
        return "Runs a test case on demand";
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    private void spawnThread(final Runnable r) {
        new Thread(r).start();
    }

}
