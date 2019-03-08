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

import com.amazon.corretto.arctic.common.session.ArcticSessionKeeper;
import com.amazon.corretto.arctic.player.model.FailureId;
import com.amazon.corretto.arctic.player.model.PixelCheckFailure;
import com.amazon.corretto.arctic.player.results.impl.InMemoryScFailureKeeper;
import com.google.inject.ImplementedBy;

/**
 * Keeps track of all the Screen check failures during the current run. If those failures were persisted to disk, they
 * can be reviewed later, and potentially add as alternatives.
 */
@ImplementedBy(InMemoryScFailureKeeper.class)
public interface ArcticScFailureKeeper extends ArcticSessionKeeper<FailureId, PixelCheckFailure> {

    /**
     * Returns the next failure to review without removing it from the queue.
     * @return The next failure to review. Null if no more failures are present
     */
    PixelCheckFailure peek();

    /**
     * Returns the next failure to review, removing it from the queue.
     * @return The next failure to review. Null if no more failures are present
     */
    PixelCheckFailure poll();


    /**
     * Applies the correct operation to a result.
     * @param result Operation to perform
     * @param failure The failure we want to promote to alternative image.
     * @return True if the failure has been successfully added as an alternative image.
     */
    boolean acceptResult(Result result, FailureId failure);

    /**
     * Enum to keep track of the different decisions that can happen during a review.
     */
    enum Result {
        ACCEPT,
        REJECT,
        IGNORE
    }
}
