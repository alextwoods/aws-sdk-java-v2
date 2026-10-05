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
 * Root package of the {@code software.amazon.awssdk:rest-json-protocol} V3 artifact.
 *
 * <p>Owns: The schema-driven REST JSON protocol implementation: the {@code aws.protocols#restJson1} runtime
 * ({@link software.amazon.awssdk.protocols.restjson.RestJsonProtocolProvider}), registered as a
 * {@code software.amazon.awssdk.protocols.core.ProtocolProvider} service. HTTP bindings are read and written with
 * protocol-core's {@code HttpBindingWriter}/{@code HttpBindingReader}; bodies use Smithy Java's JSON codec. Streaming
 * blob payloads are passed through unbuffered ({@code StreamingPayloads}); event streams are not yet supported
 * (task 4.3 phase B).
 *
 * <p>Allowed V3 module dependencies: {@code protocol-core}. Forbidden dependencies: per-service generated
 * marshallers.
 */
package software.amazon.awssdk.protocols.restjson;
