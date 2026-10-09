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

package software.amazon.awssdk.benchmark.harness;

/**
 * The SDK-free form of a {@link BenchmarkCell} id, as every arm's client process receives it on {@code --cell}:
 * {@code protocol/path/transport/kind/payload/outcome/cN}. Only the tokens' shape is checked here; what a token means
 * (and whether the arm serves it) is the {@link ClientDriver.WorkloadSource}'s business, so a reference client never
 * needs the V3-bound {@link BenchmarkCell} (whose transports are V3 classes).
 */
public record CellId(String protocol, String path, String transport, String kind, String payload, String outcome,
                     int concurrency) {

    public CellId {
        for (String t : new String[] {protocol, path, transport, kind, payload, outcome}) {
            if (t == null || t.isEmpty() || t.contains("/")) {
                throw new IllegalArgumentException("malformed cell token: " + t);
            }
        }
        if (concurrency < 1) {
            throw new IllegalArgumentException("concurrency must be >= 1: " + concurrency);
        }
    }

    public static CellId parse(String id) {
        String[] t = id.split("/", -1);
        if (t.length != 7 || !t[6].startsWith("c")) {
            throw new IllegalArgumentException("malformed cell id: " + id);
        }
        int concurrency;
        try {
            concurrency = Integer.parseInt(t[6].substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("malformed cell concurrency: " + id, e);
        }
        return new CellId(t[0], t[1], t[2], t[3], t[4], t[5], concurrency);
    }

    /** The exact id this was parsed from. */
    public String id() {
        return protocol + '/' + path + '/' + transport + '/' + kind + '/' + payload + '/' + outcome + "/c" + concurrency;
    }

    @Override
    public String toString() {
        return id();
    }
}
