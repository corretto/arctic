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
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.imageio.ImageIO;

import com.amazon.corretto.arctic.common.backend.ArcticHashCalculator;
import com.amazon.corretto.arctic.api.exception.ArcticException;
import lombok.extern.slf4j.Slf4j;

/**
 * This implementation relies on {@link MessageDigest} to calculate the hash of an image.
 */
@Slf4j
public class MessageDigestHashCalculator implements ArcticHashCalculator {
    /**
     * {@inheritDoc}
     */
    @Override
    public String calculateHash(final BufferedImage image, final String algorithm) throws NoSuchAlgorithmException {
        final int[] rgbData = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        return calculateHash(rgbData, algorithm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String calculateHash(final int[] data, final String algorithm) throws NoSuchAlgorithmException {
        final ByteBuffer bb = ByteBuffer.allocate(data.length * 4);
        bb.asIntBuffer().put(data);

        final MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(bb);
        final BigInteger bigInt = new BigInteger(1, md.digest());
        return bigInt.toString(16);
    }

    @Override
    public String calculateHash(final String path, final String algorithm) throws NoSuchAlgorithmException {
        final File imageFile = new File(path);
        log.debug("Calculating hash for {}", imageFile);
        try {
            final BufferedImage image = ImageIO.read(imageFile);
            return calculateHash(image, algorithm);
        } catch (final IOException ex) {
            throw new ArcticException("Error reading image file " + path, ex);
        }
    }
}
