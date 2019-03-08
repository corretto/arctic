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

import com.amazon.corretto.arctic.common.serialization.GsonIntegerAsHexAdapter;
import com.google.gson.annotations.JsonAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the state of a JFrame, either the Workbench or one of the shades.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArcticFrame {
    /**
     * Title of the window.
     */
    private String title;

    /**
     * Color used for the window background.
     */
    @JsonAdapter(GsonIntegerAsHexAdapter.class)
    private int color;

    /**
     * Position and size of the window.
     */
    private ScreenArea sa;
}
