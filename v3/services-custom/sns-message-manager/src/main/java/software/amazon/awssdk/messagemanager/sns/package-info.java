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
 * Root package of the {@code software.amazon.awssdk:sns-message-manager} V3 artifact.
 *
 * <p>Owns: The SNS Message Manager high-level library: parsing SNS HTTP/S endpoint messages and verifying their
 * signatures against the regional SNS signing certificate ({@link software.amazon.awssdk.messagemanager.sns.SnsMessageManager}).
 *
 * <p>Allowed V3 module dependencies: {@code aws-core}, {@code sdk-core}, {@code smithy-runtime}, {@code regions},
 * {@code endpoints} and {@code http-spi}; it uses only their public APIs. Forbidden dependencies: V2 and bridge
 * internals, generated service clients, and concrete HTTP implementations.
 */
package software.amazon.awssdk.messagemanager.sns;
