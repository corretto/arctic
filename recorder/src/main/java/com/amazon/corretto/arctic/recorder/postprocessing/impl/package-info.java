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

/**
 * This package contains postprocessors for the recorder. These postprocessors are executed immediately after the test
 * recording finishes. Some of them may correct problems that can happen in the recorder (like leaving a key pressed
 * without releasing) while others might persist the recording to disk.
 */
package com.amazon.corretto.arctic.recorder.postprocessing.impl;
