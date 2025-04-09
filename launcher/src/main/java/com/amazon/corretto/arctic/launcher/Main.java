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

package com.amazon.corretto.arctic.launcher;

import com.amazon.corretto.arctic.common.BaseMain;
import com.amazon.corretto.arctic.common.logging.LogController;

/**
 * A common launcher for the different arctic modes, it is the entry point for the Arctic-x.y.z.jar.
 */
public class Main extends BaseMain {
    static {
        LogController.init();
    }

    private static final String USAGE = "Perform an Arctic operation.\n"
            + "Usage:\n";

    /**
     * Parses the command line arguments and starts the selected task.
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        switch (CommandLineOption.parseArgs(args)) {
            case RECORDER:
                com.amazon.corretto.arctic.recorder.Main.main(args);
                break;
            case PLAYER:
                com.amazon.corretto.arctic.player.Main.main(args);
                break;
            case COMMAND:
                com.amazon.corretto.arctic.cmd.client.Main.main(CommandLineOption.COMMAND, args);
                break;
            case INTERACTIVE:
                com.amazon.corretto.arctic.cmd.client.Main.main(CommandLineOption.INTERACTIVE, args);
                break;
            case DUMPER:
                com.amazon.corretto.arctic.keycode.Dumper.main(args);
                break;
            case KEYTEST:
                com.amazon.corretto.arctic.keycode.KeyTest.main(args);
            case VERSION:
                printVersion();
                break;
            default:
                System.out.println("Unrecognized option");
            case HELP:
                printHelp();
        }
    }

    private static void printHelp() {
        System.out.print(USAGE);
        BaseMain.CommandLineOption.OPTIONS.forEach(System.out::println);
    }

    private static void printVersion() {
        String title = Main.class.getPackage().getImplementationTitle();
        String version = Main.class.getPackage().getImplementationVersion();
        String vendor = Main.class.getPackage().getImplementationVendor();
        System.out.println(title + " by " + vendor);
        System.out.println("Version: " + version);
    }
}
