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

package com.amazon.corretto.arctic.common.model.gui;

import java.awt.Rectangle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A very simple representation of a rectangle that we can serialize/deserialize using Gson.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class ScreenArea {
    private int x;
    private int y;
    private int w;
    private int h;

    public ScreenArea(final Rectangle r) {
        this((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
    }

    public Rectangle asRectangle() {
        return new Rectangle(x, y, w, h);
    }
}
