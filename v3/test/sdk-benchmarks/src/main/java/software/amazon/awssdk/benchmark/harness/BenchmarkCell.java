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

import java.util.Objects;

/**
 * One benchmark cell: protocol x client path x transport x workload kind x payload class x outcome x concurrency.
 * The cell is the unit a repetition measures, a null experiment duplicates and a gate compares; its {@link #id()} is
 * the stable key both forked processes receive and every result record carries.
 *
 * <p>Invariants (checked here, so no malformed cell can be launched): the transport's mode matches the path's;
 * finite cells have a payload class and streaming cells do not; streaming cells are {@code success} only; the
 * protocol supports the streaming kind; concurrency is positive.
 *
 * @param payload the finite payload class, or {@code null} for streaming and Event Stream cells
 */
public record BenchmarkCell(BenchProtocol protocol, ClientPath path, Transport transport, WorkloadKind kind,
                            PayloadClass payload, Outcome outcome, int concurrency) {

    private static final String NO_PAYLOAD = "-";

    public BenchmarkCell {
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(transport, "transport");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(outcome, "outcome");
        if (transport.mode() != path.mode()) {
            throw new IllegalArgumentException("transport " + transport.id() + " is " + transport.mode()
                                               + " but path " + path.id() + " is " + path.mode());
        }
        if (kind == WorkloadKind.FINITE && payload == null) {
            throw new IllegalArgumentException("finite cells need a payload class");
        }
        if (kind.isStreaming() && payload != null) {
            throw new IllegalArgumentException(kind.id() + " cells have no finite payload class");
        }
        if (kind.isStreaming() && outcome != Outcome.SUCCESS) {
            throw new IllegalArgumentException(kind.id() + " cells are success-only");
        }
        if (kind == WorkloadKind.STREAMING && !protocol.supportsStreamingBlobs()) {
            throw new IllegalArgumentException(protocol.id() + " has no streaming blob payloads");
        }
        if (kind == WorkloadKind.EVENT_STREAM && !protocol.supportsEventStreams()) {
            throw new IllegalArgumentException(protocol.id() + " has no Event Streams");
        }
        if (concurrency < 1) {
            throw new IllegalArgumentException("concurrency must be >= 1: " + concurrency);
        }
    }

    public static BenchmarkCell finite(BenchProtocol protocol, ClientPath path, Transport transport,
                                       PayloadClass payload, Outcome outcome, int concurrency) {
        return new BenchmarkCell(protocol, path, transport, WorkloadKind.FINITE, payload, outcome, concurrency);
    }

    /**
     * {@code protocol/path/transport/kind/payload/outcome/cN}, e.g.
     * {@code awsJson1_0/generated-sync/apache5-sync/finite/small/success/c1}; streaming cells use {@code -} for the
     * payload. Every token is from a fixed alphabet without {@code /}, so {@link #parse} inverts it exactly.
     */
    public String id() {
        return protocol.id() + '/' + path.id() + '/' + transport.id() + '/' + kind.id() + '/'
               + (payload == null ? NO_PAYLOAD : payload.id()) + '/' + outcome.id() + "/c" + concurrency;
    }

    public static BenchmarkCell parse(String id) {
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
        return new BenchmarkCell(BenchProtocol.fromId(t[0]), ClientPath.fromId(t[1]), Transport.fromId(t[2]),
                                 WorkloadKind.fromId(t[3]), NO_PAYLOAD.equals(t[4]) ? null : PayloadClass.fromId(t[4]),
                                 Outcome.fromId(t[5]), concurrency);
    }

    @Override
    public String toString() {
        return id();
    }
}
