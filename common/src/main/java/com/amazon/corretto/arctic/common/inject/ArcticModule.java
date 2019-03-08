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
package com.amazon.corretto.arctic.common.inject;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.common.exception.ArcticConfigurationException;
import com.google.inject.AbstractModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConversionException;

import static com.google.inject.name.Names.named;

/**
 * A base guice module that can easily perform checks or binds on {@link Configuration} objects.
 */
@Slf4j
public abstract class ArcticModule extends AbstractModule {
    private final Configuration config;

    /**
     * Retrieves the current configuration for the module.
     * @return current configuration for the module
     */
    protected Configuration getConfig() {
        return config;
    }

    /**
     * Constructor for the ArcticModule.
     * @param config A configuration to be used by the module.
     */
    public ArcticModule(final Configuration config) {
        this.config = config;
    }

    /**
     * Ensures a key is present in our Configuration repository.
     * @param keyName Name of the key to check.
     * @param values Valid values for the key to report in case of the key missing.
     * @param <T> Type of the values we will report.
     * @throws ArcticConfigurationException if the key is missing from the properties file.
     */
    protected <T> void check(final String keyName, final Collection<T> values) {
        if (!getConfig().containsKey(keyName)) {
            fail(keyName, values);
        }
    }

    /**
     * Ensures a key is present in our Configuration repository.
     * @param keyName Name of the key to check.
     * @param validValues In case of failure, a String with information about the valid values.
     * @throws ArcticConfigurationException if the key is missing from the properties file.
     */
    protected void check(final String keyName, final String validValues) {
        if (!getConfig().containsKey(keyName)) {
            fail(keyName, Collections.singletonList(validValues));
        }
    }

    /**
     * Binds a specific value from the properties. The injection will be annotated with
     * {@link com.google.inject.name.Named} using the key name.
     * @param clazz Class of the value we want to bind.
     * @param key Property we want to bind.
     * @param values In case of failure, valid list of values to list on the error.
     * @param <T> Type of the value.
     * @throws ArcticConfigurationException if the key is missing or not
     */
    protected <T> void bindFromConfig(final Class<T> clazz, final String key, final Collection<T> values) {
        check(key, values);
        try {
            final T value = getConfig().get(clazz, key);
            bind(clazz).annotatedWith(named(key)).toInstance(value);
        } catch (final ConversionException e) {
            log.error("Unable to convert key {}:{} to {}", key, getConfig().getString(key), clazz.getSimpleName(), e);
            fail(key, values);
        }
    }

    /**
     * Binds an specific value from the properties. The injection will be annotated with
     * {@link com.google.inject.name.Named} using the key name.
     * @param clazz Class of the value we want to bind.
     * @param key Property we want to bind.
     * @param validValues In case of failure, a String with information about the valid values.
     * @param <T> Type of the value.
     * @throws ArcticConfigurationException if the key is missing or not
     */
    protected <T> void bindFromConfig(final Class<T> clazz, final String key, final String validValues) {
        check(key, validValues);
        try {
            final T value = getConfig().get(clazz, key);
            bind(clazz).annotatedWith(named(key)).toInstance(value);
        } catch (final ConversionException e) {
            log.error("Unable to convert key {}:{} to {}", key, getConfig().getString(key), clazz.getSimpleName(), e);
            fail(key, Collections.singletonList(validValues));
        }
    }

    /**
     * Used when errors are detected in the configuration, and exception is thrown and the key accessed logged.
     * @param keyName Name of the key we were processing.
     * @param values Valid values for the key.
     * @param <T> Type of the values.
     */
    protected <T> void fail(final String keyName, final Collection<T> values) {
        final Collection<String> stringValues = values.stream().map(Object::toString).collect(Collectors.toList());
        log.error("Invalid configuration value for {}. Valid values are: {}", keyName, String.join(", ", stringValues));
        throw new ArcticConfigurationException(keyName, stringValues);
    }

    /**
     * Used when errors are detected in the configuration, and exception is thrown and the key accessed logged.
     * @param keyName Name of the key we were processing.
     * @param help A hint for the error message
     */
    protected void fail(final String keyName, final String help) {
        log.error("Invalid configuration value for {}. Valid values are: {}", keyName, help);
        throw new ArcticConfigurationException(keyName, Collections.singletonList(help));
    }
}
