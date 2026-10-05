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

package software.amazon.awssdk.protocols.ec2query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
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
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

/** Request, response and error fixtures of the ec2Query runtime. */
class Ec2QueryProtocolTest {
    private static final String MODEL = """
        $version: "2"
        namespace example.ec2

        use aws.protocols#ec2Query
        use aws.protocols#ec2QueryName

        @ec2Query
        @xmlNamespace(uri: "https://ec2.example.com/doc/")
        service Compute {
            version: "2016-11-15"
            operations: [RunInstances]
        }

        operation RunInstances {
            input := {
                @ec2QueryName("ImageId")
                image: String
                @xmlName("clientToken")
                token: String
                count: Integer
                zones: Zones
                placement: Placement
                empty: Zones
            }
            output := {
                reservationId: String
                @xmlName("instancesSet")
                instances: Instances
            }
            errors: [InvalidAmi]
        }

        list Zones {
            @xmlName("item")
            member: String
        }

        structure Placement {
            @ec2QueryName("AZ")
            zone: String
            tenancy: String
        }

        list Instances {
            @xmlName("item")
            member: Instance
        }

        structure Instance {
            instanceId: String
        }

        @error("client")
        structure InvalidAmi {
            Message: String
        }
        """;

    private static final URI ENDPOINT = URI.create("https://ec2.example.com");

    private static DynamicService service;

    @BeforeAll
    static void load() {
        Model model = Model.assembler(Ec2QueryProtocolTest.class.getClassLoader())
                           .discoverModels(Ec2QueryProtocolTest.class.getClassLoader())
                           .addUnparsedModel("compute.smithy", MODEL)
                           .assemble()
                           .unwrap();
        service = new DynamicService(model, ShapeId.from("example.ec2#Compute"));
    }

    private static OperationProtocol plan() {
        OperationShape shape = service.operation(ShapeId.from("example.ec2#RunInstances"));
        return service.client(new Ec2QueryProtocolProvider(), true).operation(service.schemas(shape));
    }

    private static SdkHttpResponse response(int status, String body, String... headers) {
        SdkHttpResponse.Builder builder = SdkHttpResponse.builder().statusCode(status).body(HttpBody.fromUtf8(body));
        for (int i = 0; i < headers.length; i += 2) {
            builder.addHeader(headers[i], headers[i + 1]);
        }
        return builder.build();
    }

    @Test
    void serializesEc2KeysFlattenedListsAndOmitsEmptyLists() {
        OperationShape shape = service.operation(ShapeId.from("example.ec2#RunInstances"));
        StructDocument input = service.value(service.model().expectShape(shape.getInputShape()), Node.parse("""
            {"image": "ami-1", "token": "t", "count": 2, "zones": ["a", "b"],
             "placement": {"zone": "us-1a", "tenancy": "default"}, "empty": []}"""));
        SdkHttpRequest request = plan().serializeRequest(input, ENDPOINT);
        assertEquals(Optional.of("application/x-www-form-urlencoded"), request.headers().firstValue("Content-Type"));
        assertEquals("Action=RunInstances&Version=2016-11-15&ImageId=ami-1&ClientToken=t&Count=2&Zones.1=a&Zones.2=b"
                     + "&Placement.AZ=us-1a&Placement.Tenancy=default",
                     request.body().asUtf8String());
    }

    @Test
    void readsOutputWithoutResultWrapper() {
        OperationShape shape = service.operation(ShapeId.from("example.ec2#RunInstances"));
        StructDocument output = plan().deserializeResponse(response(200, """
            <RunInstancesResponse xmlns="https://ec2.example.com/doc/">
              <requestId>req</requestId>
              <reservationId>r-1</reservationId>
              <instancesSet><item><instanceId>i-1</instanceId></item></instancesSet>
            </RunInstancesResponse>"""), service.builder(service.model().expectShape(shape.getOutputShape())));
        assertEquals(Node.parse("{\"reservationId\":\"r-1\",\"instances\":[{\"instanceId\":\"i-1\"}]}"),
                     DynamicService.canonical(output));
    }

    @Test
    void readsEc2ErrorEnvelope() {
        AwsServiceException e = plan().deserializeError(response(400, """
            <Response><Errors><Error><Code>InvalidAmi</Code><Message>bad ami</Message></Error></Errors>
            <RequestID>ec2-req</RequestID></Response>""", "x-amzn-RequestId", "header-req"));
        DynamicError error = assertInstanceOf(DynamicError.class, e);
        assertEquals(ShapeId.from("example.ec2#InvalidAmi"), error.schema().id());
        assertEquals("ec2-req", error.requestId());
        assertEquals("InvalidAmi", error.awsErrorDetails().errorCode());
        assertEquals("bad ami", error.awsErrorDetails().errorMessage());
        assertEquals("bad ami", error.getMemberValue(error.schema().member("Message")));

        AwsServiceException generic = plan().deserializeError(response(
            500, "<Response><Errors><Error><Code>Unavailable</Code></Error></Errors></Response>"));
        assertEquals(AwsServiceException.class, generic.getClass());
        assertEquals("Unavailable", generic.awsErrorDetails().errorCode());
    }

    @Test
    void rulesUseRecordedEc2QueryNames() {
        ServiceProtocols protocols = service.protocols();
        assertEquals("ImageId", protocols.ec2QueryNames().get(ShapeId.from("example.ec2#RunInstancesInput$image")));
        assertEquals("Id", Ec2QueryFormRules.capitalize("id"));
        assertEquals("", Ec2QueryFormRules.capitalize(""));
        assertEquals("ABC", Ec2QueryFormRules.capitalize("ABC"));
    }

    @Test
    void requiresServiceVersionAndIsDiscovered() {
        ServiceProtocols unversioned = ServiceProtocols.builder(Schema.createService(ShapeId.from("example#S")))
                                                       .protocol(ModeledProtocol.of(ProtocolIds.EC2_QUERY))
                                                       .build();
        assertThrows(ProtocolSelectionException.class,
            () -> new Ec2QueryProtocolProvider().validate(unversioned, ModeledProtocol.of(ProtocolIds.EC2_QUERY)));
        InstalledProtocols installed = InstalledProtocols.discover(Ec2QueryProtocolTest.class.getClassLoader());
        assertInstanceOf(Ec2QueryProtocolProvider.class, installed.provider(ProtocolIds.EC2_QUERY).orElseThrow());
        assertFalse(new Ec2QueryProtocolProvider().usesHttpBindingTraits());
    }
}
