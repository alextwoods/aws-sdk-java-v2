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
 * Internal implementation of the {@code software.amazon.awssdk:aws-crt-client} V3 artifact: native resource
 * ownership, request/response adaptation, failure mapping and native availability. Not a public API.
 *
 * <p>Owns: The native sync and async AWS CRT implementations.
 *
 * <p>Allowed V3 module dependencies: {@code http-spi}. Forbidden dependencies: generated service artifacts.
 */
package software.amazon.awssdk.http.crt.internal;
