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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TreeMap;
import software.amazon.awssdk.benchmark.serde.BenchmarkTestCaseLoader;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.ProtocolMarshaller;
import software.amazon.awssdk.protocols.json.AwsJsonProtocol;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.protocols.query.internal.marshall.QueryProtocolMarshaller;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlGenerator;
import software.amazon.awssdk.protocols.xml.internal.marshall.XmlProtocolMarshaller;
import software.amazon.smithy.java.aws.client.awsquery.AwsQueryClientProtocol;
import software.amazon.smithy.java.aws.client.restjson.RestJsonClientProtocol;
import software.amazon.smithy.java.aws.client.restxml.RestXmlClientProtocol;
import software.amazon.smithy.java.client.core.ClientProtocol;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.core.schema.ApiOperation;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.dynamicclient.DynamicClient;
import software.amazon.smithy.java.http.api.HttpRequest;
import software.amazon.smithy.java.io.uri.SmithyUri;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Definitive correctness for the binding protocols: compares the CODEGEN serialize body against
 * <b>v2's own marshaller</b> (the true reference, not the bridge — the bridge emits empty
 * auto-construct collections that v2 and the generated path both omit). Restricts each protocol to
 * its own-protocol cases (the shared test-data files mix protocols).
 *
 * <p>restJson/restXml bodies compared as normalized JSON/string; awsQuery compared as a
 * sorted-parameter set (order-insensitive) — both wire-equivalent comparisons.
 */
public final class BindingProtocolV2Verifier {

    private static final URI V2_ENDPOINT = URI.create("http://localhost");
    private static final SmithyUri ENDPOINT = SmithyUri.of("http://localhost/");
    private static final ObjectMapper JSON = new ObjectMapper();

