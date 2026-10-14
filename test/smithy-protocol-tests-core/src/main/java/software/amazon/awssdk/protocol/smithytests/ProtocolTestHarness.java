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

package software.amazon.awssdk.protocol.smithytests;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runs one generated protocol test case against a client of type {@code C} without opening a network connection.
 *
 * <p>Generated request tests call {@link #captureRequest}; generated response tests call {@link #respondWith}. Each call
 * builds a fresh client configured for the test: the given endpoint, plus the region, credentials and no-retry settings
 * the generator passes to {@link ProtocolTestHarnesses#forClient}. Generated request test classes fix the idempotency
 * token themselves. Generated classes obtain a harness through {@link ProtocolTestHarnesses}; the transport-backed
 * implementation is {@link TransportProtocolTestHarnessFactory} (a {@link CapturingClientTransport} for requests and a
 * {@link FixedResponseTransport} for responses).
 *
 * <p>Example, as emitted by the protocol test generator:
 * <pre>{@code
 * CapturedRequest r = harness.captureRequest(c -> c.httpPayloadTraits(input), "https://example.com");
 * ProtocolAsserts.method(r, "POST");
 *
 * HttpPayloadTraitsResponse out = harness.respondWith(response, c -> c.httpPayloadTraits(input));
 * ProtocolAsserts.outputEquals(expected, out);
 * }</pre>
 *
 * @param <C> the client type the harness builds
 */
public interface ProtocolTestHarness<C> {

    /**
     * Build a client whose endpoint is {@code endpoint}, invoke {@code call} with it, and return the request the client
     * handed to its transport. The transport records the request and stops the call; no response is processed.
     *
     * @param call     invokes exactly one operation on the client
     * @param endpoint the client endpoint, built from the test case {@code host} (for example {@code https://example.com})
     * @return the request as it reached the transport boundary
     * @throws AssertionError if {@code call} completes without the client sending a request
     */
    CapturedRequest captureRequest(Consumer<? super C> call, String endpoint);

    /**
     * Build a client whose transport answers every request with {@code response}, invoke {@code call} with it, and return
     * its result. When the response is a modeled error, the client's exception propagates to the caller unchanged.
     *
     * @param response the status code, headers and body from the test case
     * @param call     invokes exactly one operation on the client and returns its output
     * @param <T>      the output type
     * @return the deserialized output
     */
    <T> T respondWith(StubbedResponse response, Function<? super C, ? extends T> call);
}
