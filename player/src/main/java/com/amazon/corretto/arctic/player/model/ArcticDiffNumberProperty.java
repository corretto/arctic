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

import java.math.RoundingMode;
import java.text.DecimalFormat;

import com.amazon.corretto.arctic.player.backend.pixel.PixelCheck;

/**
 * Represents a numeric property of an image.
 * @param <T> The type of Number used for the metric.
 */
public final class ArcticDiffNumberProperty<T extends Number> extends ArcticDiffProperty<T> {
    private final String unit;
    private static final DecimalFormat DF = new DecimalFormat("0.0000");
    static {
        DF.setRoundingMode(RoundingMode.DOWN);
    }

    /**
     * Creates a new numeric property.
     * @param type PixelCheck type this property is related to.
     * @param name Name of the property.
     * @param value Value of the property.
     * @param unit Unit of the property.
     */
    public ArcticDiffNumberProperty(final PixelCheck.Type type, final String name, final T value, final String unit) {
        super(type, name, value);
        this.unit = unit;
    }

    @Override
    public String toString() {
        if (getValue() instanceof Float) {
            float floatValue = (float) getValue();
            return String.format("%s.%s: %s%s", getType().getName(), getName(), DF.format(floatValue), unit);
        } else if (getValue() instanceof Double) {
            double doubleValue = (double) getValue();
            return String.format("%s.%s: %s%s", getType().getName(), getName(), DF.format(doubleValue), unit);
        } else {
            return String.format("%s.%s: %s%s", getType().getName(), getName(), getValue(), unit);
        }
    }
}