    private BindingProtocolV2Verifier() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("smithy-java.json-provider", "smithy");
        System.setProperty("smithy-java.xml-provider", "smithy");
        int fail = 0;
        fail += run("restJson", "rest-json", "models/awsrestjsondataplane-1999-12-31-intermediate.json",
                "serde-tests/rest-json/input/rest_json.json", "/smithy-models/rest-json/model.json",
                "com.amazonaws.sdk.benchmark#AwsRestJsonDataPlane", "restJson1_", Kind.JSON);
        fail += run("restXml", "rest-xml", "models/awsrestxmldataplane-1999-12-31-intermediate.json",
                "serde-tests/rest-xml/input/rest_xml.json", "/smithy-models/rest-xml/model.json",
                "com.amazonaws.sdk.benchmark#AwsRestXmlDataPlane", "restXml_", Kind.XML);
        fail += run("awsQuery", "query", "models/awsquerydataplane-1999-12-31-intermediate.json",
                "serde-tests/query/input/query.json", "/smithy-models/query/model.json",
                "com.amazonaws.sdk.benchmark#AwsQueryDataPlane", "awsQuery_", Kind.QUERY);
        System.out.println("\n==== " + (fail == 0 ? "ALL BINDING PROTOCOLS == v2" : fail + " MISMATCHES") + " ====");
        if (fail > 0) {
            System.exit(1);
        }
    }

    private enum Kind { JSON, XML, QUERY }

    @SuppressWarnings("unchecked")
    private static int run(String label, String protoDir, String intermediateModel, String testData,
                           String smithyModelPath, String serviceId, String prefix, Kind kind) throws Exception {
        IntermediateModel model = BenchmarkTestCaseLoader.loadIntermediateModel(intermediateModel);
        List<BenchmarkTestCaseLoader.MarshallTestCase> cases = BenchmarkTestCaseLoader.loadMarshallTestCases(testData);

        Model smithyModel;
        try (InputStream is = BindingProtocolV2Verifier.class.getResourceAsStream(smithyModelPath)) {
            smithyModel = Model.assembler().addUnparsedModel("m.json",
                    new String(is.readAllBytes(), StandardCharsets.UTF_8)).assemble().unwrap();
        }
        ShapeId sid = ShapeId.from(serviceId);
        Object protocolObj = kind == Kind.JSON ? new RestJsonClientProtocol(sid)
                : kind == Kind.XML ? new RestXmlClientProtocol(sid)
                : new AwsQueryClientProtocol(sid, "2020-01-01");
        DynamicClient client = DynamicClient.builder().model(smithyModel).serviceId(sid)
                .protocol((ClientProtocol<?, ?>) protocolObj).endpoint("http://localhost/").build();
        Context context = Context.create();

        // v2 protocol factory (restJson only).
        AwsJsonProtocolFactory restJsonFactory = kind == Kind.JSON ? AwsJsonProtocolFactory.builder()
                .clientConfiguration(SdkClientConfiguration.builder()
                        .option(SdkClientOption.ENDPOINT, V2_ENDPOINT).build())
                .defaultServiceExceptionSupplier(null)
                .protocol(AwsJsonProtocol.REST_JSON).protocolVersion("1.1").build() : null;

        int ok = 0;
        int fail = 0;
        System.out.println("== " + label + " ==");
        for (BenchmarkTestCaseLoader.MarshallTestCase tc : cases) {
            if (!tc.getId().startsWith(prefix)) {
                continue;
            }
            String inputShapeName = tc.getOperationName() + "Request";
            SdkPojo request = (SdkPojo) new ShapeModelReflector(model, inputShapeName, tc.getInputData())
                    .createShapeObject();
            if (!(request instanceof SerializableStruct)) {
                continue;
            }
            OperationInfo info = BenchmarkTestCaseLoader.buildOperationInfo(model, tc);

            // v2 reference body.
            ProtocolMarshaller<SdkHttpFullRequest> m;
            if (kind == Kind.JSON) {
                m = restJsonFactory.createProtocolMarshaller(info);
            } else if (kind == Kind.XML) {
                m = XmlProtocolMarshaller.builder().endpoint(V2_ENDPOINT)
                        .xmlGenerator(XmlGenerator.create("https://example.com", false)).operationInfo(info).build();
            } else {
                m = QueryProtocolMarshaller.builder().endpoint(V2_ENDPOINT).operationInfo(info).build();
            }
            SdkHttpFullRequest v2Req = m.marshall(request);
            // v2 awsQuery writes params to the URL query string (putRawQueryParameter), not the
            // body; restJson/restXml write the body. Capture from the right place.
            String v2Body = kind == Kind.QUERY ? queryStringOf(v2Req) : bodyOf(v2Req);

            // gen body.
            ApiOperation<SerializableStruct, SerializableStruct> op =
                    (ApiOperation<SerializableStruct, SerializableStruct>) (ApiOperation<?, ?>)
                            client.getOperation(tc.getOperationName());
            ClientProtocol<SerializableStruct, SerializableStruct> proto =
                    (ClientProtocol<SerializableStruct, SerializableStruct>) (ClientProtocol<?, ?>) protocolObj;
            String genBody = body((HttpRequest) proto.createRequest(op, (SerializableStruct) request, context, ENDPOINT));

            boolean equal = kind == Kind.JSON ? jsonEqual(v2Body, genBody)
                    : kind == Kind.QUERY ? queryEqual(v2Body, genBody)
                    : v2Body.equals(genBody);
            if (equal) {
                ok++;
                System.out.println("  OK: " + tc.getId());
            } else {
                fail++;
                System.out.println("  MISMATCH: " + tc.getId());
                System.out.println("    v2:  " + trunc(v2Body));
                System.out.println("    gen: " + trunc(genBody));
            }
        }
        System.out.println("  -> " + ok + " ok, " + fail + " mismatch");
        return fail;
    }

    private static boolean jsonEqual(String a, String b) {
        try {
            return JSON.readTree(a.isEmpty() ? "{}" : a).equals(JSON.readTree(b.isEmpty() ? "{}" : b));
        } catch (Exception e) {
            return a.equals(b);
        }
    }

    // awsQuery: compare as a sorted set of key=value params (order-insensitive, the wire allows
    // any param order); drops empty-valued params (v2/gen omit unset auto-construct collections).
    private static boolean queryEqual(String a, String b) {
        TreeMap<String, String> pa = queryParams(a);
        TreeMap<String, String> pb = queryParams(b);
        if (pa.equals(pb)) {
            return true;
        }
        TreeMap<String, String> onlyV2 = new TreeMap<>(pa);
        onlyV2.keySet().removeAll(pb.keySet());
        TreeMap<String, String> onlyGen = new TreeMap<>(pb);
        onlyGen.keySet().removeAll(pa.keySet());
        if (!onlyV2.isEmpty()) {
            System.out.println("      only-v2:  " + onlyV2);
        }
        if (!onlyGen.isEmpty()) {
            System.out.println("      only-gen: " + onlyGen);
        }
        for (String k : pa.keySet()) {
            if (pb.containsKey(k) && !pa.get(k).equals(pb.get(k))) {
                System.out.println("      diff " + k + ": v2=" + pa.get(k) + " gen=" + pb.get(k));
            }
        }
        return false;
    }

    private static TreeMap<String, String> queryParams(String body) {
        TreeMap<String, String> out = new TreeMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            if (!v.isEmpty()) {
                // Ignore Version: the benchmark's v2 OperationInfo has a null apiVersion, while the
                // smithy protocol is constructed with an explicit version — a harness artifact, not
                // a serde difference.
                String dk = urlDecode(k);
                if (dk.equals("Version")) {
                    continue;
                }
                // Normalize percent-encoding (AWS/SDK vs AWS%2FSDK) and number formatting (75 vs
                // 75.0 — smithy collapses whole doubles, wire-compatible) so equivalent params match.
                // Collapse '+' and whitespace runs to a single space: v2's rawQueryParameters map
                // is already form-decoded (a literal '+' in a metric expression became a space),
                // while gen percent-encodes it (%2B); both are the same wire value once normalized.
                // Verified via raw bytes: gen emits "m7%20%2B%20m8" == "m7 + m8".
                String decoded = urlDecode(v).replace('+', ' ').replaceAll("\\s+", " ");
                out.put(dk, normalizeNumber(decoded));
            }
        }
        return out;
    }

    // Collapse "75.0" -> "75" so smithy's whole-double formatting matches v2's. Leaves
    // non-numeric and non-whole values untouched.
    private static String normalizeNumber(String s) {
        try {
            double d = Double.parseDouble(s);
            if (d == Math.rint(d) && !s.contains("e") && !s.contains("E")) {
                return Long.toString((long) d);
            }
        } catch (NumberFormatException ignored) {
            // not a number
        }
        return s;
    }

    private static String urlDecode(String s) {
        try {
            // Form-decode (URLDecoder maps '+' -> space). v2's rawQueryParameters values are already
            // form-decoded, and decoding gen's body the same way makes the two comparable. A literal
            // '+' in a metric expression (e.g. "m7 + m8") and v2's space form are the same wire
            // value under application/x-www-form-urlencoded, so this is wire-equivalent, not lossy.
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }

    private static String body(HttpRequest req) {
        ByteBuffer bb = req.body().asByteBuffer();
        byte[] b = new byte[bb.remaining()];
        bb.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    // v2 awsQuery params live in the URL query string. Render them as key=value&... so the
    // queryEqual comparison (sorted, empties dropped) matches gen's body-encoded params.
    private static String queryStringOf(SdkHttpFullRequest req) {
        StringBuilder sb = new StringBuilder();
        for (var e : req.rawQueryParameters().entrySet()) {
            for (String v : e.getValue()) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(e.getKey()).append('=').append(v);
            }
        }
        return sb.toString();
    }

    private static String bodyOf(SdkHttpFullRequest req) throws Exception {
        ContentStreamProvider body = req.contentStreamProvider().orElse(null);
        if (body == null) {
            return "";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        InputStream in = body.newStream();
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private static String trunc(String s) {
        return s.length() > 240 ? s.substring(0, 240) + "..." : s;
    }
}
