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

package software.amazon.awssdk.awscodegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.codegen.core.CodegenException;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

class AwsSymbolProviderTest {
    private static final String PACKAGE = "software.amazon.awssdk.services.weather";
    private static final String MODEL = """
        $version: "2"
        namespace example.weather

        service Weather {
            version: "2006-03-01"
            operations: [GetForecast, Ping]
            rename: { "example.other#Forecast": "OtherForecast" }
        }

        operation GetForecast {
            input := {
                city: String
                SSECustomerKey: String
                ID: Integer
                default: Boolean
                when: Timestamp
                data: Blob
                doc: Document
                items: Items
                tags: Tags
                kind: Kind
                level: Level
                choice: Choice
                big: BigDecimal
                other: example.other#Forecast
            }
            output := {
                forecast: Forecast
            }
            errors: [NotFound, WeatherFault, BadCode]
        }

        @error("client")
        structure BadCode {
            Message: Integer
        }

        operation Ping {
            input: Shared
            output: Shared
        }

        structure Shared {
            schema: String
            Builder: String
            type: String
        }

        union Choice2 { type: String, toBuilder: String }

        @error("server")
        structure WeatherFault {
            requestId: String
            message: String
        }

        structure Forecast {}
        list Items { member: String }
        map Tags { key: String, value: String }
        enum Kind { A, B }
        intEnum Level {
            LOW = 1
        }
        union Choice { a: String }
        @error("client")
        structure NotFound {}

        resource Station {}
        """;
    private static final String OTHER = """
        $version: "2"
        namespace example.other
        structure Forecast {}
        """;

    private static Model model;
    private static AwsSymbolProvider provider;

    @BeforeAll
    static void setUp() {
        model = Model.assembler().addUnparsedModel("weather.smithy", MODEL).addUnparsedModel("other.smithy", OTHER)
                     .assemble().unwrap();
        ServiceShape service = model.expectShape(ShapeId.from("example.weather#Weather"), ServiceShape.class);
        provider = new AwsSymbolProvider(model, service, new AwsCodegenSettings(service.getId(), PACKAGE));
    }

    @Test
    void serviceMapsToTheGeneratedIdentityType() {
        assertGenerated("example.weather#Weather", PACKAGE, "GeneratedServiceInfo");
    }

    @Test
    void aggregatesAndEnumsMapToTheModelPackage() {
        assertGenerated("example.weather#Forecast", PACKAGE + ".model", "Forecast");
        assertGenerated("example.weather#Kind", PACKAGE + ".model", "Kind");
        assertGenerated("example.weather#Level", PACKAGE + ".model", "Level");
        assertGenerated("example.weather#Choice", PACKAGE + ".model", "Choice");
        assertGenerated("example.weather#NotFound", PACKAGE + ".model", "NotFoundException");
        assertGenerated("example.weather#WeatherFault", PACKAGE + ".model", "DefaultWeatherException");
        assertGenerated("example.weather#GetForecastInput", PACKAGE + ".model", "GetForecastRequest");
        assertGenerated("example.weather#GetForecastOutput", PACKAGE + ".model", "GetForecastResponse");
        assertGenerated("example.weather#Shared", PACKAGE + ".model", "Shared");
        assertEquals("software.amazon.smithy.java.core.schema.Unit",
                     provider.toSymbol(model.expectShape(ShapeId.from("smithy.api#Unit"))).getFullName());
        ServiceShape service = model.expectShape(ShapeId.from("example.weather#Weather"), ServiceShape.class);
        assertEquals(PACKAGE + ".model.WeatherException",
                     AwsSymbolProvider.serviceException(service, new AwsCodegenSettings(service.getId(), PACKAGE))
                                      .getFullName());
    }

    @Test
    void renamedShapesUseTheirServiceContextualName() {
        assertGenerated("example.other#Forecast", PACKAGE + ".model", "OtherForecast");
    }

    @Test
    void v2RenamedShapesKeepTheirV2NameSoTheyDoNotShadowJavaLang() {
        Model s3 = Model.assembler().addUnparsedModel("s3.smithy", """
            $version: "2"
            namespace com.amazonaws.s3
            service AmazonS3 { version: "2006-03-01", operations: [ListObjects] }
            operation ListObjects { output := { Contents: ObjectList, Errors: ErrorList } }
            list ObjectList { member: Object }
            list ErrorList { member: Error }
            structure Object { Key: String }
            structure Error { Code: String }
            """).assemble().unwrap();
        ServiceShape service = s3.expectShape(ShapeId.from("com.amazonaws.s3#AmazonS3"), ServiceShape.class);
        String root = "software.amazon.awssdk.services.s3";
        AwsSymbolProvider s3Provider =
            new AwsSymbolProvider(s3, service, new AwsCodegenSettings(service.getId(), root));
        assertEquals(root + ".model.S3Object",
                     s3Provider.toSymbol(s3.expectShape(ShapeId.from("com.amazonaws.s3#Object"))).getFullName());
        assertEquals(root + ".model.S3Error",
                     s3Provider.toSymbol(s3.expectShape(ShapeId.from("com.amazonaws.s3#Error"))).getFullName());
        // The renames are keyed by service shape ID: other services' shapes keep their model names.
        assertGenerated("example.weather#Forecast", PACKAGE + ".model", "Forecast");
    }

