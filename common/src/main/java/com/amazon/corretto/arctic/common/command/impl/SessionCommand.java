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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.inject.CommonInjectionKeys;
import com.amazon.corretto.arctic.common.repository.TestRepository;
import com.amazon.corretto.arctic.common.session.ArcticSessionKeeper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command to persist and restore Arctic sessions. This allows for runtime information like tests results and failures
 * to be stored and opened later. This can also be used to store arctic sessions that were generated in one machine and
 * open them in a different machine to perform a review of the failures.
 */
public final class SessionCommand extends ArcticCommand {
    private static final Logger log = LoggerFactory.getLogger(SessionCommand.class);
    private static final Type GSON_TYPE = new TypeToken<Map<String, ArcticSessionKeeper.SessionObject>>(){}.getType();
    public static final String BASIC_SESSION_INFO = "sessionInfo";
    public static final String[] COMMAND_LINE = new String[]{"session"};
    public static final String SESSION_GSON = "sessionGson";

    private final Map<String, ArcticSessionKeeper<?, ?>> sessionKeepers;
    private final Gson gson;
    private final String defaultSessionFilename;
    private final String scope;
    private final TestRepository.Mode scopeMode;

    /**
     * Creates a new instance of SessionCommand. Called by the DI injector.
     * @param sessionKeepers All the different instances in Arctic storing session information
     * @param gson A version of gson configured to save and restore sessions. It needs to be able to deserialize
     *             polymorphic lists of SessionObjects
     * @param defaultSessionFilename Default name of the session file
     * @param scope The scope Arctic is running on.
     * @param scopeMode The scope mode Arctic is currently running on.
     */
    @Inject
    public SessionCommand(final Set<ArcticSessionKeeper<?, ?>> sessionKeepers, @Named(SESSION_GSON) final Gson gson,
                          @Named(CommonInjectionKeys.SESSION_DEFAULT) final String defaultSessionFilename,
                          @Named(CommonInjectionKeys.REPOSITORY_SCOPE) final String scope,
                          final TestRepository.Mode scopeMode) {
        this.sessionKeepers = sessionKeepers.stream()
                .collect(Collectors.toMap(it -> it.getClass().getName(), Function.identity()));
        this.gson = gson;
        this.defaultSessionFilename = defaultSessionFilename;
        this.scope = scope;
        this.scopeMode = scopeMode;
    }

    /**
     * Executes the command. There are four modes of execution:
     * - save [FILENAME]     stores the current session in FILENAME or the defaultSessionFilename if omitted
     * - load [FILENAME]     loads the session stored on FILENAME or the defaultSessionFilename if omitted
     * - print               prints the json file representing the current session
     * - clear               removes all session information in the current running instance of Arctic
     * @param args Exact arguments used to call the ArcticCommand
     * @return Result of the execution
     */
    public String run(final String... args) {
        if (args.length < 2) {
            return getHelp();
        }

        switch (args[1]) {
            case "save":
                return saveSession(args);
            case "restore":
                return loadSession(args);
            case "print":
                return getSessionString();
            case "clear":
                sessionKeepers.values().forEach(ArcticSessionKeeper::clear);
                return "All test results have been cleared";
            default:
                return getHelp();
        }
    }

    private String loadSession(final String[] args) {
        boolean force = Arrays.stream(args).anyMatch("force"::equalsIgnoreCase);
        if (sessionKeepers.values().stream().anyMatch(ArcticSessionKeeper::hasData)) {
            if (force) {
                log.debug("Wiping current session to restore previous one");
                sessionKeepers.values().forEach(ArcticSessionKeeper::clear);
            } else {
                return "Unable to restore session as there is already session data stored";
            }
        }
        final Path sessionName = Path.of(defaultSessionFilename);
        try (Reader reader = new FileReader(sessionName.toFile())) {
            final Map<String, ArcticSessionKeeper.SessionObject> session = gson.fromJson(reader, GSON_TYPE);
            BasicSessionObject basicSessionObject = (BasicSessionObject) session.get(BASIC_SESSION_INFO);
            if (basicSessionObject == null) {
                if (!force) {
                   return "Unable to load session with no Session Info";
                }
                log.warn("Loading session with no Session Info");
            }
            if (validateSession(basicSessionObject, force)) {
                session.forEach((k, v) -> {
                    if (sessionKeepers.containsKey(k)) {
                        sessionKeepers.get(k).restoreSession(v);
                    }
                });
                return "Session " + sessionName + " restored";
            }
            return "Session " + sessionName + " could not be loaded";
        } catch (final IOException e) {
            log.error("Unable to load session named: {}", sessionName, e);
            return "Unable to load session named: " + sessionName + ". Error: " + e.getMessage();
        }
    }

