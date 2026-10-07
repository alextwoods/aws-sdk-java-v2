/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.example.bodies;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.core.sync.RequestBody;

/** Builds request bodies and reads each twice; {@link #run} returns what happened, one line per body. */
public final class BodiesApp {
    private BodiesApp() {
    }

    /** Runs the body workflow; {@code endpoint} is unused (no service is called). */
    public static String run(String endpoint) {
        List<String> transcript = new ArrayList<>();
        transcript.add(describe("text", RequestBody.fromString("hello corpus")));
        transcript.add(describe("bytes", RequestBody.fromBytes("0123456789".getBytes(StandardCharsets.UTF_8))));
        transcript.add(describe("empty", RequestBody.empty()));
        return String.join("\n", transcript);
    }

    private static String describe(String name, RequestBody body) {
        String first = read(body);
        String second = read(body);
        return name + ": length=" + body.optionalContentLength().orElse(-1L) + " type=" + body.contentType()
               + " content=" + first + " replayed=" + first.equals(second);
    }

    private static String read(RequestBody body) {
        try (InputStream in = body.contentStreamProvider().newStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
