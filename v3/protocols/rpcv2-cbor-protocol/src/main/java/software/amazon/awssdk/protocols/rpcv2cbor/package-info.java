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
 * Root package of the {@code software.amazon.awssdk:rpcv2-cbor-protocol} V3 artifact.
 *
 * <p>Owns: The schema-driven Smithy RPC v2 CBOR protocol implementation, {@code smithy.protocols#rpcv2Cbor}
 * ({@link software.amazon.awssdk.protocols.rpcv2cbor.Rpcv2CborProtocolProvider}, registered as a
 * {@code ProtocolProvider} service entry), over Smithy Java's {@code Rpcv2CborCodec}. The protocol has no streaming blob
 * payloads, so streaming blob members are rejected; event streams are not yet supported (task 4.3 phase B).
 *
 * <p>Allowed V3 module dependencies: {@code protocol-core}, {@code smithy-runtime}, {@code http-spi},
 * {@code aws-core}. Forbidden dependencies: per-service generated marshallers.
 */
package software.amazon.awssdk.protocols.rpcv2cbor;
