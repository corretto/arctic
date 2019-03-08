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
 * This package contains all the different implementations capable of recording events for Arctic.
 * {@link com.amazon.corretto.arctic.recorder.backend.ArcticBackendRecorder} is the common interface for them. Some
 * auxiliary classes to convert the events may be placed in the converters package.
 *
 * The initial version contains three types of recorders
 * - {@link com.amazon.corretto.arctic.recorder.backend.impl.CompositeRecorder} is a recorder used to support more than
 * one recorder running at the same time
 * - {@link com.amazon.corretto.arctic.recorder.backend.impl.ScreenCheckBackendRecorder} captures the screen using the
 * AWT robot
 * - A series of Jnh recorders to capture mouse and keyboard events
 *
 * Which recorders are enabled it is defined by the arctic.recorder.backend.recorders property
 */
package com.amazon.corretto.arctic.recorder.backend;
