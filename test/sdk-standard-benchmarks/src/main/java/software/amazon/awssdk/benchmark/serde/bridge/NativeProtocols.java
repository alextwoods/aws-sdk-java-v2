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

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.smithy.java.aws.client.awsjson.AwsJson1Protocol;
import software.amazon.smithy.java.aws.client.awsquery.AwsQueryClientProtocol;
import software.amazon.smithy.java.aws.client.restjson.RestJsonClientProtocol;
import software.amazon.smithy.java.aws.client.restxml.RestXmlClientProtocol;
import software.amazon.smithy.java.client.core.ClientProtocol;
import software.amazon.smithy.java.client.rpcv2.RpcV2CborProtocol;
import software.amazon.smithy.model.shapes.ShapeId;

/** Per-protocol benchmark wiring for the native (smithy-from-C2J) serde benchmarks. */
final class NativeProtocols {

    enum Kind { AWS_JSON, REST_JSON, REST_XML, RPC_V2_CBOR, AWS_QUERY }

    static final class Spec {
        final String name;
        final Kind kind;
        final String intermediateModel;
        final String smithyModel;
        final String serviceId;
        final String nativePkg;
        final String inputData;
        final String outputData;
        final String v2ContentType;

        Spec(String name, Kind kind, String intermediateModel, String smithyModel, String serviceId,
             String nativePkg, String inputData, String outputData, String v2ContentType) {
            this.name = name;
            this.kind = kind;
            this.intermediateModel = intermediateModel;
            this.smithyModel = smithyModel;
            this.serviceId = serviceId;
            this.nativePkg = nativePkg;
            this.inputData = inputData;
            this.outputData = outputData;
            this.v2ContentType = v2ContentType;
        }

        ClientProtocol<?, ?> protocol(ShapeId serviceId) {
            switch (kind) {
                case AWS_JSON: return new AwsJson1Protocol(serviceId);
                case REST_JSON: return new RestJsonClientProtocol(serviceId);
                case REST_XML: return new RestXmlClientProtocol(serviceId);
                case RPC_V2_CBOR: return new RpcV2CborProtocol(serviceId);
                case AWS_QUERY: return new AwsQueryClientProtocol(serviceId, "2020-01-01");
                default: throw new IllegalStateException();
            }
        }
    }

    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();

    static {
        add(new Spec("awsJson", Kind.AWS_JSON,
                "models/awsjsonrpc10dataplane-1999-12-31-intermediate.json",
                "/smithy-models/json-rpc-1-0/model.json",
                "com.amazonaws.sdk.benchmark#AwsJsonRpc10DataPlane",
                "com.amazonaws.jsonrpc10dataplane.model.",
                "serde-tests/json-rpc-1-0/input/json_1_0.json",
                "serde-tests/json-rpc-1-0/output/json_1_0.json",
                "application/x-amz-json-1.0"));
        add(new Spec("restJson", Kind.REST_JSON,
                "models/awsrestjsondataplane-1999-12-31-intermediate.json",
                "/smithy-models/rest-json/model.json",
                "com.amazonaws.sdk.benchmark#AwsRestJsonDataPlane",
                "com.amazonaws.restjsondataplane.model.",
                "serde-tests/rest-json/input/rest_json.json",
                "serde-tests/rest-json/output/rest_json.json",
                "application/json"));
        add(new Spec("restXml", Kind.REST_XML,
                "models/awsrestxmldataplane-1999-12-31-intermediate.json",
                "/smithy-models/rest-xml/model.json",
                "com.amazonaws.sdk.benchmark#AwsRestXmlDataPlane",
                "com.amazonaws.restxmldataplane.model.",
                "serde-tests/rest-xml/input/rest_xml.json",
                "serde-tests/rest-xml/output/rest_xml.json",
                "application/xml"));
        add(new Spec("rpcv2Cbor", Kind.RPC_V2_CBOR,
                "models/smithyrpcv2cbordataplane-1999-12-31-intermediate.json",
                "/smithy-models/rpc-v2-cbor/model.json",
                "com.amazonaws.sdk.benchmark#SmithyRpcV2CborDataPlane",
                "com.amazonaws.rpcv2cbordataplane.model.",
                "serde-tests/rpc-v2-cbor/input/rpc_v2_cbor.json",
                "serde-tests/rpc-v2-cbor/output/rpc_v2_cbor.json",
                "application/cbor"));
        add(new Spec("awsQuery", Kind.AWS_QUERY,
                "models/awsquerydataplane-1999-12-31-intermediate.json",
                "/smithy-models/query/model.json",
                "com.amazonaws.sdk.benchmark#AwsQueryDataPlane",
                "com.amazonaws.querydataplane.model.",
                "serde-tests/query/input/query.json",
                "serde-tests/query/output/query.json",
                "text/xml"));
    }

    private static void add(Spec s) {
        SPECS.put(s.name, s);
    }

    static Spec byName(String name) {
        Spec s = SPECS.get(name);
        if (s == null) {
            throw new IllegalArgumentException("unknown protocol " + name);
        }
        return s;
    }

    private NativeProtocols() {
    }
}
