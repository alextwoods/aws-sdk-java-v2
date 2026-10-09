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

package software.amazon.awssdk.test.fault;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.http.HttpPhase;
import software.amazon.awssdk.http.HttpTransportMode;

/**
 * A generated fault scenario of Property 63 in symbolic form: the operation, the driver and, per transmission, the
 * fault's documented meaning ({@link Wire}), from which both the kit's {@link FaultTrace} (the wire bytes the
 * pipeline sees) and the {@link PipelineStateOracle}'s prediction are derived.
 */
record Scenario(Op op, HttpTransportMode mode, List<Step> transmissions, List<IdentityStep> identities,
                List<SignerStep> signings, EndpointMode endpoint, BodyKind body, CancelPoint cancel,
                RetrySettings retry) {
    Scenario {
        transmissions = List.copyOf(transmissions);
        identities = List.copyOf(identities);
        signings = List.copyOf(signings);
    }

    /** The operation a scenario calls (see {@link ServiceFixtures}). */
    enum Op {
        /** DynamoDB ListTables (awsJson1_0): finite request and response, modeled and unmodeled errors. */
        LIST_TABLES,
        /** MediaStore Data PutObject (restJson1): a streaming request body. */
        PUT_OBJECT,
        /** MediaStore Data GetObject (restJson1): a streaming output handed to a response transformer. */
        GET_OBJECT;

        boolean streamingInput() {
            return this == PUT_OBJECT;
        }
    }

    /** The documented meaning of one transmission's fault. */
    enum Wire {
        /** The operation's 2xx response. */
        OK,
        /** A 2xx whose body the protocol cannot read (ListTables only). */
        MALFORMED,
        /** A modeled 400 error, {@code ResourceNotFoundException} (ListTables only). */
        MODELED_ERROR,
        /** An unmodeled 400 error code (ListTables only). */
        UNMODELED_ERROR,
        /** HTTP 503. */
        UNAVAILABLE,
        /** A 400 with the throttling error code {@code ThrottlingException} (ListTables only). */
        THROTTLING_CODE,
        /** HTTP 429. */
        TOO_MANY_REQUESTS,
        /** The connection is refused. */
        CONNECT_FAILURE,
        /** A connect timeout (the request body is not read). */
        CONNECT_TIMEOUT,
        /** A read timeout after the request was sent. */
        READ_TIMEOUT,
        /** The transport fails after reading {@link Step#bytes()} request body bytes (PutObject only). */
        PARTIAL_REQUEST,
        /** The 2xx head and 10 body bytes, then a broken response body. */
        PARTIAL_RESPONSE,
        /** No response until cancelled. */
        HANG,
        /** The 2xx head and 5 body bytes, then nothing until cancelled. */
        HANG_BODY
    }

    /** One transmission: its fault, an {@code x-amz-retry-after} hint in milliseconds (or null) and a byte count. */
    record Step(Wire wire, Integer hintMillis, int bytes) {
        static Step of(Wire wire) {
            return new Step(wire, null, 0);
        }

        /** The kit's transport fault of this step for {@code op}. */
        TransportFault fault(Op op) {
            TransportFault.Respond ok = switch (op) {
                case LIST_TABLES -> ServiceFixtures.listTablesOk();
                case PUT_OBJECT -> ServiceFixtures.putObjectOk();
                case GET_OBJECT -> ServiceFixtures.getObjectOk();
            };
            return switch (wire) {
                case OK -> ok;
                case MALFORMED -> ServiceFixtures.malformedListTables();
                case MODELED_ERROR -> ServiceFixtures.resourceNotFound();
                case UNMODELED_ERROR -> ServiceFixtures.unmodeled400();
                case UNAVAILABLE -> hinted(ServiceFixtures.unavailable());
                case THROTTLING_CODE -> hinted(ServiceFixtures.throttled());
                case TOO_MANY_REQUESTS -> hinted(ServiceFixtures.tooManyRequests());
                case CONNECT_FAILURE -> TransportFault.connectFailure();
                case CONNECT_TIMEOUT -> TransportFault.timeout(HttpPhase.CONNECT);
                case READ_TIMEOUT -> TransportFault.timeout(HttpPhase.RESPONSE_CONSUMPTION);
                case PARTIAL_REQUEST -> TransportFault.partialRequest(bytes);
                case PARTIAL_RESPONSE -> TransportFault.partialResponse(ok, 10);
                case HANG -> TransportFault.hang();
                case HANG_BODY -> TransportFault.hangBody(ok, 5);
            };
        }

        private TransportFault.Respond hinted(TransportFault.Respond response) {
            return hintMillis == null ? response : response.withHeader("x-amz-retry-after", hintMillis.toString());
        }

        @Override
        public String toString() {
            return wire + (hintMillis == null ? "" : "(hint " + hintMillis + "ms)")
                   + (wire == Wire.PARTIAL_REQUEST ? "(" + bytes + "B)" : "");
        }
    }

    /** The kit trace of this scenario. */
    FaultTrace trace() {
        return new FaultTrace(transmissions.stream().map(step -> step.fault(op)).collect(Collectors.toList()),
                              identities, signings, endpoint, body, cancel, retry);
    }

    @Override
    public String toString() {
        return op + " on " + mode + ": tx=" + transmissions + " identity=" + identities + " signer=" + signings
               + " endpoint=" + endpoint + " body=" + body + " cancel=" + cancel + " retry=" + retry;
    }
}
