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

package software.amazon.awssdk.auth.api;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * The output of {@link HttpSigner#sign}, with the V2 name: the signed request and, for signers that produce one, the
 * request signature, which seeds chained signatures such as Event Stream frame signing (Requirement 13.10).
 *
 * @param request the signed request
 * @param signature the hex request signature, or null if the scheme has none
 * @param eventStreamSigning how to chain-sign the request's input event stream frames from {@code signature}, or
 *        null if the request is not a signed event stream
 */
public record SignedRequest(SdkHttpRequest request, String signature, EventStreamSigningContext eventStreamSigning) {
    public SignedRequest {
        Objects.requireNonNull(request, "request");
        if (eventStreamSigning != null && signature == null) {
            throw new IllegalArgumentException("A signed event stream needs the request signature as its seed");
        }
    }

    /** A signed request that is not a signed event stream. */
    public SignedRequest(SdkHttpRequest request, String signature) {
        this(request, signature, null);
    }

    /** A request signed without a signature (for example anonymous or bearer auth). */
    public static SignedRequest unsigned(SdkHttpRequest request) {
        return new SignedRequest(request, null);
    }

    /** The request signature, if the scheme produced one. */
    public Optional<String> signatureValue() {
        return Optional.ofNullable(signature);
    }

    /** The event stream frame signing context, if the request was signed as an event stream. */
    public Optional<EventStreamSigningContext> eventStreamSigningContext() {
        return Optional.ofNullable(eventStreamSigning);
    }

    /** This signed request with {@code replacement} as its request; the signature and its context are kept. */
    public SignedRequest withRequest(SdkHttpRequest replacement) {
        return new SignedRequest(replacement, signature, eventStreamSigning);
    }

    @Override
    public String toString() {
        return "SignedRequest[" + request + "]";
    }
}
