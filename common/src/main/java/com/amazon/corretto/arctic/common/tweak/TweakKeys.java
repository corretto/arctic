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

/**
 * A list of the different tweak keys that are used to change arctic behavior without restarting. These keys may
 * represent the same configuration options as the properties file or be independent ones.
 */
public final class TweakKeys {

    /**
     * Disable instantiation of the class.
     */
    private TweakKeys() {
        // intentionally left blank
    }

    /**
     * A prefix that is used with every key to ensure the keys are unique (although the prefix is hidden from the user).
     */
    public static final String PREFIX = "arctic.tweak.";

    /**
     * A tweak that enables safe mode. This changes the reproduction mode and tries to ensure the timeline during
     * the playback is synced with the recording timeline.
     */
    public static final String SAFE = PREFIX + "safe";

}
