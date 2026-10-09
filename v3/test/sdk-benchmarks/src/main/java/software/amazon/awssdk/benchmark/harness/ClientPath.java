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

import software.amazon.awssdk.http.HttpTransportMode;

/**
 * The client path a cell measures (Req 20.1): the generated Sync Client, the generated Async Client, and the Dynamic
 * Client in its sync and async forms. The path fixes the transport mode, so a cell can only pair a path with a
 * transport of the same mode.
 */
public enum ClientPath {
    GENERATED_SYNC("generated-sync", HttpTransportMode.SYNC, false),
    GENERATED_ASYNC("generated-async", HttpTransportMode.ASYNC, false),
    DYNAMIC_SYNC("dynamic-sync", HttpTransportMode.SYNC, true),
    DYNAMIC_ASYNC("dynamic-async", HttpTransportMode.ASYNC, true);

    private final String id;
    private final HttpTransportMode mode;
    private final boolean dynamic;

    ClientPath(String id, HttpTransportMode mode, boolean dynamic) {
        this.id = id;
        this.mode = mode;
        this.dynamic = dynamic;
    }

    public String id() {
        return id;
    }

    public HttpTransportMode mode() {
        return mode;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public static ClientPath fromId(String id) {
        for (ClientPath p : values()) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        throw new IllegalArgumentException("unknown client path: " + id);
    }
}
