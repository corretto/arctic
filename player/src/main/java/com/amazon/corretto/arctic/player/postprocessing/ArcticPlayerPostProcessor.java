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

package com.amazon.corretto.arctic.player.postprocessing;

import java.util.Set;

import com.amazon.corretto.arctic.player.model.ArcticPlayerTestStatus;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;

public interface ArcticPlayerPostProcessor {
    boolean postProcess(ArcticRunningTest test);
    int getPriority();
    String getName();
    Set<TestStatusCode> getRegisteredStatuses();

    default void reset() {

    }

    default boolean isRegisteredStatus(final ArcticPlayerTestStatus status) {
        return getRegisteredStatuses().contains(status.getStatusCode());
    }
}