    private boolean validateSession(final BasicSessionObject info, final boolean force) {
        String currentVersion = SessionCommand.class.getPackage().getImplementationVersion();
        if (info == null) {
            log.warn("Loading a session without session info");
            return force;
        }
        if (!SessionCommand.class.getPackage().getImplementationVersion().equals(info.getVersion())) {
            log.warn("Loading session generated with {} into {}", info.getVersion(), currentVersion);
        }
        if (!scope.equalsIgnoreCase(info.getScope())) {
            log.warn("Loading session with scope {} into {}", info.getScope(), scope);
        }
        if (!scopeMode.toString().equalsIgnoreCase(info.getScopeMode())) {
            log.warn("Loading session with scope mode {} into {}", info.getScopeMode(), scopeMode);
        }
        return true;
    }

    private String saveSession(final String[] args) {
        if (sessionKeepers.values().stream().noneMatch(ArcticSessionKeeper::hasData)) {
            return "Unable to save session as there is no session data stored";
        }
        final Path sessionName = Path.of(args.length > 2 ? args[2] : defaultSessionFilename);
        try (Writer writer = new FileWriter(sessionName.toFile())) {
            gson.toJson(getSession(), GSON_TYPE, writer);
        } catch (final IOException e) {
            log.error("Unable to save session in path: {}", sessionName, e);
            return "Unable to save session in path: " + sessionName + ". Error: " + e.getMessage();
        }
        return "Session saved as " + sessionName;
    }

    private Map<String, ArcticSessionKeeper.SessionObject> getSession() {
        Map<String, ArcticSessionKeeper.SessionObject> session = new LinkedHashMap<>();
        session.put(BASIC_SESSION_INFO, new BasicSessionObject(scope, scopeMode));
        sessionKeepers.forEach((k, v) -> session.put(k, v.getSession()));
        return session;
    }

    private String getSessionString() {
        return gson.toJson(getSession(), new TypeToken<Map<String, ArcticSessionKeeper.SessionObject>>(){}.getType());
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator()
                + "Usage:" + System.lineSeparator()
                + String.format("  %s [SUBCOMMAND]", String.join(" ", COMMAND_LINE))
                + System.lineSeparator() + System.lineSeparator()
                + "SUBCOMMAND:" + System.lineSeparator()
                + String.format("  %-20s%s", "save", "Persist the current session") + System.lineSeparator()
                + String.format("  %-20s%s", "restore", "Restore the last persisted session") + System.lineSeparator()
                + String.format("  %-20s%s", "print", "Print session data") + System.lineSeparator()
                + String.format("  %-20s%s", "clear", "Clear session data") + System.lineSeparator();
    }

    @Override
    public String getDescription() {
        return "Last failed screen check operations";
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    /**
     * SessionObject that is directly injected by the SessionCommand. It persists some general information, like the
     * scope that arctic was running on.
     */
    public static final class BasicSessionObject implements ArcticSessionKeeper.SessionObject {


        private String scope;
        private String scopeMode;
        private long date;
        private String version;

        /**
         * Creates a new SessionObject that will be used to persist basic session information.
         *
         * @param scope
         * @param scopeMode
         */
        public BasicSessionObject(final String scope, final TestRepository.Mode scopeMode) {
            this.scope = scope;
            this.scopeMode = scopeMode.toString();
            this.date = System.currentTimeMillis();
            this.version = SessionCommand.class.getPackage().getImplementationVersion();
        }

        /**
         * Empty constructor to use during deserialization.
         */
        public BasicSessionObject() {

        }

        /**
         * Retrieve the scope Arctic was running when the session was saved.
         * @return Scope Arctic was running.
         */
        public String getScope() {
            return scope;
        }

        /**
         * Retrieve the scope mode Arctic was running when the session was saved.
         * @return Scope mode Arctic was running
         */
        public String getScopeMode() {
            return scopeMode;
        }

        /**
         * Retrieve the date (as millis) when the session was saved.
         * @return When the session was saved in millis.
         */
        public long getDate() {
            return date;
        }

        /**
         * Retrieve the version of Arctic that generated the session.
         * @return Version of arctic used to generate the session.
         */
        public String getVersion() {
            return version;
        }
    }
}
