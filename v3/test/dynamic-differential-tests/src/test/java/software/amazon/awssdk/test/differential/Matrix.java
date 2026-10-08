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

package software.amazon.awssdk.test.differential;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import software.amazon.awssdk.dynamic.model.DynamicModel;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The dimensions of the generated-versus-dynamic differential matrix, its cells and the explicit list of unsupported
 * cells with their justification.
 *
 * <p>A cell is a launch protocol, a call shape and an execution mode. Every cell of the full product is either run by
 * at least one {@link Scenario} (a synthetic service always, a representative fleet service where one has the shape)
 * or listed in {@link #UNSUPPORTED} with the reason no generated client can make the call; never both.
 */
final class Matrix {
    private Matrix() {
    }

    /** The seven launch protocols. */
    enum Protocol {
        AWS_JSON_1_0("awsJson1_0"),
        AWS_JSON_1_1("awsJson1_1"),
        RPCV2_CBOR("rpcv2Cbor"),
        AWS_QUERY("awsQuery"),
        EC2_QUERY("ec2Query"),
        REST_JSON_1("restJson1"),
        REST_XML("restXml");

        final String id;

        Protocol(String id) {
            this.id = id;
        }

        boolean rest() {
            return this == REST_JSON_1 || this == REST_XML;
        }

        boolean query() {
            return this == AWS_QUERY || this == EC2_QUERY;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    /** The call shapes of the matrix. */
    enum CallShape {
        /** A buffered request and a modeled success. */
        FINITE,
        /** A buffered request answered with a modeled error. */
        MODELED_ERROR,
        /** A buffered request answered with an unmodeled error status (retried once when retryable). */
        UNMODELED_ERROR,
        /** A streaming blob request payload. */
        STREAMING_INPUT,
        /** A streaming blob response payload consumed by a response transformer. */
        STREAMING_OUTPUT,
        /** An input event stream (signed request frames). */
        EVENT_STREAM_INPUT,
        /** An output event stream (decoded response frames). */
        EVENT_STREAM_OUTPUT,
        /** Every page of a token-paginated operation. */
        PAGINATED,
        /** A modeled waiter that polls until a success acceptor matches. */
        WAITER;

        boolean eventStream() {
            return this == EVENT_STREAM_INPUT || this == EVENT_STREAM_OUTPUT;
        }

        boolean streamingBlob() {
            return this == STREAMING_INPUT || this == STREAMING_OUTPUT;
        }
    }

    enum Mode {
        SYNC,
        ASYNC
    }

    record Cell(Protocol protocol, CallShape shape, Mode mode) {
        @Override
        public String toString() {
            return protocol + " " + shape + " " + mode;
        }
    }

    /** Every cell of the full product, in protocol, shape and mode order. */
    static List<Cell> allCells() {
        List<Cell> cells = new ArrayList<>();
        for (Protocol protocol : Protocol.values()) {
            for (CallShape shape : CallShape.values()) {
                for (Mode mode : Mode.values()) {
                    cells.add(new Cell(protocol, shape, mode));
                }
            }
        }
        return cells;
    }

    /** The reason every unsupported cell is not run; each names the runtime or API fact that excludes it. */
    static final Map<Cell, String> UNSUPPORTED = unsupported();

    static final String NO_PAYLOAD_BINDING =
        "the protocol serializes the whole input/output as one document and has no HTTP payload binding, so a "
        + "@streaming blob member cannot be bound (protocol-core StreamingPayloads.streamingRejection; the Smithy "
        + "awsJson1_0/awsJson1_1/rpcv2Cbor/awsQuery/ec2Query specifications)";
    static final String NO_EVENT_STREAMS =
        "the query protocols do not support event streams (protocol-core StreamingPayloads.streamingRejection; the "
        + "Smithy awsQuery/ec2Query specifications)";
    static final String ASYNC_ONLY_EVENT_STREAMS =
        "generated sync clients expose no event stream operation (event streams are async-only, as in V2), so there "
        + "is no generated sync call to compare with; GeneratedDynamicDifferentialMatrixTest verifies the absence";

    private static Map<Cell, String> unsupported() {
        Map<Cell, String> out = new LinkedHashMap<>();
        for (Cell cell : allCells()) {
            Protocol protocol = cell.protocol();
            if (cell.shape().streamingBlob() && !protocol.rest()) {
                out.put(cell, NO_PAYLOAD_BINDING);
            } else if (cell.shape().eventStream() && protocol.query()) {
                out.put(cell, NO_EVENT_STREAMS);
            } else if (cell.shape().eventStream() && cell.mode() == Mode.SYNC) {
                out.put(cell, ASYNC_ONLY_EVENT_STREAMS);
            }
        }
        return Collections.unmodifiableMap(out);
    }

    // -------------------------------------------------------------------------------------------------------------
    // Scenarios

    /**
     * A generated client and the Dynamic Client of the same service, built from the same model AST.
     *
     * @param name the target's name in the report ({@code synthetic:<sdkId>} or {@code fleet:<module>})
     * @param clientPackage the generated client's base package
     * @param clientPrefix the generated client's type prefix ({@code <prefix>Client})
     * @param signingName the SigV4 signing name, for verifying event stream frame signatures
     * @param model the runtime model of the Dynamic Client, loaded once
     */
    record Target(String name, Protocol protocol, ShapeId serviceId, String clientPackage, String clientPrefix,
                  String signingName, Supplier<DynamicModel> model) {
        @Override
        public String toString() {
            return name;
        }
    }

    /** The expected terminal outcome of a call, checked on the generated (reference) client. */
    enum Outcome {
        SUCCESS,
        MODELED_ERROR,
        UNMODELED_ERROR
    }

    /** One input event: the event stream union member and its values. */
    record Event(String member, Map<String, Object> values) {
    }

    /**
     * One deterministic call of a matrix cell, run in each of {@link #modes()} through both clients.
     *
     * @param operation the exact modeled operation name
     * @param input the modeled input (without any streaming member)
     * @param replies the scripted responses of the 1st, 2nd, ... request (the last repeats)
     * @param requestBody the streaming blob request body, for {@link CallShape#STREAMING_INPUT}
     * @param eventsMember the event stream member of the input, for {@link CallShape#EVENT_STREAM_INPUT}
     * @param eventUnion the simple name of the generated input event union type
     * @param inputEvents the input events, in order
     * @param waiter the modeled waiter name, for {@link CallShape#WAITER}
     * @param expected the reference outcome
     * @param requests the exact number of requests the reference must send
     * @param events the exact number of response events the reference must receive (output event streams)
     */
    record Scenario(Target target, CallShape shape, String title, String operation, Map<String, Object> input,
                    List<Reply> replies, byte[] requestBody, String eventsMember, String eventUnion,
                    List<Event> inputEvents, String waiter, Outcome expected, int requests, int events) {
        Set<Mode> modes() {
            return shape.eventStream() ? EnumSet.of(Mode.ASYNC) : EnumSet.allOf(Mode.class);
        }

        Protocol protocol() {
            return target.protocol();
        }

        @Override
        public String toString() {
            return target + " " + shape + " " + operation + (title.isEmpty() ? "" : " (" + title + ")");
        }
    }

    /** A builder of a {@link Scenario}; defaults: one request, a successful outcome, no streams. */
    static final class ScenarioBuilder {
        private final Target target;
        private final CallShape shape;
        private final String operation;
        private String title = "";
        private Map<String, Object> input = Map.of();
        private final List<Reply> replies = new ArrayList<>();
        private byte[] requestBody;
        private String eventsMember;
        private String eventUnion;
        private List<Event> inputEvents = List.of();
        private String waiter;
        private Outcome expected = Outcome.SUCCESS;
        private int requests = 1;
        private int events;

        ScenarioBuilder(Target target, CallShape shape, String operation) {
            this.target = target;
            this.shape = shape;
            this.operation = operation;
            if (shape == CallShape.MODELED_ERROR) {
                expected = Outcome.MODELED_ERROR;
            } else if (shape == CallShape.UNMODELED_ERROR) {
                expected = Outcome.UNMODELED_ERROR;
            }
        }

        ScenarioBuilder title(String value) {
            this.title = value;
            return this;
        }

        ScenarioBuilder input(Map<String, Object> value) {
            this.input = value;
            return this;
        }

        ScenarioBuilder reply(Reply... value) {
            replies.addAll(Arrays.asList(value));
            return this;
        }

        ScenarioBuilder requestBody(byte[] value) {
            this.requestBody = value;
            return this;
        }

        ScenarioBuilder inputEvents(String member, String union, Event... value) {
            this.eventsMember = member;
            this.eventUnion = union;
            this.inputEvents = List.of(value);
            return this;
        }

        ScenarioBuilder waiter(String value) {
            this.waiter = value;
            return this;
        }

        ScenarioBuilder expect(Outcome value) {
            this.expected = value;
            return this;
        }

        ScenarioBuilder requests(int value) {
            this.requests = value;
            return this;
        }

        ScenarioBuilder events(int value) {
            this.events = value;
            return this;
        }

        Scenario build() {
            if (replies.isEmpty()) {
                throw new IllegalStateException("no reply for " + target + " " + operation);
            }
            return new Scenario(target, shape, title, operation, input, List.copyOf(replies), requestBody,
                                eventsMember, eventUnion, inputEvents, waiter, expected, requests, events);
        }
    }

    /**
     * One scripted response: status, headers in order and the body as chunks (one chunk per unit of async demand; a
     * sync response gets them concatenated). An event stream body has one chunk per encoded frame.
     */
    record Reply(int status, List<String[]> headers, List<byte[]> chunks) {
        static Reply of(int status, byte[] body, String... headers) {
            return new Reply(status, pairs(headers), body.length == 0 ? List.of() : List.of(body));
        }

        static Reply of(int status, String body, String... headers) {
            return of(status, body.getBytes(StandardCharsets.UTF_8), headers);
        }

        static Reply frames(int status, List<byte[]> frames, String... headers) {
            return new Reply(status, pairs(headers), List.copyOf(frames));
        }

        byte[] body() {
            int size = chunks.stream().mapToInt(c -> c.length).sum();
            byte[] out = new byte[size];
            int at = 0;
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, out, at, chunk.length);
                at += chunk.length;
            }
            return out;
        }

        private static List<String[]> pairs(String... headers) {
            List<String[]> out = new ArrayList<>();
            for (int i = 0; i < headers.length; i += 2) {
                out.add(new String[] {headers[i], headers[i + 1]});
            }
            return out;
        }
    }
}
