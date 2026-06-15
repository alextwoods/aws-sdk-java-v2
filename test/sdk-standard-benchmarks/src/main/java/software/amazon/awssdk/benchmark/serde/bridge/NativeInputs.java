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

package software.amazon.awssdk.benchmark.serde.bridge;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.benchmark.serde.BenchmarkTestCaseLoader;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.protocols.json.internal.AwsStructuredPlainJsonFactory;
import software.amazon.awssdk.protocols.json.internal.marshall.JsonProtocolMarshallerBuilder;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.json.JsonCodec;

/**
 * Builds native input structs for the benchmarks. The Smithy data model is protocol-agnostic, so we
 * produce a single CLEAN JSON document from v2's own JSON marshaller (which omits empty
 * auto-construct collections — important for union members like DynamoDB AttributeValue) and
 * deserialize it into the native builder with the JSON codec, regardless of the benchmark's target
 * serialization protocol. The resulting native struct then serializes via the target protocol.
 */
final class NativeInputs {

    private static final String JSON_CT = "application/x-amz-json-1.0";
    private static final JsonCodec JSON_CODEC = JsonCodec.builder().build();

    private NativeInputs() {
    }

    /** v2-marshaller clean JSON bytes for a request POJO (protocol-agnostic data document). */
    static byte[] cleanJson(NativeProtocols.Spec spec, SdkPojo v2Request, IntermediateModel im,
                            BenchmarkTestCaseLoader.MarshallTestCase tc) throws Exception {
        OperationInfo info = BenchmarkTestCaseLoader.buildOperationInfo(im, tc);
        ProtocolMarshaller<SdkHttpFullRequest> m = JsonProtocolMarshallerBuilder.<SdkHttpFullRequest>create()
                .endpoint(URI.create("http://localhost/"))
                .jsonGenerator(AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY.createWriter(JSON_CT))
                .contentType(JSON_CT)
                .operationInfo(info)
                .sendExplicitNullForPayload(false)
                .protocolMetadata(AwsJsonProtocolMetadata.builder()
                        .protocol(AwsJsonProtocol.AWS_JSON).contentType(JSON_CT).build())
                .build();
        ContentStreamProvider body = m.marshall(v2Request).contentStreamProvider().orElse(null);
        if (body == null) {
            return "{}".getBytes(StandardCharsets.UTF_8);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        InputStream in = body.newStream();
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.size() == 0 ? "{}".getBytes(StandardCharsets.UTF_8) : out.toByteArray();
    }

    /** Deserialize a clean JSON document into the native builder. */
    static SerializableStruct deserialize(NativeProtocols.Spec spec, byte[] cleanJson,
                                          ShapeBuilder<? extends SerializableStruct> builder) {
        return JSON_CODEC.deserializeShape(cleanJson, builder);
    }

    /**
     * JSON carrying ALL members of the input (including @httpLabel/@httpHeader-bound ones) — produced
     * by the bridge over the input schema, which writes every member to the body regardless of HTTP
     * binding. Used for binding protocols (restJson/restXml/query) where the path/header members must
     * be present in the native struct so binding-time serialization has them.
     */
    static byte[] allMembersJson(software.amazon.smithy.java.core.schema.Schema inputSchema,
                                 SdkPojo v2Request) {
        java.nio.ByteBuffer bb = JSON_CODEC.serialize(
                software.amazon.awssdk.bridge.smithyjava.serde.BridgeStruct.of(inputSchema, v2Request));
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return b;
    }
}
