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

package software.amazon.awssdk.dynamicclient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.core.client.protocol.ClientProtocol;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson11Protocol;
import software.amazon.awssdk.protocols.client.awsjson.AwsJson1Protocol;
import software.amazon.awssdk.protocols.client.awsquery.AwsQueryClientProtocol;
import software.amazon.awssdk.protocols.client.awsquery.Ec2QueryClientProtocol;
import software.amazon.awssdk.protocols.client.restjson.RestJsonClientProtocol;
import software.amazon.awssdk.protocols.client.restxml.RestXmlClientProtocol;
import software.amazon.awssdk.protocols.client.rpcv2.RpcV2CborProtocol;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * The Supported_Protocols, in the SDK's priority order, with the wire formats the Dynamic Client SEP behavioral tests
 * need: the protocol trait to put on the reference model's service, the response bodies of outputs and errors written
 * by hand (never by the SDK's serializers), and a reader of the top-level members of a request body.
 */
enum SprocketsProtocol {
    RPCV2_CBOR("smithy.protocols#rpcv2Cbor", RpcV2CborProtocol.class, Wire.CBOR, "application/cbor"),
    AWS_JSON_1_0("aws.protocols#awsJson1_0", AwsJson1Protocol.class, Wire.JSON, "application/x-amz-json-1.0"),
    AWS_JSON_1_1("aws.protocols#awsJson1_1", AwsJson11Protocol.class, Wire.JSON, "application/x-amz-json-1.1"),
    REST_JSON_1("aws.protocols#restJson1", RestJsonClientProtocol.class, Wire.JSON, "application/json"),
    REST_XML("aws.protocols#restXml", RestXmlClientProtocol.class, Wire.REST_XML, "application/xml"),
    AWS_QUERY("aws.protocols#awsQuery", AwsQueryClientProtocol.class, Wire.AWS_QUERY, "text/xml"),
    EC2_QUERY("aws.protocols#ec2Query", Ec2QueryClientProtocol.class, Wire.EC2_QUERY, "text/xml");

    static final String XML_NAMESPACE = "https://example.com/sprockets";
    static final String REQUEST_ID = "sprockets-request-1";

    private static final Pattern XML_LEAF = Pattern.compile("<([A-Za-z_][\\w.-]*)>([^<]*)</\\1>");

    private final String traitId;
    private final Class<? extends ClientProtocol<?, ?>> protocolClass;
    private final Wire wire;
    private final String contentType;

    SprocketsProtocol(String traitId, Class<? extends ClientProtocol<?, ?>> protocolClass, Wire wire,
                      String contentType) {
        this.traitId = traitId;
        this.protocolClass = protocolClass;
        this.wire = wire;
        this.contentType = contentType;
    }

    private enum Wire {
        CBOR, JSON, REST_XML, AWS_QUERY, EC2_QUERY
    }

    /**
     * The ShapeId of the protocol definition trait, for example {@code aws.protocols#awsJson1_0}.
     */
    String traitId() {
        return traitId;
    }

    Class<? extends ClientProtocol<?, ?>> protocolClass() {
        return protocolClass;
    }

    /**
     * Whether the protocol binds members to HTTP messages with the HTTP binding traits, so the reference model needs
     * {@code @http} on its operations and {@code @httpPayload} on its streaming members.
     */
    boolean httpBindings() {
        return this == REST_JSON_1 || this == REST_XML;
    }

    /**
     * Whether the protocol needs {@code @xmlNamespace} on the service.
     */
    boolean query() {
        return wire == Wire.AWS_QUERY || wire == Wire.EC2_QUERY;
    }

    /**
     * A 200 response whose output has these string members.
     */
    RecordingHttpClients.Stub output(String operation, String... namesAndValues) {
        switch (wire) {
            case CBOR:
                return RecordingHttpClients.Stub.of(200, TestCbor.textMap(namesAndValues),
                                                    "Content-Type", contentType, "smithy-protocol", "rpc-v2-cbor",
                                                    "x-amzn-RequestId", REQUEST_ID);
            case JSON:
                return RecordingHttpClients.Stub.of(200, json(namesAndValues), "Content-Type", contentType,
                                                    "x-amzn-RequestId", REQUEST_ID);
            case REST_XML:
                return xml(200, "<" + operation + "Output>" + xmlMembers(namesAndValues) + "</" + operation
                                + "Output>");
            case AWS_QUERY:
                return xml(200, "<" + operation + "Response xmlns=\"" + XML_NAMESPACE + "\"><" + operation + "Result>"
                                + xmlMembers(namesAndValues) + "</" + operation + "Result><ResponseMetadata><RequestId>"
                                + REQUEST_ID + "</RequestId></ResponseMetadata></" + operation + "Response>");
            case EC2_QUERY:
                return xml(200, "<" + operation + "Response xmlns=\"" + XML_NAMESPACE + "\"><requestId>" + REQUEST_ID
                                + "</requestId>" + xmlMembers(namesAndValues) + "</" + operation + "Response>");
            default:
                throw new IllegalStateException(wire.toString());
        }
    }

    /**
     * A 400 error response whose error type is {@code errorType} (the protocol's discriminator: {@code __type} in a
     * JSON or CBOR body, {@code <Code>} in an XML error), with these string members.
     */
    RecordingHttpClients.Stub error(String errorType, String... namesAndValues) {
        switch (wire) {
            case CBOR:
                String[] members = new String[namesAndValues.length + 2];
                members[0] = "__type";
                members[1] = errorType;
                System.arraycopy(namesAndValues, 0, members, 2, namesAndValues.length);
                return RecordingHttpClients.Stub.of(400, TestCbor.textMap(members), "Content-Type", contentType,
                                                    "smithy-protocol", "rpc-v2-cbor", "x-amzn-RequestId", REQUEST_ID);
            case JSON:
                Map<String, Node> body = new LinkedHashMap<>();
                body.put("__type", Node.from(errorType));
                for (int i = 0; i < namesAndValues.length; i += 2) {
                    body.put(namesAndValues[i], Node.from(namesAndValues[i + 1]));
                }
                return RecordingHttpClients.Stub.of(400, Node.printJson(objectNode(body)), "Content-Type",
                                                    contentType, "x-amzn-RequestId", REQUEST_ID);
            case REST_XML:
                return xml(400, "<ErrorResponse><Error><Type>Sender</Type><Code>" + errorType + "</Code>"
                                + xmlMembers(namesAndValues) + "</Error><RequestId>" + REQUEST_ID
                                + "</RequestId></ErrorResponse>");
            case AWS_QUERY:
                return xml(400, "<ErrorResponse xmlns=\"" + XML_NAMESPACE + "\"><Error><Type>Sender</Type><Code>"
                                + errorType + "</Code>" + xmlMembers(namesAndValues) + "</Error><RequestId>"
                                + REQUEST_ID + "</RequestId></ErrorResponse>");
            case EC2_QUERY:
                return xml(400, "<Response><Errors><Error><Code>" + errorType + "</Code>" + xmlMembers(namesAndValues)
                                + "</Error></Errors><RequestID>" + REQUEST_ID + "</RequestID></Response>");
            default:
                throw new IllegalStateException(wire.toString());
        }
    }

    /**
     * Reads the top-level members of a recorded request body by wire name: strings, and blobs as the protocol
     * encodes them ({@link #wireBlob}). The query protocols' {@code Action} and {@code Version} are left out.
     */
    Map<String, Object> requestMembers(RecordingHttpClients.Recorded request) {
        Map<String, Object> members = new LinkedHashMap<>();
        switch (wire) {
            case CBOR:
                if (request.bytes.length > 0) {
                    ((Map<?, ?>) TestCbor.decode(request.bytes)).forEach((k, v) -> members.put((String) k, v));
                }
                return members;
            case JSON:
                if (request.body.isEmpty()) {
                    return members;
                }
                Node.parse(request.body).expectObjectNode().getStringMap()
                    .forEach((k, v) -> members.put(k, v.isStringNode() ? v.expectStringNode().getValue() : v));
                return members;
            case REST_XML:
                Matcher leaf = XML_LEAF.matcher(request.body);
                while (leaf.find()) {
                    members.put(leaf.group(1), leaf.group(2));
                }
                return members;
            default:
                for (String pair : request.body.split("&")) {
                    int equals = pair.indexOf('=');
                    if (equals < 0) {
                        continue;
                    }
                    String name = decode(pair.substring(0, equals));
                    if (!"Action".equals(name) && !"Version".equals(name)) {
                        members.put(name, decode(pair.substring(equals + 1)));
                    }
                }
                return members;
        }
    }

    /**
     * The wire name of a top-level input member without naming traits: ec2Query capitalizes the first letter.
     */
    String wireName(String memberName) {
        return wire == Wire.EC2_QUERY ? Character.toUpperCase(memberName.charAt(0)) + memberName.substring(1)
                                      : memberName;
    }

    /**
     * A blob as {@link #requestMembers} reads it: a CBOR byte string, else the base64 text the protocol writes.
     */
    Object wireBlob(byte[] bytes) {
        return wire == Wire.CBOR ? ByteBuffer.wrap(bytes) : Base64.getEncoder().encodeToString(bytes);
    }

    private static RecordingHttpClients.Stub xml(int status, String body) {
        return RecordingHttpClients.Stub.of(status, body, "Content-Type", "text/xml", "x-amzn-RequestId", REQUEST_ID);
    }

    private static String json(String... namesAndValues) {
        Map<String, Node> members = new LinkedHashMap<>();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            members.put(namesAndValues[i], Node.from(namesAndValues[i + 1]));
        }
        return Node.printJson(objectNode(members));
    }

    private static ObjectNode objectNode(Map<String, Node> members) {
        ObjectNode.Builder builder = ObjectNode.builder();
        members.forEach(builder::withMember);
        return builder.build();
    }

    private static String xmlMembers(String... namesAndValues) {
        StringBuilder xml = new StringBuilder();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            xml.append('<').append(namesAndValues[i]).append('>').append(namesAndValues[i + 1])
               .append("</").append(namesAndValues[i]).append('>');
        }
        return xml.toString();
    }

    private static String decode(String text) {
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
