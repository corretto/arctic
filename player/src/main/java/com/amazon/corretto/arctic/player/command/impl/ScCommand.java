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

package com.amazon.corretto.arctic.player.command.impl;

import java.nio.file.Path;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import com.amazon.corretto.arctic.common.command.ArcticCommand;
import com.amazon.corretto.arctic.common.model.ArcticResultTuple;
import com.amazon.corretto.arctic.player.backend.pixel.PixelImageComparator;
import com.amazon.corretto.arctic.player.gui.ScreenCheckReview;
import com.amazon.corretto.arctic.player.model.ArcticDiffImages;
import com.amazon.corretto.arctic.player.model.PixelCheckFailure;
import com.amazon.corretto.arctic.player.results.ArcticScFailureKeeper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ArcticCommand used to review and manipulate ScreenshotChecks. It contains four different modes:
 * - list: Prints all the recorded failures
 * - clear: Removes recorded failures (but not other test execution data.
 * - review: Review the last failure.
 * - all: Review all the failures.
 */
public final class ScCommand extends ArcticCommand {
    private static final Logger log = LoggerFactory.getLogger(ScCommand.class);
    public static final String[] COMMAND_LINE = new String[]{"sc"};

    private final ArcticScFailureKeeper failureManager;
    private final PixelImageComparator imgComparator;
    private final Provider<ScreenCheckReview> reelProvider;
    private ScreenCheckReview reel;

    /**
     * Creates a new instance of the command.
     *
     * @param failureManager To retrieve failures and send updates after reviewing
     * @param imgComparator  To calculate the differences between images
     * @param reelProvider   An injection provider to get the reel. This is a provider to avoid building the UI until
     *                       requested.
     */
    @Inject
    public ScCommand(final ArcticScFailureKeeper failureManager, final PixelImageComparator imgComparator,
                     final Provider<ScreenCheckReview> reelProvider) {
        this.failureManager = failureManager;
        this.imgComparator = imgComparator;
        this.reelProvider = reelProvider;
    }

    @Override
    public String run(final String... args) {
        if (args.length < 2) {
            return getHelp();
        }
        if (reel == null) {
            reel = reelProvider.get();
        }

        switch (args[1]) {
            case "list":
                return list();
            case "clear":
                return clear();
            case "review":
                return reviewNext();
            case "all":
                return reviewAll();
            default:
                return getHelp();
        }
    }

    private String reviewNext() {
        final PixelCheckFailure failure = failureManager.poll();
        if (failure == null) {
            return "No failures to check";
        }
        ArcticDiffImages diffImages = new ArcticDiffImages(failure);
        try {
            return review(failure, diffImages, false);
        } catch (final Exception e) {
            e.printStackTrace();
            log.error("Error when processing {}, {}", failure, e);
            log.error("FailureId: {}", failure);
            log.error("FailureId.getTestId: {}", failure.getFailureId().getTestId());
            log.error("FailureId.getScope: {}", failure.getScope());
            log.error("FailureId.getSavedImagePath: {}", failure.getFailureId().getSavedImagePath());
            return "Error when processing " + failure + System.lineSeparator() + e.getMessage();
        }
    }

    private String review(final PixelCheckFailure failure, final ArcticDiffImages diffImages, final boolean batch)
            throws AbortReviewException {
        imgComparator.generateDiff(diffImages);
        ScreenCheckReview.Result result = reel.run(diffImages);
        switch (result) {
            case REJECT:
                failureManager.acceptResult(ArcticScFailureKeeper.Result.REJECT, failure.getFailureId());
                return String.format("%s rejected", failure.getFailureId());
            case ACCEPT:
                final boolean wasAdded = failureManager.acceptResult(ArcticScFailureKeeper.Result.ACCEPT,
                        failure.getFailureId());
                return String.format("%s %s", failure.getFailureId(), wasAdded ? "updated" : "failed to update");
            case ABORT:
                if (batch) {
                    throw new AbortReviewException("Run has been aborted");
                }
            case IGNORE:
                failureManager.acceptResult(ArcticScFailureKeeper.Result.IGNORE, failure.getFailureId());
                return String.format("%s ignored", failure.getFailureId());
            default:
                return "Something strange happened, unexpected ImageReel.Result";
        }
    }

    private String reviewAll() {
        PixelCheckFailure currentFailure;
        PixelCheckFailure nextFailure = failureManager.peek();
        PixelCheckFailure first = null;
        ArcticDiffImages nextDiffImages = new ArcticDiffImages(nextFailure);
        final StringBuilder sb = new StringBuilder();
        while ((currentFailure = failureManager.peek()) != null) {
            if (first == null) {
                first = currentFailure;
            } else if (currentFailure == first) {
                // We have done a full round, end
                return sb.toString();
            }
            currentFailure = failureManager.poll();
            try {

                // State of the queue may have changed and what we polled is not what we peeked in the last cycle. This
                // means we will need to regenerate the ArcticDiffImages and call the generateDiff synchronously. This will
                // also guarantee we wait for the images to be completed if needed.
                ArcticDiffImages currentDiffImages = currentFailure == nextFailure ? nextDiffImages
                        : new ArcticDiffImages(currentFailure);
                imgComparator.generateDiff(currentDiffImages);

                // Attempt to preprocess our next set of images in parallel
                nextFailure = failureManager.peek();
                nextDiffImages = null;
                if (nextFailure != null) {
                    nextDiffImages = new ArcticDiffImages(nextFailure);
                    preProcess(nextDiffImages);
                }
                final String result = review(currentFailure, currentDiffImages, true);
                log.debug(result);
                sb.append(result).append(System.lineSeparator());
            } catch (AbortReviewException e) {
                failureManager.acceptResult(ArcticScFailureKeeper.Result.IGNORE, currentFailure.getFailureId());
                return e.getMessage();
            } catch (final Exception e) {
                log.error("Error when processing {}", currentFailure.getFailureId(), e);
                sb.append("Error when processing: ").append(currentFailure.getFailureId())
                        .append(System.lineSeparator()).append(e.getMessage());
                failureManager.acceptResult(ArcticScFailureKeeper.Result.IGNORE, currentFailure.getFailureId());
            }
        }
        return sb.toString();
    }

    private void preProcess(final ArcticDiffImages diffImages) {
        ForkJoinPool.commonPool().execute(() -> imgComparator.generateDiff(diffImages));
    }

    private String clear() {
        failureManager.clear();
        return "ScreenCheck failures cleared";
    }

    private String list() {
        return failureManager.getResults().stream()
                .map(ArcticResultTuple::getValue)
                .map(PixelCheckFailure::getCurrentImageFullPath)
                .map(Path::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public String[] getCommandLine() {
        return COMMAND_LINE;
    }

    @Override
    public String getHelp() {
        return getDescription() + System.lineSeparator()
                + "Usage:" + System.lineSeparator()
                + String.format("  %s [SUBCOMMAND]", String.join(" ", COMMAND_LINE))
                + System.lineSeparator() + System.lineSeparator()
                + "SUBCOMMAND:" + System.lineSeparator()
                + String.format("  %-20s%s", "list", "Display ScreenCheck failures stored") + System.lineSeparator()
                + String.format("  %-20s%s", "clear", "Clear ScreenCheck failures stores") + System.lineSeparator()
                + String.format("  %-20s%s", "review", "Review the next ScreenCheck failure in the list")
                + System.lineSeparator()
                + String.format("  %-20s%s", "all", "Review all the ScreenCheck failures");
    }

    @Override
    public String getDescription() {
        return "Last failed screen check operations";
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    private static final class AbortReviewException extends Exception {
        private AbortReviewException(final String msg) {
            super(msg);
        }
    }
}
