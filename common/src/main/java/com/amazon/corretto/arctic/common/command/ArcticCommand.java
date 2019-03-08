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

package com.amazon.corretto.arctic.common.command;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/**
 * This is the base class for the different Arctic command line commands. It ensures the different commands have some
 * consistency. It builds a tree with a word being each of the nodes. The leaf node that matches more words for the
 * input string will be the one that receives the execution. This allows to have commands split into different
 * subcommands. This helps keep the different commands relatively small, as they don't need to parse many options.
 */
public abstract class ArcticCommand {
    private CommandTreeNode registeredNode;

    /**
     * Main entry point for the command, called when this specific command has been invoked.
     * @param args The command line that was introduced by the user. This includes the command registered words
     * @return A String that will be presented to the user
     */
    public abstract String run(String... args);

    /**
     * Defines the set of words that identify this command in the tree. Whenever the user introduces those words as a
     * commands, the run method of this command will be executed
     * @return An array of Strings, one for each word. Should have at least one word and the combination should be
     * unique across all commands
     */
    public abstract String[] getCommandLine();

    /**
     * The help for the command. This will be printed whenever the user calls `help COMMAND`.
     * @return A String representing the help
     */
    public abstract String getHelp();

    /**
     * A short description for the command, ideally one line. This is used whenever the user asks for help for an
     * expression that covers multiple commands.
     * @return A description of the commands
     */
    public abstract String getDescription();

    /**
     * Arctic allows to execute commands via RMI. When that is happening, we might have commands we want to run locally
     * or commands we want to run in a different process we are connected to, this flag identifies which one will be
     * executing the specific command. The best example is `quit` and `terminate`. The first one is local, and will
     * finish the current interactive session, while the second one is not, and will end the one we are connected to.
     * @return true to indicate the command should be run locally and not sent to the server
     */
    public abstract boolean isLocal();

    /**
     * The node of the command tree on which this specific node is registered.
     * @return Node where this command is registered.
     */
    protected CommandTreeNode getRegisteredNode() {
        return registeredNode;
    }

    /**
     * Indicates the command in which node it has been registered to. This is useful because some commands may want to
     * be aware of the different subcommands that are registered into children of its own node. This method is invoked
     * byt the CommandTreeNode the command gets registered into when we call
     * {@link CommandTreeNode#registerCommand(ArcticCommand, int)}
     * @param node The node into which the command was registered
     * @return True if the command successfully gets registered. This method might be overriden by specific commands,
     * they may end up rejecting being registered.
     */
    public boolean registerInto(final CommandTreeNode node) {
        this.registeredNode = node;
        return true;
    }

    /**
     * This class is the node used for the tree that stores all the different commands. It implements
     */
    public static final class CommandTreeNode implements Iterable<ArcticCommand> {
        private ArcticCommand command;
        private final Map<String, CommandTreeNode> children = new HashMap<>();

        /**
         * Registers a command under this node, which could be in this specific node or in one of its children. Children
         * will be created as needed to allocate the command.
         * @param toRegister Command we are registering.
         * @param position The depth of the tree on which we are right now, as this method uses recursion.
         */
        public void registerCommand(final ArcticCommand toRegister, final int position) {
            if (position < 0 || position == toRegister.getCommandLine().length) {
                if (toRegister.registerInto(this)) {
                    this.command = toRegister;
                }
            } else {
                children.computeIfAbsent(toRegister.getCommandLine()[position], k -> new CommandTreeNode())
                        .registerCommand(toRegister, position + 1);
            }
        }

        /**
         * Retrieves the command that best fits the commandLine that was invoked. If no command is found, the command,
         * if any, registered in this node is returned. If this method were to return null, it is a way of saying our
         * parent should be returned.
         * @param commandLine The command line that was invoked by the user
         * @param position Depth of the tree on which we are looking at this step
         * @return The best command that suits the command line. Null if none matches and no command is registered in
         * the current node.
         */
        public ArcticCommand getCommand(final String[] commandLine, final int position) {
            if (position >= commandLine.length) {
                return command;
            }
            return Optional.ofNullable(children.getOrDefault(commandLine[position], null))
                    .map(it -> it.getCommand(commandLine, position + 1))
                    .orElse(position == 0 ? null : command);
        }

        /**
         * An iterator that will go through all the commands that are registered under this node. This not include the
         * command (if any) registered in this specific node, only the children.
         * @return And Iterator of al the ArcticCommand registered in children nodes.
         */
        @Nonnull
        @Override
        public Iterator<ArcticCommand> iterator() {
            return getChildren()
                    .distinct()
                    .sorted(Comparator.comparing(it -> String.join(" ", it.getCommandLine())))
                    .iterator();
        }

        private Stream<ArcticCommand> getChildren() {
            final Stream<ArcticCommand> directChildren = children.values().stream()
                    .filter(it -> it.command != null)
                    .map(it -> it.command);
            final Stream<ArcticCommand> indirectChildren = children.values().stream()
                    .filter(it -> it.command == null)
                    .flatMap(CommandTreeNode::getChildren);
            return Stream.concat(directChildren, indirectChildren);
        }
    }
}
