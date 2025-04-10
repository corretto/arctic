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

package com.amazon.corretto.arctic.recorder.backend.impl;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.recorder.backend.ArcticBackendRecorder;
import com.amazon.corretto.arctic.recorder.control.ArcticController;
import com.amazon.corretto.arctic.recorder.inject.InjectionKeys;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CompositeRecorder implements ArcticBackendRecorder {
    private static final String NAME = "comp";
    private final Set<ArcticBackendRecorder> subRecorders;
    private final int recordingMask;

    @Inject
    public CompositeRecorder(final Set<ArcticBackendRecorder> subRecorders,
            @Named(InjectionKeys.BACKEND_RECORDING_MODE) final int recordingMask) {
        this.subRecorders = subRecorders;
        this.recordingMask = recordingMask;
        log.debug("Composite recorder loaded with {} subRecorders", subRecorders.size());
    }

    @Override
    public List<ArcticEvent> getRecordingBuffer() {
        return subRecorders.stream()
                .peek(it -> log.debug("SubRecorder {}: {}", it.getName(), it.getRecordingBuffer().size()))
                .map(ArcticBackendRecorder::getRecordingBuffer)
                .flatMap(Collection::stream)
                .filter(it -> it.getSubType().inMask(recordingMask))
                .sorted(Comparator.comparing(ArcticEvent::getTimestamp))
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void acceptEvent(final ArcticController.Event event) {
        subRecorders.forEach(it -> it.acceptEvent(event));
    }
}
