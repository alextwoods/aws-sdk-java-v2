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
 * Root package of the {@code software.amazon.awssdk:aws-json-protocol} V3 artifact.
 *
 * <p>Owns: The schema-driven AWS JSON protocol implementation: {@code awsJson1_0} and {@code awsJson1_1}
 * ({@link software.amazon.awssdk.protocols.awsjson.AwsJson10ProtocolProvider},
 * {@link software.amazon.awssdk.protocols.awsjson.AwsJson11ProtocolProvider}, registered as
 * {@code ProtocolProvider} service entries). It is the reference implementation of the protocol-core Schema SERDE
 * contract. The protocol has no HTTP payload binding, so streaming blob members are rejected; event streams are
 * not yet supported (task 4.3 phase B).
 *
 * <p>Allowed V3 module dependencies: {@code protocol-core}, {@code smithy-runtime}, {@code http-spi},
 * {@code aws-core}. Forbidden dependencies: per-service generated
 * marshallers.
 */
package software.amazon.awssdk.protocols.awsjson;
