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
package com.amazon.corretto.arctic.common.model;

import lombok.Data;

/**
 * Record object for the Mouse Offset. Mouse offset indicate that we should click in a slightly different position when
 * running our test that what it was recorded.
 * TODO: Move to record when migrated to Java 17
 */
@Data
public class ArcticTestMouseOffsets {
    private int x = 0;
    private int y = 0;
}
