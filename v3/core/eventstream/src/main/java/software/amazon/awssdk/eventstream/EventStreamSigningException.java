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

package software.amazon.awssdk.eventstream;

import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * An outgoing event stream frame could not be signed: the signing key or signature could not be computed, or the
 * frame cannot be carried in a signed envelope. The signature chain is broken, so the {@link EventStreamSigner} that
 * threw it signs nothing further and the request event stream fails. The message never contains key material,
 * signatures, header values or payload bytes.
 */
public final class EventStreamSigningException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    private EventStreamSigningException(BuilderImpl builder) {
        super(builder);
    }

    /** A signing failure with {@code message} and {@code cause} (either may be {@code null}). */
    public static EventStreamSigningException create(String message, Throwable cause) {
        BuilderImpl builder = new BuilderImpl();
        builder.message(message);
        builder.cause(cause);
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl {
        BuilderImpl() {
        }

        BuilderImpl(EventStreamSigningException ex) {
            super(ex);
        }

        @Override
        public EventStreamSigningException build() {
            return new EventStreamSigningException(this);
        }
    }
}
