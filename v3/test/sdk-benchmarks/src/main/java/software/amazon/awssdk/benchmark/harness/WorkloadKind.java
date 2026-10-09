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
 * The shape of a cell's operation (Req 20.3): a finite request/response sized by a {@link PayloadClass}, a streaming
 * blob transfer, or an Event Stream exchange. Streaming kinds carry no payload class: their size is the run's stream
 * length, not a finite class.
 */
public enum WorkloadKind {
    FINITE("finite"),
    STREAMING("stream"),
    EVENT_STREAM("eventstream");

    private final String id;

    WorkloadKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public boolean isStreaming() {
        return this != FINITE;
    }

    public static WorkloadKind fromId(String id) {
        for (WorkloadKind k : values()) {
            if (k.id.equals(id)) {
                return k;
            }
        }
        throw new IllegalArgumentException("unknown workload kind: " + id);
    }
}
