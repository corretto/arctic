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
 * Record object for the Truncations. Truncations indicate that some events either at the beginning of the end of the
 * tests should be ignored during reproduction. This is because the events recorded might represent the act of start or
 * finish a recording, and might not be coherent.
 * TODO: Move to record when migrated to Java 17
 */
@Data
public class ArcticTestTruncations {
    private int mouseStart = 0;
    private int mouseEnd = 0;
    private int kbStart = 0;
    private int kbEnd = 0;
}
