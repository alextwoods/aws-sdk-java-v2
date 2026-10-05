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

package software.amazon.awssdk.awscodegen.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

class SemanticValidatorTest {
    private static final String SERVICE = "example.weather#Weather";

    @Test
    void serviceWithSupportedProtocolAndAuthPasses() {
        assertEquals(List.of(), validate("""
            @aws.protocols#restJson1
            @aws.auth#sigv4(name: "weather")
            service Weather { version: "1", operations: [GetForecast] }
            @http(method: "GET", uri: "/forecast")
            @readonly
            operation GetForecast {}
            """));
    }

    @Test
    void everyLaunchProtocolIsSupported() {
        for (String protocol : List.of("aws.protocols#awsJson1_0", "aws.protocols#awsJson1_1",
                                       "aws.protocols#restJson1", "aws.protocols#restXml", "aws.protocols#awsQuery",
                                       "aws.protocols#ec2Query", "smithy.protocols#rpcv2Cbor")) {
            String xmlNamespace = protocol.contains("Query") ? "@xmlNamespace(uri: \"https://example.com/\")\n" : "";
            assertEquals(List.of(), validate("@" + protocol + "\n" + xmlNamespace
                                             + "@auth([])\nservice Weather { version: \"1\" }\n"),
                         protocol);
        }
    }

    @Test
    void missingProtocolIsAServiceDiagnostic() {
        assertEquals(List.of(Diagnostic.forService(
                         DiagnosticCode.MISSING_SEMANTIC_ELEMENT, SERVICE, "protocol trait",
                         "The service applies no protocol trait, so a generated client has no modeled protocol to "
                         + "select. Apply one of [aws.protocols#awsJson1_0, aws.protocols#awsJson1_1, "
                         + "aws.protocols#awsQuery, aws.protocols#ec2Query, aws.protocols#restJson1, "
                         + "aws.protocols#restXml, smithy.protocols#rpcv2Cbor] to " + SERVICE + ".")),
                     validate("@auth([])\nservice Weather { version: \"1\" }\n"));
    }

    @Test
    void unsupportedProtocolIsAServiceDiagnostic() {
        List<Diagnostic> diagnostics = validate("""
            @protocolDefinition
            @trait(selector: "service")
            structure customProtocol {}
            @customProtocol
            @auth([])
            service Weather { version: "1" }
            """);
        assertEquals(1, diagnostics.size());
        assertEquals(DiagnosticCode.UNSUPPORTED_SEMANTIC_ELEMENT, diagnostics.get(0).code());
        assertEquals("protocol trait", diagnostics.get(0).element());
        assertEquals("The service applies only protocol traits [example.weather#customProtocol], none of which the "
                     + "V3 protocol runtime supports; supported protocols are [aws.protocols#awsJson1_0, "
                     + "aws.protocols#awsJson1_1, aws.protocols#awsQuery, aws.protocols#ec2Query, "
                     + "aws.protocols#restJson1, aws.protocols#restXml, smithy.protocols#rpcv2Cbor].",
                     diagnostics.get(0).message());
    }

    @Test
    void operationsWithoutModeledAuthAreReportedIndividually() {
        List<Diagnostic> diagnostics = validate("""
            @aws.protocols#awsJson1_0
            service Weather { version: "1", operations: [Unmodeled, Anonymous, Optional, AlsoUnmodeled] }
            operation Unmodeled {}
            @auth([])
            operation Anonymous {}
            @optionalAuth
            operation Optional {}
            operation AlsoUnmodeled {}
            """);
        String message = "The operation resolves no auth scheme: neither it nor the service applies an auth scheme "
                         + "trait (for example @aws.auth#sigv4), and neither declares anonymous access with "
                         + "@smithy.api#auth([]) or @smithy.api#optionalAuth.";
        assertEquals(List.of(Diagnostic.forOperation(DiagnosticCode.MISSING_SEMANTIC_ELEMENT, SERVICE,
                                                     "example.weather#AlsoUnmodeled", "auth scheme", message),
                             Diagnostic.forOperation(DiagnosticCode.MISSING_SEMANTIC_ELEMENT, SERVICE,
                                                     "example.weather#Unmodeled", "auth scheme", message)),
                     diagnostics);
    }

    @Test
    void anonymousServiceAndSupportedSchemesPass() {
        assertEquals(List.of(), validate("""
            @aws.protocols#awsJson1_0
            @auth([])
            service Weather { version: "1", operations: [GetForecast] }
            operation GetForecast {}
            """));
        assertEquals(List.of(), validate("""
            @aws.protocols#awsJson1_0
            @aws.auth#sigv4(name: "weather")
            @aws.auth#sigv4a(name: "weather")
            @httpBearerAuth
            @auth([httpBearerAuth, aws.auth#sigv4a])
            service Weather { version: "1", operations: [GetForecast] }
            operation GetForecast {}
            """));
    }

    @Test
    void operationsResolvingOnlyUnsupportedSchemesAreReported() {
        List<Diagnostic> diagnostics = validate("""
            @aws.protocols#awsJson1_0
            @httpBasicAuth
            service Weather { version: "1", operations: [Basic, OptionalBasic] }
            operation Basic {}
            @optionalAuth
            operation OptionalBasic {}
            """);
        assertEquals(List.of(Diagnostic.forOperation(
                         DiagnosticCode.UNSUPPORTED_SEMANTIC_ELEMENT, SERVICE, "example.weather#Basic", "auth scheme",
                         "The operation resolves only auth schemes [smithy.api#httpBasicAuth], none of which V3 "
                         + "Modern Auth supports; supported auth schemes are [aws.auth#sigv4, aws.auth#sigv4a, "
                         + "smithy.api#httpBearerAuth].")),
                     diagnostics);
    }

    @Test
    void allProblemsAreReportedInDeterministicOrder() {
        List<Diagnostic> diagnostics = validate("""
            service Weather { version: "1", operations: [B, A] }
            operation B {}
            operation A {}
            """);
        assertEquals(List.of("protocol trait", "auth scheme", "auth scheme"),
                     diagnostics.stream().map(Diagnostic::element).toList());
        assertEquals(List.of("", "example.weather#A", "example.weather#B"),
                     diagnostics.stream().map(d -> d.operation().orElse("")).toList());
    }

    @Test
    void checksMustHaveDistinctIdentifiers() {
        Model model = assemble("@auth([])\n@aws.protocols#awsJson1_0\nservice Weather { version: \"1\" }\n");
        ServiceShape service = model.expectShape(ShapeId.from(SERVICE), ServiceShape.class);
        assertThrows(IllegalArgumentException.class, () -> SemanticValidator.validate(
            model, service, List.of(new ProtocolCheck(), new ProtocolCheck())));
    }

    private static List<Diagnostic> validate(String body) {
        Model model = assemble(body);
        return SemanticValidator.validate(model, model.expectShape(ShapeId.from(SERVICE), ServiceShape.class));
    }

    private static Model assemble(String body) {
        ClassLoader classLoader = SemanticValidatorTest.class.getClassLoader();
        return Model.assembler(classLoader).discoverModels(classLoader)
                    .addUnparsedModel("weather.smithy", "$version: \"2\"\nnamespace example.weather\n" + body)
                    .assemble().unwrap();
    }
}
