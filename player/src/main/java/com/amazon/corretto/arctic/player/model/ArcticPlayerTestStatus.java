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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Wrap TestStatusCode with an AtomicReference to ensure Thread safety. All transitions are guaranteed to be processed
 * and applied, but there is no guarantee the end result will be the same for different orders. For example:
 * (STARTING) -> pass -> (RUNNING) -> fail -> (FAILED)
 * (STARTING) -> fail -> (ABORTED) -> pass (ABORTED)
 */
@ThreadSafe
public final class ArcticPlayerTestStatus {
    private static final Logger log = LoggerFactory.getLogger(ArcticPlayerTestStatus.class);
    private final AtomicReference<TestStatusCode> value;

    public ArcticPlayerTestStatus() {
        this(TestStatusCode.STARTING);
    }

    public ArcticPlayerTestStatus(TestStatusCode statusCode) {
        value = new AtomicReference<>(statusCode);
    }

    public TestStatusCode getStatusCode() {
        return value.get();
    }

    public void passed(final boolean passed) {
        final TestStatusCode old;
        if (passed) {
            old = value.getAndUpdate(TestStatusCode::pass);
            log.debug("{} -> {} -> {}", old, "pass", value.get());
        } else {
            old = value.getAndUpdate(TestStatusCode::fail);
            log.debug("{} -> {} -> {}", old, "fail", value.get());
        }
    }

    public void error() {
        final TestStatusCode old = value.getAndUpdate(TestStatusCode::error);
        log.debug("{} -> {} -> {}", old, "error", value.get());

    }

    public void stop() {
        final TestStatusCode old = value.getAndUpdate(TestStatusCode::stop);
        log.debug("{} -> {} -> {}", old, "stop", value.get());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ArcticPlayerTestStatus that = (ArcticPlayerTestStatus) o;
        return Objects.equals(value.get(), that.value.get());
    }
}
