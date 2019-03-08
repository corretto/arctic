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
 * This packages holds different image comparison strategies to define whether a screenshot of a test is acceptable
 * compared to the one from a recording. The pixel check works like a pipeline with several steps. Similar to a chain of
 * responsibility DP. Each one of the steps can pass or fail the image. Depending on the type of check, this can cause
 * the pipeline to finish or to proceed to the next check.
 *
 * There are two types of checks, sufficient and non sufficient. The different checks are executed in order. Failing a
 * non-sufficient test causes the image to be considered not valid. Passing a sufficient test causes the image to be
 * considered valid. Usually, the first checks to be executed are able to give a response very quickly (matching the
 * hash is enough to pass the check, but non-matching dimensions is enough for a fail), while the last ones perform
 * expensive pixel by pixel operations.
 *
 */
package com.amazon.corretto.arctic.player.backend.pixel.check;