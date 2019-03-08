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

package com.amazon.corretto.arctic.common.model;

import java.util.function.Consumer;

/**
 * A tuple that is used to store values for implementations of
 * {@link com.amazon.corretto.arctic.common.session.ArcticSessionKeeper}.
 * @param <I> Type that is use to identify the tuple value.
 * @param <T> Type of the value we want to store
 */
public final class ArcticResultTuple<I, T> {
    private final I id;
    private long lastUpdated;
    private T value;


    /**
     * Creates an empty tuple for a TestName/TestCase.
     * @param id identification of the result for which the tuple is created
     */
    public ArcticResultTuple(final I id) {
        this.id = id;
    }

    /**
     * Creates a tuple for a TestName/TestCase with an specific value.
     * @param id identification of the result for which the tuple is created
     * @param value Value we want to store in the tuple
     */
    public ArcticResultTuple(final I id, final T value) {
        this(id);
        setValue(value);
    }

    /**
     * Replaces the value stored in the tuple with a new value.
     * @param value Value we want to store in the tuple
     */
    public void setValue(final T value) {
        this.value = value;
        lastUpdated = System.currentTimeMillis();
    }

    /**
     * Updates the value object stored in the tuple.
     * @param updater A consumer of the update that will modify it. For replacing the object, use
     * {@link this#setValue(Object)}
     */
    public void updateValue(final Consumer<T> updater) {
        if (value != null) {
            updater.accept(value);
            lastUpdated = System.currentTimeMillis();
        }
    }

    /**
     * Retrieves the test name for which the tuple was created.
     * @return Id for which the tuple was created
     */
    public I getId() {
        return id;
    }

    /**
     * Retrieves the last time in currentMillis the value stored in the tuple was modified. This value is set every time
     * {@link this#setValue(Object)} or {@link this#updateValue(Consumer)} is executed.
     * @return Last time the value of the tuple was set/updated. 0 for an empty tuple.
     */
    public long getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Returns the value object stored in the tuple.
     * @return The value object stored in the tuple.
     */
    public T getValue() {
        return value;
    }
}
