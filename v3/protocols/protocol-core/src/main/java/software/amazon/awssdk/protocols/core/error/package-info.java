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

/**
 * Error-response helpers shared by the protocol runtimes: wire error-code rules
 * ({@link software.amazon.awssdk.protocols.core.error.ErrorCodes}: type sanitization, {@code x-amzn-query-error},
 * request IDs) and the per-client modeled-error index
 * ({@link software.amazon.awssdk.protocols.core.error.ModeledErrors}), which resolves codes through the operation's
 * errors, the service's {@code SchemaTypeRegistry} and {@code awsQueryError} codes, and completes modeled or generic
 * {@code AwsServiceException}s with code, message, request IDs and status code.
 */
package software.amazon.awssdk.protocols.core.error;
