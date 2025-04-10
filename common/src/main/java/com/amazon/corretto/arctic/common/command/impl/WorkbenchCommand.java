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
import com.amazon.corretto.arctic.common.gui.WorkbenchManager;
import jakarta.inject.Inject;

public final class WorkbenchCommand extends ArcticCommand {
    public static final String[] COMMAND_LINE = new String[]{"wb"};
    private final WorkbenchManager wbManager;

    @Inject
    public WorkbenchCommand(final WorkbenchManager wbManager) {
        this.wbManager = wbManager;
    }

    @Override
    public String run(final String... args) {
        if (args.length < 2) {
            return getHelp();
        }
        switch (args[1]) {
            case "back":
                wbManager.toBack();
                return "Workbench set to back";
            case "hide":
                wbManager.hide();
                return "Workbench hidden";
            case "show":
                wbManager.show();
                return "Workbench visible";
            default:
                return getHelp();
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
                + String.format("  %s [OPTION]", String.join(" ", COMMAND_LINE))
                + System.lineSeparator() + System.lineSeparator()
                + "Options:" + System.lineSeparator()
                + "  back: Send the workbench to the back" + System.lineSeparator()
                + "  hide: Hide the workbench" + System.lineSeparator()
                + "  show: Show the workbench" + System.lineSeparator();
    }

    @Override
    public String getDescription() {
        return "Manipulate the workbench";
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}
