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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * A simple type adapter for Gson so we don't store integers with value 0. This is used to reduce the Events file
 * size.
 */
public final class GsonIgnoreZeroIntAdapter extends ArcticTypeAdapter<Integer> {
    @Override
    public Integer read(final JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return 0;
        }

        return in.nextInt();
    }

    @Override
    public void write(final JsonWriter out, final Integer data) throws IOException {
        if (data == null || data.equals(0)) {
            out.nullValue();
            return;
        }

        out.value(data.intValue());
    }

    @Override
    public Class<Integer> getAdaptedClass() {
        return Integer.class;
    }
}
