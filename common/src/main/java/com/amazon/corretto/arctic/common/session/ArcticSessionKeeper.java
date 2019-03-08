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

package com.amazon.corretto.arctic.common.session;

import java.util.Collection;
import java.util.function.Consumer;

import com.amazon.corretto.arctic.common.model.ArcticResultTuple;
import com.amazon.corretto.arctic.common.model.TestId;


/**
 * Basic interface for an Arctic object that stores information about the running session, like test results or
 * ScreenCheck comparisons.
 * @param <I> Type of the id used to identified the information.
 * @param <T> Type of information stored.
 */
public interface ArcticSessionKeeper<I, T> {
    /**
     * A unique for the SessionKeeper used for identification and serialization.
     * @return Name of the SessionKeeper
     */
    String getName();

    /**
     * Whether an specific session object contains data.
     * @return true if the SessionKeeper holds data
     */
    boolean hasData();

    /**
     * Returns a collection with all the failures. There is no guarantee regarding the order.
     * @return A collection with all the failures
     */
    Collection<ArcticResultTuple<I, T>> getResults();

    /**
     * Retries a specific tuple based on its id.
     * @param id Id of the tuple to retrieve.
     * @return The tuple matching that id. Can be null.
     */
    ArcticResultTuple<I, T> getResult(I id);

    /**
     * Removes all the stored failures.
     */
    void clear();

    /**
     * Removes all the failures associated with a specific testName.
     * @param testName Name of the test to clear the results
     */
    void clear(String testName);

    /**
     * Removes all the failures associated with a specific testName.
     * @param testId Id of the test to clear the results
     */
    void clear(TestId testId);

    /**
     * Removes all the failures associated with a specific testId.
     * @param testId Identification of the test to clear
     */
    void clear(I testId);

    /**
     * Adds or replaces the value stored for an entry with a new value.
     * @param id Identification of the test to add the value
     * @param value New value we want to store
     */
    void addValue(I id, T value);

    /**
     * Modifies the existing entry for one value.
     * @param id Identification of the test to replace the value
     * @param value Operation we want to do over the value
     */
    void updateValue(I id, Consumer<T> value);

    /**
     * Returns a session object that can be serialized to persist information across Arctic sessions.
     * @return Contents of the session kept by this SessionKeeper
     */
    SessionObject getSession();

    /**
     * Restore a session from a session object.
     * @param sessionObject Session to restore
     * @param <S> Type of the session object, should match the one this class uses
     */
    <S extends SessionObject> void restoreSession(S sessionObject);

    /**
     * Returns the class of the SessionObject this keeper works with.
     * @return Class of the SessionObject this keeper works with
     */
    Class<? extends SessionObject>  getSessionObjectClass();

    /**
     * A common interface for SessionObjects. This interface does not define any method, but it is used to identify
     * serializable session objects.
     */
    interface SessionObject {
    }
}
