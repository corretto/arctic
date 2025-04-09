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
package com.amazon.corretto.arctic.common.exception;

import java.util.Collection;

import com.amazon.corretto.arctic.api.exception.ArcticException;

/**
 * An exception to represent problems when reading configuration from the properties file.
 */
public class ArcticConfigurationException extends ArcticException {
    private static final long serialVersionUID = 6747660312461450443L;

    /**
     * Creates a new ArcticConfigurationException for a specific key and a list of valid values.
     * @param keyName Name of the configuration key that caused the issue
     * @param validValues List of potential valid values for that key
     */
    public ArcticConfigurationException(final String keyName, final Collection<String> validValues) {
        super("Invalid value for key " + keyName + ". Valid values are " + String.join(", ", validValues));
    }

    /**
     * Creates a new ArcticConfigurationException for a specific key.
     * @param keyName Name of the configuration key that caused the issue
     * @param message An error message
     */
    public ArcticConfigurationException(final String keyName, final String message) {
        super("Invalid value for key " + keyName + ". " + message);
    }

    /**
     * Creates a new ArcticConfigurationException for a specific key.
     * @param keyName Name of the configuration key that caused the issue.
     * @param message An error message.
     * @param cause Nested exception.
     */
    public ArcticConfigurationException(final String keyName, final String message, final Throwable cause) {
        super("Invalid value for key " + keyName + ". " + message, cause);
    }
}
