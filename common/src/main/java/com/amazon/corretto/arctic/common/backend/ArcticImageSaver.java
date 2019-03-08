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
package com.amazon.corretto.arctic.common.backend;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

import com.amazon.corretto.arctic.common.backend.impl.JavaImageIoSaver;
import com.google.inject.ImplementedBy;

/**
 * Allows to save a BufferedImage into disk.
 *
 * This interface has a default implementation on {@link JavaImageIoSaver}
 */
@ImplementedBy(JavaImageIoSaver.class)
public interface ArcticImageSaver {
    /**
     * Saves an specific image into disk.
     * @param image Image to save.
     * @param folder Folder where the image is saved. Folder tree will be created if needed.
     * @param relativeName Name of the file to save, without extension.
     * @return path and name of the file, including extension, relative to the folder parameter.
     */
    Path saveImage(BufferedImage image, Path folder, Path relativeName);

    /**
     * Returns the format the instance is configured to save the images into.
     * @return String representing the format.
     */
    String getFormat();
}
