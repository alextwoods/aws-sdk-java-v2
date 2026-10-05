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

/** The absolute Smithy shape IDs of the auth traits of the launch baseline's Modern Auth schemes. */
public final class AuthSchemeIds {
    /** {@code aws.auth#sigv4}: AWS Signature Version 4. */
    public static final String SIGV4 = "aws.auth#sigv4";
    /** {@code aws.auth#sigv4a}: AWS Signature Version 4a (multi-region, ECDSA). */
    public static final String SIGV4A = "aws.auth#sigv4a";
    /** {@code aws.auth#sigv4-s3express}: SigV4 with S3 Express One Zone session credentials. */
    public static final String SIGV4_S3EXPRESS = "aws.auth#sigv4-s3express";
    /** {@code smithy.api#httpBearerAuth}: an HTTP bearer token. */
    public static final String HTTP_BEARER = "smithy.api#httpBearerAuth";
    /** {@code smithy.api#noAuth}: anonymous; the request is not signed. */
    public static final String NO_AUTH = "smithy.api#noAuth";

    private AuthSchemeIds() {
    }
}
