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

package software.amazon.awssdk.protocols.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocols.core.InstalledProtocols;
import software.amazon.awssdk.protocols.core.ModeledProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolIds;
import software.amazon.awssdk.protocols.core.ProtocolSelectionException;
import software.amazon.awssdk.protocols.core.SdkProtocolException;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

/** Request, response and error fixtures of the awsQuery runtime. */
class AwsQueryProtocolTest {
    private static final String MODEL = """
        $version: "2"
        namespace example.widgets

        use aws.protocols#awsQuery
        use aws.protocols#awsQueryError

        @awsQuery
        @xmlNamespace(uri: "https://widgets.example.com/doc/")
        service Widgets {
            version: "2020-01-01"
            operations: [GetWidget, PutWidget, Upload]
        }

        operation GetWidget {
            input := {
                @required
                id: String
                note: String
                created: Timestamp
                tags: Tags
                attrs: Attrs
                @xmlFlattened
                flat: Tags
                ratio: Float
            }
            output := {
                id: String
                @xmlAttribute
                @xmlName("revision")
                revision: Integer
                created: Timestamp
                @timestampFormat("epoch-seconds")
                updated: Timestamp
                items: Items
                data: Blob
            }
            errors: [WidgetNotFound, Throttled]
        }

        @endpoint(hostPrefix: "{tenant}.data.")
        operation PutWidget {
            input := {
                @required
                @hostLabel
                tenant: String
            }
        }

        operation Upload {
            input := {
                @required
                body: Payload
            }
        }

        @streaming
        blob Payload

        list Tags {
            member: String
        }

        map Attrs {
            key: String
            value: String
        }

        list Items {
            member: Item
        }

        structure Item {
            size: Integer
        }

        @error("client")
        @awsQueryError(code: "AWS.Widgets.NotFound", httpResponseCode: 404)
        structure WidgetNotFound {
            Message: String
            widgetId: String
        }

        @error("server")
        structure Throttled {
            Message: String
        }
        """;

    private static final URI ENDPOINT = URI.create("https://widgets.example.com");
    private static final ShapeId GET_WIDGET = ShapeId.from("example.widgets#GetWidget");

    private static DynamicService service;

    @BeforeAll
    static void load() {
        Model model = Model.assembler(AwsQueryProtocolTest.class.getClassLoader())
                           .discoverModels(AwsQueryProtocolTest.class.getClassLoader())
                           .addUnparsedModel("widgets.smithy", MODEL)
                           .assemble()
                           .unwrap();
        service = new DynamicService(model, ShapeId.from("example.widgets#Widgets"));
    }

    private static OperationProtocol plan(String operation, boolean hostPrefixInjection) {
        OperationShape shape = service.operation(ShapeId.from("example.widgets#" + operation));
        return service.client(new AwsQueryProtocolProvider(), hostPrefixInjection).operation(service.schemas(shape));
    }

    private static StructDocument input(String operation, String params) {
        OperationShape shape = service.operation(ShapeId.from("example.widgets#" + operation));
        return service.value(service.model().expectShape(shape.getInputShape()), Node.parse(params));
    }

    private static StructDocument output(String body) {
        return plan("GetWidget", true).deserializeResponse(
            response(200, body), service.builder(service.model().expectShape(
                service.operation(GET_WIDGET).getOutputShape())));
    }

    private static SdkHttpResponse response(int status, String body, String... headers) {
        SdkHttpResponse.Builder builder = SdkHttpResponse.builder().statusCode(status).body(HttpBody.fromUtf8(body));
        for (int i = 0; i < headers.length; i += 2) {
            builder.addHeader(headers[i], headers[i + 1]);
        }
        return builder.build();
    }

    @Test
    void serializesFormBodyInMemberOrder() {
        SdkHttpRequest request = plan("GetWidget", true).serializeRequest(
            input("GetWidget", """
                {"id": "w 1/é", "created": 1.5, "tags": ["a", "b"], "attrs": {"k": "v"}, "flat": ["x"],
                 "ratio": 0.1}"""),
            URI.create("https://widgets.example.com:8443/base"));
        assertEquals("POST", request.method());
        assertEquals(8443, request.port());
        assertEquals("/base/", request.encodedPath());
        assertEquals(Optional.of("application/x-www-form-urlencoded"), request.headers().firstValue("Content-Type"));
        assertEquals("Action=GetWidget&Version=2020-01-01&id=w%201%2F%C3%A9&created=1970-01-01T00%3A00%3A01.500Z"
                     + "&tags.member.1=a&tags.member.2=b&attrs.entry.1.key=k&attrs.entry.1.value=v&flat.1=x"
                     + "&ratio=0.1",
                     request.body().asUtf8String());
    }

