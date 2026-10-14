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

import software.amazon.awssdk.utils.Validate;

/**
 * Thrown by {@link CapturingClientTransport} once it has recorded a request, so that the call stops before any network
 * connection is opened and no response is processed.
 *
 * <p>It is neither an {@code SdkException} nor an {@code IOException}, so the SDK's retry strategies do not classify it
 * as retryable and the call is not retried. It has no stack trace: it marks the end of a test call, not a failure.
 */
public final class RequestCaptured extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final transient CapturedRequest request;

    RequestCaptured(CapturedRequest request) {
        super("Request captured by the protocol test transport: " + Validate.paramNotNull(request, "request"), null, false,
              false);
        this.request = request;
    }

    /**
     * The request that was captured.
     */
    public CapturedRequest request() {
        return request;
    }
}
