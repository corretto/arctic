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

import com.amazon.corretto.arctic.common.model.TestId;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * An adapter to allow Gson to serialize FailureId objects as Strings.
 */
public final class TestIdTypeAdapter extends ArcticTypeAdapter<TestId> {
    private static final String SEPARATOR = "___";

    @Override
    public TestId read(final JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String[] tokens = in.nextString().split(SEPARATOR);
        if (tokens.length == 2) {
            return new TestId(tokens[0], tokens[1]);
        } else {
            return new TestId(tokens[2], tokens[0], tokens[1]);
        }
    }

    @Override
    public void write(final JsonWriter out, final TestId data) throws IOException {
        if (data == null) {
            out.nullValue();
            return;
        }
        out.value(String.format("%s%s%s%s%s", data.getTestClass(), SEPARATOR, data.getTestCase(), SEPARATOR,
                data.getTestGroup()));
    }

    @Override
    public Class<TestId> getAdaptedClass() {
        return TestId.class;
    }
}
