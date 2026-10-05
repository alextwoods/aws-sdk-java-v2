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

package software.amazon.awssdk.pipeline;

import software.amazon.awssdk.auth.api.EventStreamSigningContext;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.eventstream.EventStreamSigner;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.protocols.core.eventstream.EventStreamRequestBody;

/**
 * The {@link PipelineStage#SIGN} step for an input event stream (Requirement 13.10): attaches the event stream signer
 * of the signed request's {@link EventStreamSigningContext} to its {@link EventStreamRequestBody}, so every outgoing
 * frame is chain-signed from the request signature and the stream ends with the signed empty frame.
 *
 * <p>Loaded only for a request whose body is an {@link EventStreamRequestBody}, which implies the optional
 * {@code eventstream} module is present, so the pipeline runs without that module otherwise.
 */
final class EventStreamFrameSigning {
    private EventStreamFrameSigning() {
    }

    /**
     * {@code signed} with its event stream body chain-signed, or {@code signed} itself when the signer returned no
     * event stream context (an unsigned or bearer-auth call keeps unsigned frames).
     */
    static SignedRequest apply(SignedRequest signed, EventStreamRequestBody body) {
        EventStreamSigningContext context = signed.eventStreamSigningContext().orElse(null);
        if (context == null) {
            return signed;
        }
        if (!EventStreamSigningContext.SIGV4_PAYLOAD.equals(context.algorithm())) {
            throw SdkClientException.create("Unsupported event stream frame signature algorithm "
                                            + context.algorithm());
        }
        EventStreamSigner signer = EventStreamSigner.sigV4(signed.signature(), context.region(), context.service(),
                                                           context.signingKeys()::apply, context.frameTime());
        HttpBody framed = HttpBody.streaming(body.withFrameTransformer(signer));
        return signed.withRequest(signed.request().toBuilder().body(framed).build());
    }
}
