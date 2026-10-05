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

package software.amazon.awssdk.http;

/**
 * An HTTP protocol version a transport can negotiate (Requirements 12.2, 12.3). Same constants as the V2
 * {@code software.amazon.awssdk.http.Protocol}.
 */
public enum Protocol {
    /** HTTP/1.1: one exchange at a time per connection, request line target, {@code Host} header. */
    HTTP1_1,
    /** HTTP/2: multiplexed streams with flow control, pseudo-header fields ({@link SdkHttpRequest#pseudoHeaders()}). */
    HTTP2
}
