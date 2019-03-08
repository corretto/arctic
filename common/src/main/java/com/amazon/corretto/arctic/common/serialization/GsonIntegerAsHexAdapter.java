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
import com.google.gson.stream.JsonWriter;

/**
 * Gson TypeAdapter so we store some integers with their hex representation. This is useful for those integers that
 * represent binary masks.
 */
public final class GsonIntegerAsHexAdapter extends ArcticTypeAdapter<Integer> {
    @Override
    public Integer read(final JsonReader in) throws IOException {
        return Integer.parseUnsignedInt(in.nextString(), 16);
    }

    @Override
    public void write(final JsonWriter out, final Integer data) throws IOException {
        out.value(Integer.toHexString(data));
    }

    @Override
    public Class<Integer> getAdaptedClass() {
        return Integer.class;
    }
}
