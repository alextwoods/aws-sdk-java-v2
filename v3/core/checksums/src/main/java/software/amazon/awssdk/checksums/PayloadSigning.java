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

package software.amazon.awssdk.checksums;

/**
 * How the selected auth scheme treats the request payload, which decides where a request checksum of a streaming
 * payload is placed (Smithy "Resolving checksum location"). A normal (non-streaming) payload always carries its
 * checksum in a header.
 */
public enum PayloadSigning {
    /** The signature covers the whole payload through a header ("header-based auth"): checksum in a header. */
    SIGNED,
    /** The payload is signed chunk by chunk ("streaming-signing auth"): checksum in the signed chunked trailer. */
    STREAMING_SIGNED,
    /** The payload is not signed ("unsigned payload auth", including anonymous): checksum in an unsigned trailer. */
    UNSIGNED
}
