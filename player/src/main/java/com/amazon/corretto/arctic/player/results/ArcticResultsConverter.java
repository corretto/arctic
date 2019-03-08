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

package com.amazon.corretto.arctic.player.results;

import com.amazon.corretto.arctic.player.exception.ArcticNoResultsException;

/**
 * This represents an interface for any class that is able to convert the test execution results. Examples would be to
 * generate the contents of a tap file or a junit file.
 * @param <T> The type of the results that is returned.
 */
public interface ArcticResultsConverter<T> {
    /**
     * Get the results of the run in the desired format.
     * @return An object of type T that represents the results
     * @throws ArcticNoResultsException If there are no results to return.
     */
    T getResults() throws ArcticNoResultsException;
}
