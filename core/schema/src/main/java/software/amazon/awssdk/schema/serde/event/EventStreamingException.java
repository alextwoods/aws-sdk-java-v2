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

package software.amazon.awssdk.schema.serde.event;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * Thrown when an event stream message has the message type {@code error}, with the message's {@code :error-code}
 * and {@code :error-message} headers.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.serde.event.EventStreamingException}.
 */
@SdkPublicApi
public final class EventStreamingException extends EventStreamingProtocolException {
    private static final long serialVersionUID = 1L;

    private final String errorCode;

    /**
     * @param errorCode the error code
     * @param errorMessage the error message
     * @throws NullPointerException if an argument is null
     */
    public EventStreamingException(String errorCode, String errorMessage) {
        super(Validate.paramNotNull(errorMessage, "errorMessage"));
        this.errorCode = Validate.paramNotNull(errorCode, "errorCode");
    }

    /**
     * Returns the error code of the exception.
     *
     * @return the error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}
