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
 * This package contains the actual implementation for the different backend players used for arctic. Each one of them
 * is capable of taking events of at least one type, reproduce them and optionally, decide if the test passes or not.
 *
 * And example of a backend player would be one that reproduces mouse events or keyboard events. ScreenshotChecks are
 * considered normal events to, so the proper backend player will take a screenshot and perform the comparison to
 * decide if the test should continue.
 */
package com.amazon.corretto.arctic.player.backend.impl;
