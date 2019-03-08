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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminateCommand extends ArcticCommand {
    private static final Logger log = LoggerFactory.getLogger(TerminateCommand.class);
    public static final String[] COMMAND_LINE = new String[]{"terminate"};
    private static final int DEFAULT_DELAY_MS = 500;

    @Override
    public String run(final String... args) {
        final int delay;
        int requestedDelay = -1;
        if (args.length > 1) {
            try {
                requestedDelay = Integer.parseInt(args[1]);
            } catch (Exception e) {
                requestedDelay = DEFAULT_DELAY_MS;
            }
        }
        delay = requestedDelay > 0 ? requestedDelay : DEFAULT_DELAY_MS;
        new Thread(() -> terminate(delay)).start();
        return "Arctic shutting down";
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator()
                + "Usage:" + System.lineSeparator()
                + "  terminate [DELAY]"
                + "Parameters:"
                + String.format("  %-20s%s", "DELAY", "Amount of milliseconds to wait before terminating. Default: " +
                DEFAULT_DELAY_MS);
    }

    @Override
    public String getDescription() {
        return "Ends the running Arctic instance";
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    private void terminate(int delay) {
        log.info("Arctic shutting down");
        try {
            Thread.sleep(delay);
            System.exit(0);
        } catch (Exception e) {
            log.warn("Error while shutting down", e);
            System.exit(-1);
        }

    }
}
