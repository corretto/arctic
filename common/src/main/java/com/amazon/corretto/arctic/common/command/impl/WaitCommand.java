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

public final class WaitCommand extends ArcticCommand {
    public static final String[] COMMAND_LINE = new String[]{"wait"};
    @Override
    public String run(final String... args) {
        if (args.length >= 2) {
            try {
                final int amount = Integer.parseInt(args[1]);
                Thread.sleep(amount);
                return "Waited for + " + amount + " milliseconds";
            } catch (final InterruptedException ie) {
                return "Wait interrupted!";
            } catch (final NumberFormatException nfe) {
                return "Incapable of waiting for: " + args[1] + System.lineSeparator() + getHelp();
            }
        }
        return getHelp();
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator()
                + "Usage:" + System.lineSeparator()
                + String.format("  %s AMOUNT", String.join(" ", COMMAND_LINE))
                + System.lineSeparator() + System.lineSeparator()
                + "Parameters:" + System.lineSeparator()
                + String.format("  %-20s%s", "AMOUNT", "Time to wait in ms");
    }

    @Override
    public String getDescription() {
        return "Wait for some time";
    }

    @Override
    public boolean isLocal() {
        return true;
    }
}
