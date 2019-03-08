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
package com.amazon.corretto.arctic.player.backend;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.amazon.corretto.arctic.common.model.event.ArcticEvent;
import com.amazon.corretto.arctic.common.tweak.ArcticTweakableComponent;
import com.amazon.corretto.arctic.common.tweak.TweakKeys;
import com.amazon.corretto.arctic.player.control.TimeController;
import com.amazon.corretto.arctic.player.inject.InjectionKeys;
import com.amazon.corretto.arctic.player.model.ArcticRunningTest;
import com.amazon.corretto.arctic.player.model.TestStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the central backend player. It will take the full timeline of events and send those events to the respective
 * backend players. It will handle whether the test needs to pass or fail.
 */
@Singleton
public final class MultiBackendPlayer implements ArcticTweakableComponent {
    private static final Logger log = LoggerFactory.getLogger(MultiBackendPlayer.class);
    private static final int SAFE_MASK = 0x13e06;

    private final Set<ArcticBackendPlayer> subPlayers;
    private final int supportedSubTypes;
    private final TimeController timeController;
    private final boolean fastMode;
    private final AtomicReference<ArcticRunningTest> runningTest = new AtomicReference<>();

    private boolean safeMode = false;

    /**
     * Creates a new MultiBackedPlayer. This constructor is called by Guice.
     * @param subPlayers All the subPlayers that will be player events.
     * @param timeController The timeController will guarantee the events are executed at the correct time, either
     *                       matching the original timeline or by trying to speed things up.
     * @param fastMode If fastMode is enabled, the playback of events will not stop if a failure is found.
     */
    @Inject
    public MultiBackendPlayer(final Set<ArcticBackendPlayer> subPlayers, final TimeController timeController,
                              @Named(InjectionKeys.FAST_MODE) final boolean fastMode) {
        this.subPlayers = subPlayers;
        supportedSubTypes = subPlayers.stream().map(ArcticBackendPlayer::supportedSubTypes).reduce(0, (a, b) -> a | b);
        this.timeController = timeController;
        this.fastMode = fastMode;
    }

    /**
     * Method that runs all the events for a test.
     * @param test Test for which we want to run the events.
     */
    public synchronized void runTestEvents(final ArcticRunningTest test) {
        if (!test.getStatus().getStatusCode().equals(TestStatusCode.RUNNING)) {
            // We only care about RUNNING status
            return;
        }
        runningTest.set(test);
        subPlayers.forEach(it -> it.init(test));
        ArcticEvent event;
        log.debug("Starting playback of {}:{}", test.getRecording().getTestName(), test.getRecording().getTestCase());
        boolean result = true;
        try {
            while ((event = timeController.getNextEvent()) != null) {
                if (runningTest.get() != test) {
                    // Received a signal to stop processing events. There are three scenarios for this to happen:
                    // A stop signal is received
                    // A finishedTestCase signal for the current test has been received
                    // A startingTestCase for a different test has been received
                    break;
                }
                final boolean supported = event.getSubType().inMask(supportedSubTypes);
                final boolean testPlay = event.getSubType().inMask(test.getRecording().getPreferredPlayMode());
                final boolean safePlay = safeMode && event.getSubType().inMask(SAFE_MASK);
                final boolean shouldPlay =  supported && (testPlay || safePlay);
                if (shouldPlay) {
                    //log.debug("Playing event: {}", event);
                    final boolean isOk = processEvent(event);
                    if (!isOk) {
                        log.info("FAILED {}:{}", test.getRecording().getTestName(), test.getRecording().getTestCase());

                        // This should cause the test to timeout. But when running in fast mode, we keep going and just
                        // mark the status of the test as failed. This means the test might only be seen as failed by
                        // arctic.
                        result = false;
                        test.getStatus().passed(false);
                        if (!fastMode) {
                            break;
                        }
                    }
                }
            }
        } finally {
            cleanup();
        }
        if (result) {
            test.getStatus().passed(result);
        }
    }

    /**
     * Interrupts the playback of events for a test, if that test is still running. Will not interrupt if the test being
     * executed is not the one specified. We interrupt a test because that test has finished or a new test has started.
     * @param interruptTest Test for which we want to interrupt.
     */
    public void interrupt(final ArcticRunningTest interruptTest) {
        runningTest.compareAndSet(interruptTest, null);
    }

    /**
     * Interrupts the current playback, regardless of the test that is currently running.
     */
    public void interrupt() {
        runningTest.set(null);
    }

    private boolean processEvent(final ArcticEvent e) {
        final boolean shouldPlay = e.getSubType().inMask(supportedSubTypes);
        if (shouldPlay) {
            return subPlayers.stream()
                    .filter(it -> it.acceptsEvent(e.getSubType()))
                    .map(it -> it.consumeEvent(e))
                    .reduce(true, (a, b) -> a && b);
        }

        return true;
    }

    /**
     * Cleanup the different backend players, getting ready to execute a new test.
     */
    public void cleanup() {
        subPlayers.forEach(ArcticBackendPlayer::cleanup);
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
            return "Reproduce all events in mode " + Integer.toHexString(SAFE_MASK);
        } else {
            return "Key not being used by this component";
        }
    }
}
