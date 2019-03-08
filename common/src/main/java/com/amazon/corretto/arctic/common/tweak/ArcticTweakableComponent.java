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

package com.amazon.corretto.arctic.common.tweak;

import java.util.Set;

/**
 * Defines an interface that different components can implement to allow the set of different tweaks without the need
 * to restart arctic.
 */
public interface ArcticTweakableComponent {
    /**
     * Set a specific tweak for the component.
     * @param key tweak to set.
     * @param value value of the tweak to set.
     */
    void setTweak(String key, String value);

    /**
     * Returns all the tweak keys relevant for the component.
     * @return set with all the tweak keys.
     */
    Set<String> getTweakKeys();

    /**
     * Returns a small description of how a specific tweak key alters the behavior of this component.
     * @param key tweak to query about.
     * @return description of the tweak.
     */
    String getTweakKeyDescription(String key);
}
