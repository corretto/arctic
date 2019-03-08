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
package com.amazon.corretto.arctic.player.control.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Singleton;

import com.amazon.corretto.arctic.common.model.ArcticTest;
import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.tweak.ArcticTweakableComponent;
import com.amazon.corretto.arctic.common.tweak.TweakKeys;
import com.amazon.corretto.arctic.player.control.TimeController;
import com.amazon.corretto.arctic.shared.exception.ArcticException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class controls when different events need to be replayed. Depending on the configuration and the recording, this
 * might require sleeping for a certain amount to ensure the event is not replayed before it should.
 */
@Singleton
public final class AdvancedTimeController implements TimeController, ArcticTweakableComponent {
    private static final Logger log = LoggerFactory.getLogger(AdvancedTimeController.class);

    public static final String NAME = "advanced";

    private ArcticTest runningTest;
    private Iterator<ArcticEvent> events;
    private boolean safeMode = false;
    private long lastEventReturned;
    private long lastEventTs;

    @Override
    public void startTestCase(final ArcticTest test) {
        runningTest = test;
        this.events = Stream.concat(Stream.concat(
                test.getScreenChecks().stream(),
                test.getEvents().getMouseEvents().stream()),
                test.getEvents().getKeyboardEvents().stream())
                .filter(it -> it.getSubType().inMask(test.getPreferredPlayMode()))
                .sorted(Comparator.comparing(ArcticEvent::getTimestamp))
                .iterator();

        lastEventReturned = System.nanoTime();
        lastEventTs = 0;
    }

    @Override
    public ArcticEvent getNextEvent() {
        if (!events.hasNext()) {
            return null;
        }
        final ArcticEvent nextEvent = events.next();

        final long elapsed = System.nanoTime() - lastEventReturned;
        final long expected = nextEvent.getTimestamp() - lastEventTs;
        long toWait = expected - elapsed;
        if (runningTest.getTimings().getMaxWaitNs() > -1 && !safeMode) {
            // As of 2022-06-27 there is a bug on the recordings, they have the time in ms, not ns
            toWait = Math.min(toWait, runningTest.getTimings().getMaxWaitNs() * 1000000);
        }
        if (toWait > runningTest.getTimings().getMinWaitNs()) {
            waitFor(toWait / 1000000);
        }
        lastEventTs = nextEvent.getTimestamp();
        lastEventReturned = System.nanoTime();
        return nextEvent;
    }

    @Override
    public void waitForScreen() {
        waitFor(runningTest.getTimings().getScDelayMs());
        lastEventReturned = System.nanoTime();
    }

    @Override
    public void waitFor(final long timeMs) {
        try {
            Thread.sleep(timeMs);
        } catch (final InterruptedException e) {
            log.error("Time controller has been interrupted");
            throw new ArcticException("TimeController has been interrupted", e);
        }
    }

    @Override
    public void setTweak(final String key, final String value) {
        if (key.equalsIgnoreCase(TweakKeys.SAFE)) {
            safeMode = !("false".equalsIgnoreCase(value) || "0".equalsIgnoreCase(value));
            log.info("{} is now {}", key, safeMode);
        }
    }

    @Override
    public Set<String> getTweakKeys() {
        return Set.of(TweakKeys.SAFE);
    }

    @Override
    public String getTweakKeyDescription(final String key) {
        if (key.equalsIgnoreCase(TweakKeys.SAFE)) {
            return "Disable all time wait shortcuts";
        } else {
            return "Key not being used by this component";
        }
    }
}
