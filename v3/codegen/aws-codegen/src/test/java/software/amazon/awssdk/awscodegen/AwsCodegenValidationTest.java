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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.awscodegen.validation.Diagnostic;
import software.amazon.awssdk.awscodegen.validation.DiagnosticCode;
import software.amazon.awssdk.awscodegen.validation.V2Layouts;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DynamicTrait;
import software.amazon.smithy.model.traits.PaginatedTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.waiters.WaitableTrait;

/** Canonical-model validation as part of {@link AwsCodegen#generate}: rejection is complete and atomic. */
class AwsCodegenValidationTest {
    private static final String SERVICE = "example.weather#Weather";
    private static final String PACKAGE = "software.amazon.awssdk.services.weather";

    /** A canonical model that uses AWS protocol, auth, endpoint, pagination, waiter and example traits. */
    private static final String CANONICAL = """
        $version: "2"
        namespace example.weather

        use aws.api#service
        use aws.auth#sigv4
        use aws.protocols#awsJson1_0
        use smithy.rules#endpointRuleSet
        use smithy.waiters#waitable

        @service(sdkId: "Weather")
        @sigv4(name: "weather")
        @awsJson1_0
        @endpointRuleSet({
            version: "1.0"
            parameters: {
                Region: { type: "String", builtIn: "AWS::Region", required: true, documentation: "The region." }
            }
            rules: [
                {
                    type: "endpoint"
                    conditions: []
                    endpoint: { url: "https://weather.{Region}.amazonaws.com" }
                }
            ]
        })
        service Weather {
            version: "2006-03-01"
            operations: [GetForecast, ListCities]
        }

        @readonly
        @waitable(ForecastReady: {
            acceptors: [
                { state: "success", matcher: { output: { path: "summary", expected: "ready", comparator: "stringEquals" } } }
            ]
        })
        @examples([{ title: "Seattle", input: { city: "Seattle" }, output: { summary: "ready" } }])
        operation GetForecast {
            input := { city: String }
            output := { summary: String }
        }

        @readonly
        @paginated(inputToken: "nextToken", outputToken: "nextToken", pageSize: "maxResults", items: "cities")
        operation ListCities {
            input := { nextToken: String, maxResults: Integer }
            output := { nextToken: String, cities: Cities }
        }

        list Cities { member: String }
        """;

    @TempDir
    Path temp;

    @Test
    void canonicalAwsTraitsAreTypedAndGenerationSucceeds() throws IOException {
        Path models = write("model/weather.smithy", CANONICAL);
        List<Model> captured = new ArrayList<>();

        AwsCodegen.generate(new AwsCodegenRequest(SERVICE, PACKAGE, models, temp.resolve("generated")),
                            List.of(new Capturing(captured)));

        Model model = captured.get(0);
        Shape service = model.expectShape(ShapeId.from(SERVICE));
        assertInstanceOf(AwsJson1_0Trait.class, service.findTrait(AwsJson1_0Trait.ID).orElseThrow());
        assertInstanceOf(SigV4Trait.class, service.findTrait(SigV4Trait.ID).orElseThrow());
        assertInstanceOf(EndpointRuleSetTrait.class, service.findTrait(EndpointRuleSetTrait.ID).orElseThrow());
        assertTrue(model.expectShape(ShapeId.from("example.weather#GetForecast")).hasTrait(WaitableTrait.class));
        assertTrue(model.expectShape(ShapeId.from("example.weather#ListCities")).hasTrait(PaginatedTrait.class));
    }

