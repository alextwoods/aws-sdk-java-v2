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

import java.util.Objects;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * A malformed event stream frame. The {@link #reason()} identifies the violated rule exactly; the message reports
 * lengths, CRCs, offsets and type codes only, never header or payload values.
 */
public final class EventStreamFormatException extends SdkClientException {
    private static final long serialVersionUID = 1L;

    /** The violated framing rule. */
    public enum Reason {
        /** The CRC32 of the 8-byte prelude does not match the prelude CRC. */
        PRELUDE_CRC_MISMATCH,
        /** The CRC32 of the message does not match the trailing message CRC. */
        MESSAGE_CRC_MISMATCH,
        /** The total length is below the 16-byte minimum. */
        INVALID_TOTAL_LENGTH,
        /** The headers length does not fit the total length or exceeds the 128 KiB header limit. */
        INVALID_HEADERS_LENGTH,
        /** The total length exceeds the decoder's maximum message size. */
        MESSAGE_TOO_LARGE,
        /** A header has an unknown type code. */
        INVALID_HEADER_TYPE,
        /** A header name is empty or not valid UTF-8. */
        INVALID_HEADER_NAME,
        /** A header string value is not valid UTF-8. */
        INVALID_HEADER_VALUE,
        /** A header runs past the end of the header section. */
        TRUNCATED_HEADER,
        /** Two headers have the same name. */
        DUPLICATE_HEADER,
        /** The stream ended inside a message. */
        TRUNCATED_MESSAGE
    }

    private final Reason reason;

    private EventStreamFormatException(BuilderImpl builder) {
        super(builder);
        this.reason = builder.reason;
    }

    /** A malformed-frame failure for {@code reason}. */
    public static EventStreamFormatException create(Reason reason, String message) {
        BuilderImpl builder = new BuilderImpl();
        builder.reason = Objects.requireNonNull(reason, "reason");
        builder.message("Malformed event stream message (" + reason + "): " + message);
        return builder.build();
    }

    /** The violated framing rule. */
    public Reason reason() {
        return reason;
    }

    @Override
    public Builder toBuilder() {
        return new BuilderImpl(this);
    }

    private static final class BuilderImpl extends SdkClientException.BuilderImpl {
        private Reason reason;

        BuilderImpl() {
        }

        BuilderImpl(EventStreamFormatException ex) {
            super(ex);
            this.reason = ex.reason;
        }

        @Override
        public EventStreamFormatException build() {
            return new EventStreamFormatException(this);
        }
    }
}
