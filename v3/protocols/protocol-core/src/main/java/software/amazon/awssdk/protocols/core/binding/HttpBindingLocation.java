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

package software.amazon.awssdk.protocols.core.binding;

/** Where an HTTP-bound protocol places a structure member in an HTTP message. */
public enum HttpBindingLocation {
    /** A URI path label ({@code smithy.api#httpLabel}); requests only. */
    LABEL,
    /** A named query parameter ({@code smithy.api#httpQuery}); requests only. */
    QUERY,
    /** A map of query parameters ({@code smithy.api#httpQueryParams}); requests only. */
    QUERY_PARAMS,
    /** A named header ({@code smithy.api#httpHeader}). */
    HEADER,
    /** A map of headers sharing a prefix ({@code smithy.api#httpPrefixHeaders}). */
    PREFIX_HEADERS,
    /** The whole message body ({@code smithy.api#httpPayload}). */
    PAYLOAD,
    /** The HTTP status code ({@code smithy.api#httpResponseCode}); responses only. */
    STATUS_CODE,
    /** A member of the protocol-serialized body document (no binding trait, or one ignored in this direction). */
    BODY
}
