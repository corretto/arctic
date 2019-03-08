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
 * Implementations for the different post-processors available in Arctic Player. These post-processors will form a
 * pipeline that will be executed after the test replay end. Some of the functions of the post-processors include
 * storing information related to the test execution and update the existing recordings. When this pipeline is executed
 * depends on whether {@link com.amazon.corretto.arctic.player.inject.InjectionKeys#CONFIRMATION_MODE} is enabled. When
 * disabled, the pipeline will be executed immediately after the test replay ends. When enabled, it will not be executed
 * until confirmation of the test ending is received.
 * The order of execution of the pre-processors are defined by the value of
 * {@link com.amazon.corretto.arctic.player.postprocessing.ArcticPlayerPostProcessor#getPriority()},
 * not by the list order of the configuration key defined by
 * {@link com.amazon.corretto.arctic.player.inject.InjectionKeys#POST_PROCESSORS}.
 */
package com.amazon.corretto.arctic.player.postprocessing.impl;
