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

package com.amazon.corretto.arctic.player.model;

import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;

/**
 * Generic interface to represent a property of an image check during review.
 * @param <T> The type of the property.
 */
public abstract class ArcticDiffProperty<T> {
    private final PixelCheck.Type type;
    private final String name;
    private final T value;

    /**
     * Constructor for ArcticDiffProperty. This is an abstract class.
     * @param type PixelCheck type this property is related to.
     * @param name Name of the property.
     * @param value Value of the property.
     */
    public ArcticDiffProperty(final PixelCheck.Type type, final String name, final T value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    /**
     * Get the key that represents this property, a combination of the type and name.
     * @return The key of the property.
     */
    public String getKey() {
        return keyOf(type, name);
    }

    /**
     * Get the name of the property.
     * @return Name of the property.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the type of the property.
     * @return Type of the property.
     */
    public PixelCheck.Type getType() {
        return type;
    }

    /**
     * Get the value stored in the property.
     * @return The value of the property.
     */
    public T getValue() {
        return value;
    }

    /**
     * Generates the property key for a specific type and name.
     * @param type Type of the property.
     * @param name Name of the property.
     * @return A string that represents the key of the property.
     */
    public static String keyOf(final PixelCheck.Type type, final String name) {
        return String.format("%s.%s", type, name);
    }
}
