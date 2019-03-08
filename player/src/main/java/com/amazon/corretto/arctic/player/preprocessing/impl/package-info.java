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
 * Implementations for the different pre-processors available in Arctic Player. These pre-processors will form a
 * pipeline that will be executed before the test replay starts. Some of the functions of the pre-processors include
 * reading and modifying the test, prepare the screen for the replay or clean up Arctic status. During the execution of
 * the pre-processing pipeline the test is in status
 * {@link com.amazon.corretto.arctic.player.model.TestStatusCode#STARTING}. Any pre-processor can fail the test and
 * transition to {@link com.amazon.corretto.arctic.player.model.TestStatusCode#ABORTED}.
 * The order of execution of the pre-processors are defined by the value of
 * {@link com.amazon.corretto.arctic.player.preprocessing.ArcticPlayerPreProcessor#getPriority()},
 * not by the list order of the configuration key defined by
 * {@link com.amazon.corretto.arctic.player.inject.InjectionKeys#PRE_PROCESSORS}.
 */
package com.amazon.corretto.arctic.player.preprocessing.impl;