    @Test
    void everyLegacyInputIsReportedAndNothingIsPublished() throws IOException {
        Path models = write("model/weather.smithy", CANONICAL);
        List<Path> legacy = new ArrayList<>();
        V2Layouts.copy(V2Layouts.SIGNER, models.resolve("v2")).forEach(p -> legacy.add(Path.of("v2").resolve(p)));
        V2Layouts.copy(V2Layouts.NESTED, models.resolve("nested")).forEach(p -> legacy.add(Path.of("nested").resolve(p)));
        Path freshOutput = temp.resolve("fresh");
        Path existingOutput = existingOutput();

        for (Path output : List.of(freshOutput, existingOutput)) {
            AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> AwsCodegen.generate(
                new AwsCodegenRequest(SERVICE, PACKAGE, models, output), List.of(new Capturing(new ArrayList<>()))));

            assertEquals(legacy.stream().map(p -> p.toString().replace('\\', '/')).sorted().toList(),
                         e.diagnostics().stream().map(d -> d.source().orElseThrow()).toList());
            assertTrue(e.diagnostics().stream().allMatch(d -> d.service().equals(SERVICE)));
            assertTrue(e.diagnostics().stream().allMatch(d -> d.code().name().startsWith("LEGACY_")));
            assertTrue(e.getMessage().startsWith("The canonical model input of service " + SERVICE + " contains "
                                                 + "non-canonical inputs (12 diagnostic(s)); no Generated Service "
                                                 + "Package artifact was produced:\n  [LEGACY_"), e.getMessage());
            e.diagnostics().forEach(d -> assertTrue(e.getMessage().contains("\n  " + d.render()), d.render()));
        }
        assertFalse(Files.exists(freshOutput));
        assertExistingOutputUntouched(existingOutput);
        try (Stream<Path> children = Files.list(temp)) {
            assertEquals(List.of("existing", "model"), children.map(p -> p.getFileName().toString()).sorted().toList(),
                         "no staging or output directory is created for a rejected service");
        }
    }

    @Test
    void legacyRejectionRunsBeforeIntegrationsOrModelAssembly() throws IOException {
        Path models = write("model/broken.smithy", "$version: \"2\"\nnamespace example.weather\nstructure X { a: Nope }\n");
        write("model/paginators-1.json", "{\"pagination\": {}}");
        List<Model> captured = new ArrayList<>();

        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> AwsCodegen.generate(
            new AwsCodegenRequest(SERVICE, PACKAGE, models, temp.resolve("generated")),
            List.of(new Capturing(captured))));

        assertEquals(List.of(DiagnosticCode.LEGACY_PAGINATOR_SIDECAR),
                     e.diagnostics().stream().map(Diagnostic::code).toList());
        assertEquals(List.of(), captured);
    }

    @Test
    void missingSemanticsFailWithEveryDiagnosticAndPublishNothing() throws IOException {
        Path models = write("model/weather.smithy", """
            $version: "2"
            namespace example.weather
            service Weather { version: "1", operations: [GetForecast, ListCities] }
            operation GetForecast {}
            operation ListCities {}
            """);
        Path output = existingOutput();

        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> AwsCodegen.generate(
            new AwsCodegenRequest(SERVICE, PACKAGE, models, output), List.of()));

        assertEquals(List.of("MISSING_SEMANTIC_ELEMENT  protocol trait",
                             "MISSING_SEMANTIC_ELEMENT example.weather#GetForecast auth scheme",
                             "MISSING_SEMANTIC_ELEMENT example.weather#ListCities auth scheme"),
                     e.diagnostics().stream()
                      .map(d -> d.code() + " " + d.operation().orElse("") + " " + d.element()).toList());
        assertTrue(e.getMessage().contains(SERVICE + " lacks required AWS semantics (3 diagnostic(s))"),
                   e.getMessage());
        assertExistingOutputUntouched(output);
    }

    @Test
    void smithyValidationEventsBecomeStructuredDiagnostics() throws IOException {
        Path models = write("model/weather.smithy", """
            $version: "2"
            namespace example.weather
            @aws.protocols#awsJson1_0
            @auth([])
            service Weather { version: "1", operations: [GetForecast] }
            operation GetForecast { input: Missing }
            structure Broken { member: AlsoMissing }
            """);

        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> AwsCodegen.generate(
            new AwsCodegenRequest(SERVICE, PACKAGE, models, temp.resolve("generated")), List.of()));

        List<Diagnostic> diagnostics = e.diagnostics();
        assertEquals(2, diagnostics.size(), e.getMessage());
        assertTrue(diagnostics.stream().allMatch(d -> d.code() == DiagnosticCode.INVALID_MODEL));
        Diagnostic operation = diagnostics.stream().filter(d -> d.operation().isPresent()).findFirst().orElseThrow();
        assertEquals(Optional.of("example.weather#GetForecast"), operation.operation());
        assertEquals(Optional.of("weather.smithy:6:1"), operation.source());
        assertEquals("Smithy validation event Target.UnresolvedShape on example.weather#GetForecast",
                     operation.element());
        assertTrue(operation.message().startsWith("ERROR: "), operation.message());
        assertTrue(e.getMessage().contains("is invalid"), e.getMessage());
    }

    @Test
    void unknownTraitsAreRetainedExactlyThroughTheDefaultTransforms() throws IOException {
        Path models = write("model/weather.smithy", """
            $version: "2"
            namespace example.weather

            @aws.protocols#awsJson1_0
            @auth([])
            @com.example.unknown#serviceTag({ owner: "weather", tiers: [1, 2.5, true, null] })
            service Weather { version: "1", operations: [GetForecast] }

            @com.example.unknown#marker
            operation GetForecast { input: ForecastInput }

            @com.example.unknown#inherited("from-mixin")
            @mixin
            structure Mixed {}

            structure ForecastInput with [Mixed] {
                @com.example.unknown#member(["a", "b"])
                city: String
            }

            apply ForecastInput @com.example.unknown#applied(42)
            """);
        write("model/extra.json", """
            {
              "smithy": "2.0",
              "shapes": {
                "example.weather#Unused": {
                  "type": "string",
                  "traits": { "com.example.unknown#json": { "nested": { "k": "v" } } }
                }
              }
            }
            """);
        List<Model> captured = new ArrayList<>();

        AwsCodegen.generate(new AwsCodegenRequest(SERVICE, PACKAGE, models, temp.resolve("generated")),
                            List.of(new Capturing(captured)));

        Model model = captured.get(0);
        assertUnknownTrait(model, SERVICE, "com.example.unknown#serviceTag",
                           Node.objectNodeBuilder().withMember("owner", "weather")
                               .withMember("tiers", Node.fromNodes(Node.from(1), Node.from(2.5), Node.from(true),
                                                                   Node.nullNode()))
                               .build());
        assertUnknownTrait(model, "example.weather#GetForecast", "com.example.unknown#marker", Node.objectNode());
        assertUnknownTrait(model, "example.weather#ForecastInput", "com.example.unknown#inherited",
                           Node.from("from-mixin"));
        assertUnknownTrait(model, "example.weather#ForecastInput", "com.example.unknown#applied", Node.from(42));
        assertUnknownTrait(model, "example.weather#ForecastInput$city", "com.example.unknown#member",
                           Node.fromStrings("a", "b"));
        assertUnknownTrait(model, "example.weather#Unused", "com.example.unknown#json",
                           Node.objectNode().withMember("nested", Node.objectNode().withMember("k", "v")));
    }

    private static void assertUnknownTrait(Model model, String target, String traitId, Node value) {
        Trait trait = model.expectShape(ShapeId.from(target)).findTrait(ShapeId.from(traitId))
                           .orElseThrow(() -> new AssertionError(traitId + " missing on " + target));
        assertInstanceOf(DynamicTrait.class, trait);
        assertEquals(ShapeId.from(traitId), trait.toShapeId());
        assertEquals(value, trait.toNode(), traitId);
    }

    /** Records the model integrations see (the model after Smithy's default codegen transforms). */
    private record Capturing(List<Model> models) implements AwsCodegenIntegration {
        @Override
        public String identifier() {
            return "test.capturing";
        }

        @Override
        public void customize(AwsCodegenContext context) {
            models.add(context.model());
        }
    }

    private Path write(String relative, String content) throws IOException {
        Path file = temp.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.getParent();
    }

    private Path existingOutput() throws IOException {
        Path output = Files.createDirectories(temp.resolve("existing"));
        Files.writeString(output.resolve("Existing.java"), "class Existing {}");
        return output;
    }

    private static void assertExistingOutputUntouched(Path output) throws IOException {
        try (Stream<Path> files = Files.walk(output)) {
            assertEquals(List.of(output.resolve("Existing.java")), files.filter(Files::isRegularFile).toList());
        }
    }
}
