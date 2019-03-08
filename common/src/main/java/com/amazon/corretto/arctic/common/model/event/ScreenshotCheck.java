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

package com.amazon.corretto.arctic.common.model.event;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amazon.corretto.arctic.common.model.gui.ArcticFrame;
import com.amazon.corretto.arctic.common.model.gui.ScreenArea;
import lombok.Data;

/**
 * Represents the need to ensure that the current state of the screen matches what was being recorded.
 */
@Data
public final class ScreenshotCheck implements ArcticEvent {
    /**
     * Actual image of the screen. This property is not store in the Json file, although it can be optionally stored on
     * a different image file.
     */
    private transient BufferedImage image;

    /**
     * When the event happened relative to the start of the recording.
     */
    private long timestamp;

    /**
     * If present, represents where the image for this event is store relative to the test repository folder.
     */
    private Path filename;

    /**
     * If present, a List of alternative images that are acceptable for this screen check.
     */
    private Set<Path> alternativeImages = new HashSet<>();

    /**
     * If present, a List of alternative hashes of images that are acceptable for this screen check.
     */
    private Set<String> alternativeHashes = new HashSet<>();

    /**
     * Format used to save this image into disk.
     */
    private String format;

    /**
     * Algorithm used to calculate a hash value of the image.
     */
    private String hashMode;

    /**
     * A hash calculated of the image contents. This can be easy to compare if two images are the same without the need
     * to load them from disk and compare pixel by pixel.
     */
    private String hashValue;

    /**
     * Area of the screen the ScreenCheck covers.
     */
    private ScreenArea sa;

    /**
     * Only applicable when used with the fuzzy comparator.
     */
    private float confidenceLevel;

    /**
     * State of the Workbench when the recording of the test started.
     */
    private ArcticFrame workbench;

    /**
     * State of the shades when the recording of the test started.
     */
    private List<ArcticFrame> shades = new ArrayList<>();

    @Override
    public Type getType() {
        return Type.SCREENSHOT_CHECK;
    }

    @Override
    public SubType getSubType() {
        return SubType.SCREENSHOT_CHECK;
    }
}
