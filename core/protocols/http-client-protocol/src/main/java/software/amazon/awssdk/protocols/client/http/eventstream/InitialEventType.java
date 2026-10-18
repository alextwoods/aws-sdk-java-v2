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

package software.amazon.awssdk.protocols.client.http.eventstream;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * The {@code :event-type} of the initial message of an event stream, which RPC protocols use to send the
 * non-stream members of the operation's input or output.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.events.InitialEventType}.
 */
@SdkPublicApi
public enum InitialEventType {
    /**
     * The initial message of an input event stream.
     */
    INITIAL_REQUEST("initial-request"),

    /**
     * The initial message of an output event stream.
     */
    INITIAL_RESPONSE("initial-response");

    private final String value;

    InitialEventType(String value) {
        this.value = value;
    }

    /**
     * Returns the {@code :event-type} header value.
     *
     * @return the header value
     */
    public String value() {
        return value;
    }
}