    @Test
    void emptyListIsSentAndEmptyMapIsOmitted() {
        SdkHttpRequest request = plan("GetWidget", true).serializeRequest(
            input("GetWidget", "{\"id\": \"w\", \"tags\": [], \"attrs\": {}}"), ENDPOINT);
        assertEquals("Action=GetWidget&Version=2020-01-01&id=w&tags=", request.body().asUtf8String());
    }

    @Test
    void appliesHostPrefixUnlessDisabled() {
        StructDocument input = input("PutWidget", "{\"tenant\": \"acme\"}");
        assertEquals("acme.data.widgets.example.com", plan("PutWidget", true).serializeRequest(input, ENDPOINT).host());
        assertEquals("widgets.example.com", plan("PutWidget", false).serializeRequest(input, ENDPOINT).host());
        assertEquals("Action=PutWidget&Version=2020-01-01&tenant=acme",
                     plan("PutWidget", true).serializeRequest(input, ENDPOINT).body().asUtf8String());
    }

    @Test
    void readsResultElementAttributesAndTimestamps() {
        StructDocument value = output("""
            <GetWidgetResponse xmlns="https://widgets.example.com/doc/">
              <GetWidgetResult revision="7">
                <id>w-1</id>
                <created>2020-01-02T03:04:05.123Z</created>
                <updated>512.035</updated>
                <items><member><size>1</size></member><member><size>2</size></member></items>
                <data>dmFsdWU=</data>
                <unknown>ignored</unknown>
              </GetWidgetResult>
              <ResponseMetadata><RequestId>req-1</RequestId></ResponseMetadata>
            </GetWidgetResponse>""");
        Schema schema = value.schema();
        assertEquals("w-1", value.getMemberValue(schema.member("id")));
        assertEquals(7, (Integer) value.getMemberValue(schema.member("revision")));
        assertEquals(Instant.parse("2020-01-02T03:04:05.123Z"), value.getMemberValue(schema.member("created")));
        assertEquals(Instant.ofEpochSecond(512, 35_000_000), value.getMemberValue(schema.member("updated")));
        assertEquals(Node.parse("{\"id\":\"w-1\",\"revision\":7,\"created\":1577934245.123,\"updated\":512.035,"
                                + "\"items\":[{\"size\":1},{\"size\":2}],\"data\":\"dmFsdWU=\"}"),
                     DynamicService.canonical(value));
    }

    @Test
    void emptyBodyOrMissingResultIsAnEmptyOutput() {
        assertEquals(Node.parse("{}"), DynamicService.canonical(output("")));
        assertEquals(Node.parse("{}"), DynamicService.canonical(output(
            "<GetWidgetResponse><ResponseMetadata><RequestId>r</RequestId></ResponseMetadata></GetWidgetResponse>")));
    }

    @Test
    void invalidWireInputIsLocatedWithoutWireValues() {
        SdkProtocolException e = assertThrows(SdkProtocolException.class, () -> output("""
            <GetWidgetResponse><GetWidgetResult>
              <items><member><size>1</size></member><member><size>secret-value</size></member></items>
            </GetWidgetResult></GetWidgetResponse>"""));
        assertEquals(ProtocolIds.AWS_QUERY, e.protocolId());
        assertEquals(GET_WIDGET, e.operationId());
        assertEquals(List.of("items", "[1]", "size"), e.memberPath());
        assertEquals(ShapeId.from("smithy.api#Integer"), e.shapeId());
        assertEquals(Optional.of("body"), e.wireLocation());
        assertFalse(e.getMessage().contains("secret-value"), e.getMessage());

        SdkProtocolException malformed = assertThrows(SdkProtocolException.class,
                                                      () -> output("<GetWidgetResponse><secret-value>"));
        assertEquals(List.of(), malformed.memberPath());
        assertFalse(malformed.getMessage().contains("secret-value"), malformed.getMessage());
    }

    @Test
    void rejectsDoctypeDeclarations() {
        String xxe = """
            <?xml version="1.0"?>
            <!DOCTYPE r [<!ENTITY x SYSTEM "file:///etc/passwd">]>
            <GetWidgetResponse><GetWidgetResult><id>&x;</id></GetWidgetResult></GetWidgetResponse>""";
        assertThrows(SdkProtocolException.class, () -> output(xxe));
    }

