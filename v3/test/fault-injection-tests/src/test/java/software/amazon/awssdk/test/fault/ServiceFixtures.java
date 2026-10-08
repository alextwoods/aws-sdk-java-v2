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

package software.amazon.awssdk.test.fault;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.awscore.operation.AwsOperationDescriptor;
import software.amazon.awssdk.pipeline.AwsOperation;
import software.amazon.awssdk.pipeline.client.AwsServiceClientDefinition;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbServiceMetadata;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.mediastoredata.MediaStoreDataClient;
import software.amazon.awssdk.services.mediastoredata.MediaStoreDataServiceMetadata;
import software.amazon.awssdk.services.mediastoredata.model.GetObjectRequest;
import software.amazon.awssdk.services.mediastoredata.model.GetObjectResponse;
import software.amazon.awssdk.services.mediastoredata.model.PutObjectRequest;
import software.amazon.awssdk.services.mediastoredata.model.PutObjectResponse;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;

/**
 * The generated services the engine-level fault matrix runs through {@link FaultHarness}: their real protocols, type
 * registries, models and operation descriptors, bound with SigV4 auth as their generated bindings are, but without
 * their endpoint rules (the harness substitutes {@link FaultEndpoints}).
 *
 * <ul>
 *     <li>DynamoDB {@code ListTables} (awsJson1_0): finite request and response.</li>
 *     <li>MediaStore Data {@code PutObject} (restJson1, unsigned streaming payload): request body faults.</li>
 *     <li>MediaStore Data {@code GetObject} (restJson1, streaming output): response body faults after the payload is
 *     handed to a response transformer.</li>
 * </ul>
 */
final class ServiceFixtures {
    static final String REQUEST_ID = "fault-request-id";

    static final AwsServiceClientDefinition DYNAMODB = new AwsServiceClientDefinition(
        DynamoDbServiceMetadata.serviceId(), DynamoDbClient.SERVICE_NAME, DynamoDbServiceMetadata.serviceProtocols(),
        DynamoDbServiceMetadata.typeRegistry(), null);

    static final AwsServiceClientDefinition MEDIASTORE_DATA = new AwsServiceClientDefinition(
        MediaStoreDataServiceMetadata.serviceId(), MediaStoreDataClient.SERVICE_NAME,
        MediaStoreDataServiceMetadata.serviceProtocols(), MediaStoreDataServiceMetadata.typeRegistry(), null);

    static final AwsOperation<ListTablesRequest, ListTablesResponse> LIST_TABLES = operation(
        DynamoDbServiceMetadata.operations(), "ListTables", ListTablesRequest::builder, ListTablesResponse::builder,
        sigv4("com.amazonaws.dynamodb#ListTables", "dynamodb", false));

    static final AwsOperation<PutObjectRequest, PutObjectResponse> PUT_OBJECT = operation(
        MediaStoreDataServiceMetadata.operations(), "PutObject", PutObjectRequest::builder,
        PutObjectResponse::builder, sigv4("com.amazonaws.mediastoredata#PutObject", "mediastore", true));

    static final AwsOperation<GetObjectRequest, GetObjectResponse> GET_OBJECT = operation(
        MediaStoreDataServiceMetadata.operations(), "GetObject", GetObjectRequest::builder,
        GetObjectResponse::builder, sigv4("com.amazonaws.mediastoredata#GetObject", "mediastore", false));

    /** The bytes of a successful {@code GetObject}'s payload. */
    static final String OBJECT_PAYLOAD = "media-object-payload-0123456789-abcdefghijklmnopqrstuvwxyz";

    private ServiceFixtures() {
    }

    // ---- DynamoDB (awsJson1_0) wire ----

    static TransportFault.Respond dynamoDb(int status, String body) {
        return TransportFault.respond(status, body, "Content-Type", "application/x-amz-json-1.0",
                                      "x-amzn-RequestId", REQUEST_ID);
    }

    static TransportFault.Respond listTablesOk() {
        return dynamoDb(200, "{\"TableNames\":[\"fault-table\"]}");
    }

    static TransportFault.Respond dynamoDbError(int status, String type) {
        return dynamoDb(status, "{\"__type\":\"com.amazonaws.dynamodb#" + type + "\",\"message\":\"injected " + type
                                + "\"}");
    }

    static TransportFault.Respond resourceNotFound() {
        return dynamoDbError(400, "ResourceNotFoundException");
    }

    static TransportFault.Respond unmodeled400() {
        return dynamoDbError(400, "FaultInjectedMysteryError");
    }

    static TransportFault.Respond unavailable() {
        return dynamoDb(503, "<html>Service Unavailable</html>");
    }

    static TransportFault.Respond throttled() {
        return dynamoDbError(400, "ThrottlingException");
    }

    static TransportFault.Respond tooManyRequests() {
        return dynamoDb(429, "");
    }

    static TransportFault.Respond malformedListTables() {
        return dynamoDb(200, "{\"TableNames\":[\"fault-table\", ");
    }

    // ---- MediaStore Data (restJson1) wire ----

    static TransportFault.Respond putObjectOk() {
        return TransportFault.respond(200, "{\"ETag\":\"etag-1\",\"StorageClass\":\"TEMPORAL\"}",
                                      "Content-Type", "application/json", "x-amzn-RequestId", REQUEST_ID);
    }

    static TransportFault.Respond getObjectOk() {
        return TransportFault.respond(200, OBJECT_PAYLOAD, "Content-Type", "application/octet-stream",
                                      "Content-Length", Integer.toString(OBJECT_PAYLOAD.length()),
                                      "x-amzn-RequestId", REQUEST_ID);
    }

    static ListTablesRequest listTablesRequest() {
        return ListTablesRequest.builder().build();
    }

    static PutObjectRequest putObjectRequest() {
        return PutObjectRequest.builder().path("fault/object").build();
    }

    static GetObjectRequest getObjectRequest() {
        return GetObjectRequest.builder().path("fault/object").build();
    }

    private static ModeledAuth sigv4(String operationId, String signingName, boolean unsignedPayload) {
        ModeledAuth.Builder auth = ModeledAuth.builder().operationId(operationId)
                                              .addServiceScheme(FaultAuthScheme.SIGV4, Map.of("name", signingName));
        if (unsignedPayload) {
            auth.operationAuthTrait(List.of(FaultAuthScheme.SIGV4)).unsignedPayload(true);
        }
        return auth.build();
    }

    private static <I extends SerializableStruct, O extends SerializableStruct> AwsOperation<I, O> operation(
        List<AwsOperationDescriptor> operations, String name, Supplier<? extends ShapeBuilder<I>> input,
        Supplier<? extends ShapeBuilder<O>> output, ModeledAuth auth) {
        AwsOperationDescriptor descriptor = operations.stream().filter(d -> d.id().getName().equals(name))
                                                      .findFirst().orElseThrow();
        return AwsOperation.<I, O>builder(descriptor, input, output).auth(auth).build();
    }
}
