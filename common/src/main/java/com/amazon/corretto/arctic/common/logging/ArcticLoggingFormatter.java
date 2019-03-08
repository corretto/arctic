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

package com.amazon.corretto.arctic.common.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * This class ensures the formatting of the arctic logging (used to print to the console) is kept after the migration
 * from log4j2 to java.util.logging.
 *
 * This is heavily based on the SimpleFormatter, but to ensure we produce the same output format we change the name of
 * the levels to keep Slf4j names and we also change the logger name so we use the shortened format, like j.u.l.Logger.
 */
public final class ArcticLoggingFormatter extends Formatter {
    private final String format = LogManager.getLogManager()
            .getProperty(ArcticLoggingFormatter.class.getCanonicalName() + ".format");

    private final Map<String, String> deflatedLoggers = new HashMap<>();

    /**
     * Formats a log record. This works very similar as the format method for the SimpleFormatter of JUL, with two
     * differences.
     * Level names are based on Slf4j names, and logger name are reduce to only use the initials of the package.
     * @param record The record to format.
     * @return A string with the proper formatting applied, ready to be logged.
     */
    @Override
    public String format(final LogRecord record) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(
                record.getInstant(), ZoneId.systemDefault());
        String source;
        if (record.getSourceClassName() != null) {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null) {
                source += " " + record.getSourceMethodName();
            }
        } else {
            source = record.getLoggerName();
        }
        String message = formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }
        return String.format(format,
                zdt,
                source,
                deflateLoggerName(record.getLoggerName()),
                LevelMappings.JUL_SLF4J.get(record.getLevel()),
                message,
                throwable);
    }

    /**
     * We are going to have plenty of logging from a very small number of loggers. The conversion of the logger into
     * the smaller format is cached in a HashMap.
     * @param loggerName Name of the logger as received from JUL.
     * @return Deflated version, keeping only the initials for the package.
     */
    private String deflateLoggerName(final String loggerName) {
        if (!deflatedLoggers.containsKey(loggerName)) {
            String className = loggerName.split(" ")[0];
            String[] tokens = className.split("\\.");
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < tokens.length - 1; i++) {
                out.append(tokens[i].charAt(0)).append('.');
            }
            out.append(tokens[tokens.length - 1]);
            deflatedLoggers.put(loggerName, out.toString());
        }
        return deflatedLoggers.get(loggerName);
    }
}