    @Test
    void mapsQueryErrorCodeFirstAndPrefersBodyRequestId() {
        AwsServiceException e = plan("GetWidget", true).deserializeError(response(404, """
            <ErrorResponse>
              <Error>
                <Type>Sender</Type>
                <Code>AWS.Widgets.NotFound</Code>
                <Message>gone</Message>
                <widgetId>w-1</widgetId>
              </Error>
              <RequestId>body-req</RequestId>
            </ErrorResponse>""", "x-amzn-RequestId", "header-req", "x-amz-id-2", "ext"));
        DynamicError error = assertInstanceOf(DynamicError.class, e);
        assertEquals(ShapeId.from("example.widgets#WidgetNotFound"), error.schema().id());
        assertEquals(404, error.statusCode());
        assertEquals("body-req", error.requestId());
        assertEquals("ext", error.extendedRequestId());
        assertEquals("AWS.Widgets.NotFound", error.awsErrorDetails().errorCode());
        assertEquals("gone", error.awsErrorDetails().errorMessage());
        assertEquals("Widgets", error.awsErrorDetails().serviceName());
        assertEquals("w-1", error.getMemberValue(error.schema().member("widgetId")));
    }

    @Test
    void mapsShapeNameAndFallsBackToHeaderRequestId() {
        AwsServiceException e = plan("GetWidget", true).deserializeError(response(
            500, "<ErrorResponse><Error><Code>Throttled</Code><Message>slow</Message></Error></ErrorResponse>",
            "x-amzn-RequestId", "header-req"));
        assertEquals(ShapeId.from("example.widgets#Throttled"), ((DynamicError) e).schema().id());
        assertEquals("header-req", e.requestId());
        assertEquals("slow", e.awsErrorDetails().errorMessage());
    }

    @Test
    void unknownOrUnparseableErrorIsGeneric() {
        AwsServiceException unknown = plan("GetWidget", true).deserializeError(response(
            503, "<ErrorResponse><Error><Code>Mystery</Code><Message>what</Message></Error>"
                 + "<RequestId>r-2</RequestId></ErrorResponse>"));
        assertEquals(AwsServiceException.class, unknown.getClass());
        assertEquals("Mystery", unknown.awsErrorDetails().errorCode());
        assertEquals("what", unknown.awsErrorDetails().errorMessage());
        assertEquals("r-2", unknown.requestId());
        assertEquals(503, unknown.statusCode());

        AwsServiceException html = plan("GetWidget", true).deserializeError(response(502, "<html><body>bad"));
        assertEquals(AwsServiceException.class, html.getClass());
        assertEquals(502, html.statusCode());
        AwsServiceException empty = plan("GetWidget", true).deserializeError(response(500, ""));
        assertEquals(AwsServiceException.class, empty.getClass());
    }

    @Test
    void streamingOperationsAreRejected() {
        OperationProtocol plan = plan("Upload", true);
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
            () -> plan.serializeRequest(input("GetWidget", "{\"id\": \"w\"}"), ENDPOINT));
        assertTrue(e.getMessage().contains("not support"), e.getMessage());
    }

    @Test
    void rejectsInputOfAnotherOperation() {
        assertThrows(IllegalArgumentException.class, () -> plan("GetWidget", true)
            .serializeRequest(input("PutWidget", "{\"tenant\":\"a\"}"), ENDPOINT));
    }

    @Test
    void requiresServiceVersionAtConstruction() {
        ServiceProtocols unversioned = ServiceProtocols.builder(Schema.createService(ShapeId.from("example#S")))
                                                       .protocol(ModeledProtocol.of(ProtocolIds.AWS_QUERY))
                                                       .build();
        ProtocolSelectionException e = assertThrows(ProtocolSelectionException.class,
            () -> new AwsQueryProtocolProvider().validate(unversioned, ModeledProtocol.of(ProtocolIds.AWS_QUERY)));
        assertEquals(ProtocolSelectionException.Reason.UNSUPPORTED_BY_RUNTIME, e.reason());
        new AwsQueryProtocolProvider().validate(service.protocols(), ModeledProtocol.of(ProtocolIds.AWS_QUERY));
    }

    @Test
    void isDiscoveredAsServiceEntry() {
        InstalledProtocols installed = InstalledProtocols.discover(AwsQueryProtocolTest.class.getClassLoader());
        assertInstanceOf(AwsQueryProtocolProvider.class, installed.provider(ProtocolIds.AWS_QUERY).orElseThrow());
        assertFalse(new AwsQueryProtocolProvider().usesHttpBindingTraits());
    }
}
