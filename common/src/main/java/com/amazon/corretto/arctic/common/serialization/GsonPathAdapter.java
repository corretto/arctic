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

package com.amazon.corretto.arctic.common.serialization;

import java.io.IOException;
import java.nio.file.Path;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * An adapter to allow Gson to serialize Path objects as Strings.
 */
public final class GsonPathAdapter extends ArcticTypeAdapter<Path> {

    private final boolean windowsLegacy;

    /**
     * Creates a new instance of the adapter. The Windows Legacy mode is set to false.
     */
    public GsonPathAdapter() {
        this(false);
    }

    /**
     * Creates a new instance of the adapter.
     * @param windowsLegacy If true, save paths using double backslash
     */
    public GsonPathAdapter(final boolean windowsLegacy) {
        this.windowsLegacy = windowsLegacy;
    }

    @Override
    public Path read(final JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return Path.of(in.nextString().replace('\\', '/'));
    }

    @Override
    public void write(final JsonWriter out, final Path data) throws IOException {
        if (data == null) {
            out.nullValue();
            return;
        }
        if (windowsLegacy) {
            out.value(data.toString().replace('/', '\\'));
        } else {
            out.value(data.toString().replace('\\', '/'));
        }
    }

    @Override
    public Class<Path> getAdaptedClass() {
        return Path.class;
    }
}