    /** CloudWatch's awsQuery-era errors would otherwise share the V2 names of its JSON-era errors. */
    @Test
    void collidingCloudWatchErrorsGetDistinctNamesAndTheV2NamesStayWithTheV2Shapes() {
        Model cloudwatch = Model.assembler().addUnparsedModel("cloudwatch.smithy", """
            $version: "2"
            namespace com.amazonaws.cloudwatch
            service GraniteServiceVersion20100801 {
                version: "2010-08-01"
                errors: [LimitExceededFault, LimitExceededException, ResourceNotFound, ResourceNotFoundException]
            }
            @error("client") structure LimitExceededFault { message: String }
            @error("client") structure LimitExceededException {}
            @error("client") structure ResourceNotFound { message: String }
            @error("client") structure ResourceNotFoundException { ResourceType: String, ResourceId: String }
            """).assemble().unwrap();
        ServiceShape service = cloudwatch.expectShape(
            ShapeId.from("com.amazonaws.cloudwatch#GraniteServiceVersion20100801"), ServiceShape.class);
        String model = "software.amazon.awssdk.services.cloudwatch.model.";
        AwsSymbolProvider symbols = new AwsSymbolProvider(cloudwatch, service,
                                                          new AwsCodegenSettings(service.getId(), "software.amazon"
                                                                                                  + ".awssdk.services"
                                                                                                  + ".cloudwatch"));
        Map<String, String> expected = Map.of("LimitExceededFault", "LimitExceededFaultException",
                                              "LimitExceededException", "LimitExceededException",
                                              "ResourceNotFound", "ResourceNotFoundErrorException",
                                              "ResourceNotFoundException", "ResourceNotFoundException");
        expected.forEach((shape, type) -> assertEquals(model + type, symbols.toSymbol(cloudwatch.expectShape(
            ShapeId.from("com.amazonaws.cloudwatch#" + shape))).getFullName()));
    }

    @Test
    void operationsMapToStaticDescriptorTypes() {
        assertGenerated("example.weather#GetForecast", PACKAGE + ".internal.operation", "GetForecastOperation");
    }

    @Test
    void operationTableSharesTheDescriptorPackage() {
        ServiceShape service = model.expectShape(ShapeId.from("example.weather#Weather"), ServiceShape.class);
        Symbol table = AwsSymbolProvider.operationTable(service, new AwsCodegenSettings(service.getId(), PACKAGE));
        assertEquals(PACKAGE + ".internal.operation.WeatherOperations", table.getFullName());
        assertEquals(symbol("example.weather#GetForecast").getNamespace(), table.getNamespace());
        assertEquals("software/amazon/awssdk/services/weather/internal/operation/WeatherOperations.java",
                     table.getDefinitionFile());
    }

    @Test
    void resourcesHaveNoDefinitionFile() {
        Symbol symbol = symbol("example.weather#Station");
        assertEquals(PACKAGE + ".model.Station", symbol.getFullName());
        assertEquals("", symbol.getDefinitionFile());
    }

    @Test
    void simpleAndCollectionShapesMapToRuntimeTypesThroughMembers() {
        String input = "example.weather#GetForecastInput$";
        assertEquals("java.lang.String", symbol(input + "city").getFullName());
        assertEquals("java.lang.Integer", symbol(input + "ID").getFullName());
        assertEquals("java.lang.Boolean", symbol(input + "default").getFullName());
        assertEquals("java.time.Instant", symbol(input + "when").getFullName());
        assertEquals("software.amazon.awssdk.core.SdkBytes", symbol(input + "data").getFullName());
        assertEquals("software.amazon.smithy.java.core.serde.document.Document", symbol(input + "doc").getFullName());
        assertEquals("java.util.List", symbol(input + "items").getFullName());
        assertEquals("java.util.Map", symbol(input + "tags").getFullName());
        assertEquals("java.math.BigDecimal", symbol(input + "big").getFullName());
        assertEquals(PACKAGE + ".model.Kind", symbol(input + "kind").getFullName());
        assertEquals("", symbol(input + "city").getDefinitionFile());
    }

    @Test
    void memberNamesAreJavaIdentifiers() {
        String input = "example.weather#GetForecastInput$";
        assertEquals("city", memberName(input + "city"));
        assertEquals("sseCustomerKey", memberName(input + "SSECustomerKey"));
        assertEquals("id", memberName(input + "ID"));
        assertEquals("defaultValue", memberName(input + "default"));
        assertEquals("schemaValue", memberName("example.weather#Shared$schema"));
        assertEquals("builderValue", memberName("example.weather#Shared$Builder"));
        assertEquals("type", memberName("example.weather#Shared$type"));
        assertEquals("typeValue", memberName("example.weather#Choice2$type"));
        assertEquals("toBuilderValue", memberName("example.weather#Choice2$toBuilder"));
        assertEquals("requestIdValue", memberName("example.weather#WeatherFault$requestId"));
        assertEquals("message", memberName("example.weather#WeatherFault$message"));
        // A non-string message member is state of the error, not its message: message(String) is the builder's.
        assertEquals("messageValue", memberName("example.weather#BadCode$Message"));
        assertEquals("iPv6", AwsSymbolProvider.lowerLeadingCapitals("IPv6"));
        assertEquals("bucket", AwsSymbolProvider.lowerLeadingCapitals("Bucket"));
    }

    @Test
    void otherServicesHaveNoSymbol() {
        ServiceShape other = ServiceShape.builder().id("example.weather#Other").version("1").build();
        assertThrows(CodegenException.class, () -> provider.toSymbol(other));
    }

    private static void assertGenerated(String shapeId, String namespace, String name) {
        Symbol symbol = symbol(shapeId);
        assertEquals(namespace, symbol.getNamespace());
        assertEquals(name, symbol.getName());
        assertEquals(namespace.replace('.', '/') + "/" + name + ".java", symbol.getDefinitionFile());
    }

    private static Symbol symbol(String shapeId) {
        return provider.toSymbol(model.expectShape(ShapeId.from(shapeId)));
    }

    private static String memberName(String memberId) {
        return provider.toMemberName(model.expectShape(ShapeId.from(memberId), MemberShape.class));
    }
}
