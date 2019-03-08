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
import java.security.NoSuchAlgorithmException;

import com.amazon.corretto.arctic.common.backend.impl.MessageDigestHashCalculator;
import com.google.inject.ImplementedBy;

/**
 * Calculates a hash based on the contents of an image. This hash will be different than the hash of an image file, as
 * only the contents of the image raster are taken into account.
 *
 * This interface has a default implementation on {@link MessageDigestHashCalculator}
 */
@ImplementedBy(MessageDigestHashCalculator.class)
public interface ArcticHashCalculator {

    /**
     * Calculate the hash for an image.
     * @param image Image to calculate the hash for
     * @param algorithm Algorithm to use when calculating the hash
     * @return A String with hexadecimal encoding of the hash
     * @throws NoSuchAlgorithmException if the algorithm param is not supported
     */
    String calculateHash(BufferedImage image, String algorithm) throws NoSuchAlgorithmException;

    /**
     * Calculate the hash for a set of data.
     * @param data int array to calculate the hash for
     * @param algorithm Algorithm to use when calculating the hash
     * @return A String with hexadecimal encoding of the hash
     * @throws NoSuchAlgorithmException if the algorithm param is not supported
     */
    String calculateHash(int[] data, String algorithm) throws NoSuchAlgorithmException;

    /**
     * Calculate the hash for an image file.
     * @param filePath path to an existing image
     * @param algorithm Algorithm to use when calculating the hash
     * @return A String with hexadecimal encoding of the hash
     * @throws NoSuchAlgorithmException if the algorithm param is not supported
     */
    String calculateHash(String filePath, String algorithm) throws NoSuchAlgorithmException;
}
