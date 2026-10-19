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

package software.amazon.awssdk.testutils.mock;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.testutils.mock.internal.ResponseGenerators;

/**
 * Turns an operation output, or a modeled exception, into the HTTP response a service using one protocol would send.
 *
 * <p>Responses are built from Schemas, codecs and HTTP binding serde only, without per-shape marshallers. Deserializing a
 * generated response with the matching client protocol yields a value equal to the one given: the same output, or a
 * modeled exception of the same type with equal members, message and request ID.
 *
 * <p>There is one implementation per supported protocol: {@code restJson1}, {@code restXml}, {@code awsJson1_0},
 * {@code awsJson1_1}, {@code smithy.protocols#rpcv2Cbor}, {@code awsQuery} and {@code ec2Query}. They are found with
 * {@link java.util.ServiceLoader}, so another protocol can be added by registering an implementation in
 * {@code META-INF/services/software.amazon.awssdk.testutils.mock.ResponseGenerator}.
 *
 * <p>This is an SDK addition: smithy-java's mock plugin serializes responses with a server protocol from
 * {@code server-core}, which the SDK does not have.
 */
@SdkPublicApi
@ThreadSafe
public interface ResponseGenerator {

    /**
     * Returns the ID of the protocol trait this generator writes responses for, such as {@code aws.protocols#restJson1}.
     *
     * @return the protocol ID
     */
    ShapeId protocolId();

    /**
     * Generates the successful response of an operation.
     *
     * @param operation the operation whose output is serialized; its service schema supplies service-wide traits such
     *                  as {@code xmlNamespace}
     * @param output the output value, a generated output class or a schema-backed Document
     * @return the response, with status code, headers and body following the protocol and the output's HTTP bindings
     * @throws NullPointerException if an argument is null
     */
    SdkHttpFullResponse generateOutput(ApiOperation<?, ?> operation, SerializableStruct output);

    /**
     * Generates the error response of an operation for a modeled error.
     *
     * <p>The status code is the error's {@code httpError} code, or 400 for a client and 500 for a server error. The
     * error type is written where the protocol defines it. The message and request ID of an
     * {@link software.amazon.awssdk.core.exception.SdkServiceException} are written too. For a service with the
     * {@code awsQueryCompatible} trait, the {@code x-amzn-query-error} header carries the {@code awsQueryError} code,
     * or the shape name, and the fault.
     *
     * @param operation the operation that fails
     * @param modeledError the modeled error, whose schema has the {@code error} trait
     * @return the error response
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the schema of {@code modeledError} has no {@code error} trait
     */
    SdkHttpFullResponse generateError(ApiOperation<?, ?> operation, SerializableStruct modeledError);

    /**
     * Returns the generator for a protocol, loaded with {@link java.util.ServiceLoader} from the class loader of this
     * interface.
     *
     * @param protocolId the protocol trait ID, such as the {@code id()} of a client protocol
     * @return the generator
     * @throws NullPointerException if {@code protocolId} is null
     * @throws IllegalArgumentException if no generator is registered for the protocol
     */
    static ResponseGenerator forProtocol(ShapeId protocolId) {
        return ResponseGenerators.forProtocol(protocolId);
    }
}
