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
 * Root package of the {@code software.amazon.awssdk:checksums} V3 artifact.
 *
 * <p>Owns: checksum algorithms and the payload feature plugins of the AWS pipeline: request checksums
 * ({@link software.amazon.awssdk.checksums.RequestChecksumFeature}), response checksum validation
 * ({@link software.amazon.awssdk.checksums.ResponseChecksumFeature}) and request compression / content encoding
 * ({@link software.amazon.awssdk.checksums.compression.RequestCompressionFeature}). Each plugin is planned once per
 * client and operation from the operation's schemas and trait metadata, and applied per attempt to the serialized
 * HTTP request or received response, in the pipeline order compression, request checksum, signing, transmission,
 * response validation, deserialization. Model conflicts fail with
 * {@link software.amazon.awssdk.core.exception.ModelConflictException} before any transmission.
 *
 * <p>Allowed V3 module dependencies: {@code sdk-core}, {@code smithy-runtime}, {@code http-spi}. Forbidden
 * dependencies: generated service artifacts.
 */
package software.amazon.awssdk.checksums;
