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
package com.amazon.corretto.arctic.common.backend.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.inject.Inject;

import com.amazon.corretto.arctic.common.backend.ArcticImageSaver;
import com.amazon.corretto.arctic.api.exception.ArcticException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Saves an image into disk making use of {@link ImageIO#write(java.awt.image.RenderedImage, String, java.io.File)}.
 */
@Slf4j
@NoArgsConstructor
public class JavaImageIoSaver implements ArcticImageSaver {
    @Getter private String format = "png";
    @Getter private String extension = ".png";

    @Inject
    public JavaImageIoSaver(final String format, final String extension) {
        this.format = format;
        this.extension = extension;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path saveImage(final BufferedImage image, final Path baseFolder, final Path relativeName) {
        final Path fileName  = relativeName.resolveSibling(relativeName.getFileName() + extension);
        final Path fullPath = baseFolder.resolve(fileName);
        fullPath.toFile().mkdirs();
        try {
            ImageIO.write(image, format, fullPath.toFile());
            return fileName;
        } catch (final IOException e) {
            log.warn("Unable to store image {}. Format was {}", fullPath, format);
            throw new ArcticException("Unable to save image");
        }
    }
}
