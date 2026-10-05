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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelSourceValidatorTest {
    private static final String SERVICE = "com.amazonaws.signer#WalrusSigningPlatformFrontendService";

    @TempDir
    Path temp;

    @Test
    void classifiesEveryFileOfTheFlatV2Layout() {
        assertEquals(Map.of("customization.config", ModelSourceFormat.V2_CUSTOMIZATION,
                            "docs-2.json", ModelSourceFormat.C2J_DOCUMENTATION,
                            "endpoint-bdd-1.json", ModelSourceFormat.V2_ENDPOINT_BDD,
                            "endpoint-tests.json", ModelSourceFormat.V2_ENDPOINT_TESTS,
                            "examples-1.json", ModelSourceFormat.V2_EXAMPLES,
                            "paginators-1.json", ModelSourceFormat.V2_PAGINATORS,
                            "service-2.json", ModelSourceFormat.C2J_SERVICE,
                            "waiters-2.json", ModelSourceFormat.V2_WAITERS),
                     formats(ModelSourceValidator.scan(V2Layouts.directory(V2Layouts.SIGNER))));
    }

    @Test
    void classifiesTheNestedLayoutIncludingRenamedAndCommentedFiles() {
        List<ModelSource> sources = ModelSourceValidator.scan(V2Layouts.directory(V2Layouts.NESTED));
        assertEquals(Map.of("gamelift/customization.config", ModelSourceFormat.V2_CUSTOMIZATION,
                            "route53resolver/smoke.json", ModelSourceFormat.V2_SMOKE_TESTS,
                            "signer/api-2.json", ModelSourceFormat.C2J_SERVICE,
                            "waf/rules.json", ModelSourceFormat.V2_ENDPOINT_RULE_SET),
                     formats(sources));
        assertEquals("identified by content; declares serviceId \"signer\", uid \"signer-2017-08-25\", "
                     + "protocol \"rest-json\"",
                     sources.get(2).detail());
    }

    @Test
    void contentDecidesTheFormatWhateverTheFileIsCalled() throws IOException {
        Path layout = V2Layouts.directory(V2Layouts.SIGNER);
        for (ModelSource original : ModelSourceValidator.scan(layout)) {
            String content = Files.readString(layout.resolve(original.relativePath()));
            ModelSource renamed = ModelSourceValidator.classify(Path.of("model", "renamed.json"), content).orElseThrow();
            assertEquals(original.format(), renamed.format(), original.portablePath());
            assertTrue(renamed.detail().startsWith("identified by content"), renamed.detail());
        }
    }

    @Test
    void fileNamesIdentifyLegacyFilesWhoseContentIsInconclusive() {
        assertEquals(ModelSourceFormat.V2_EXAMPLES, classify("examples-1.json", "{}").format());
        assertEquals("identified by V2 file name", classify("examples-1.json", "{}").detail());
        assertEquals(ModelSourceFormat.V2_WAITERS, classify("waiters-2.json", "[]").format());
        assertEquals(ModelSourceFormat.V2_CUSTOMIZATION, classify("customization.config", "{ not json").format());
        assertEquals("identified by V2 file name; content is not valid JSON",
                     classify("customization.config", "{ not json").detail());
    }

    @Test
    void smithyModelsAreCanonicalEvenWhenTheyLookLikeC2j() {
        // A Smithy JSON AST also has top-level metadata and shapes; its "smithy" version identifies it.
        String ast = "{\"smithy\": \"2.0\", \"metadata\": {\"a\": 1}, \"shapes\": {}}";
        assertEquals(ModelSourceFormat.SMITHY_JSON_AST, classify("service-2.json", ast).format());
        assertEquals(ModelSourceFormat.SMITHY_IDL, classify("weather.smithy", "not parsed").format());
    }

    @Test
    void unrecognizedAndMalformedJsonAreRejectedAndOtherFilesIgnored() {
        assertEquals(new ModelSource(Path.of("smithy-build.json"), ModelSourceFormat.UNRECOGNIZED_JSON,
                                     "top-level object without a \"smithy\" version"),
                     classify("smithy-build.json", "{\"version\": \"1.0\", \"projections\": {}}"));
        assertEquals(ModelSourceFormat.UNRECOGNIZED_JSON, classify("list.json", "[1, 2]").format());
        ModelSource malformed = classify("broken.json", "{\"smithy\": ");
        assertEquals(ModelSourceFormat.UNREADABLE, malformed.format());
        assertTrue(malformed.detail().startsWith("not valid JSON: "), malformed.detail());
        assertEquals(Optional.empty(), ModelSourceValidator.classify(Path.of("logging.config"), "level=INFO"));
        assertEquals(Optional.empty(), ModelSourceValidator.classify(Path.of("other.config"), "{\"level\": 1}"));
    }

    @Test
    void scanIgnoresNonModelFilesAndSortsByPortablePath() throws IOException {
        Files.createDirectories(temp.resolve("b"));
        Files.writeString(temp.resolve("README.md"), "# notes");
        Files.writeString(temp.resolve("b/z.smithy"), "$version: \"2\"\n");
        Files.writeString(temp.resolve("a.smithy"), "$version: \"2\"\n");
        Files.writeString(temp.resolve("b/model.json"), "{\"smithy\": \"2.0\"}");

        assertEquals(List.of("a.smithy", "b/model.json", "b/z.smithy"),
                     ModelSourceValidator.scan(temp).stream().map(ModelSource::portablePath).toList());
        assertEquals(List.of(), ModelSourceValidator.validate(SERVICE, ModelSourceValidator.scan(temp)));
    }

    @Test
    void reportsOneDiagnosticPerRejectedFileNamingTheServiceAndExactFormat() {
        List<Diagnostic> diagnostics =
            ModelSourceValidator.validate(SERVICE, ModelSourceValidator.scan(V2Layouts.directory(V2Layouts.SIGNER)));

        assertEquals(List.of("customization.config", "docs-2.json", "endpoint-bdd-1.json", "endpoint-tests.json",
                             "examples-1.json", "paginators-1.json", "service-2.json", "waiters-2.json"),
                     diagnostics.stream().map(d -> d.source().orElseThrow()).toList());
        assertEquals(List.of(DiagnosticCode.LEGACY_CUSTOMIZATION_SIDECAR, DiagnosticCode.LEGACY_C2J_MODEL,
                             DiagnosticCode.LEGACY_ENDPOINT_SIDECAR, DiagnosticCode.LEGACY_ENDPOINT_SIDECAR,
                             DiagnosticCode.LEGACY_EXAMPLE_SIDECAR, DiagnosticCode.LEGACY_PAGINATOR_SIDECAR,
                             DiagnosticCode.LEGACY_C2J_MODEL, DiagnosticCode.LEGACY_WAITER_SIDECAR),
                     diagnostics.stream().map(Diagnostic::code).toList());
        diagnostics.forEach(d -> {
            assertEquals(SERVICE, d.service());
            assertEquals(Optional.empty(), d.operation());
        });
        assertEquals("[LEGACY_C2J_MODEL] service " + SERVICE + ", source service-2.json, element C2J service model "
                     + "format (service-2.json): C2J service model format (service-2.json) is not a canonical Smithy "
                     + "model input (identified by content; declares serviceId \"signer\", uid "
                     + "\"signer-2017-08-25\", protocol \"rest-json\"). V3 generates only from canonical Smithy "
                     + "models: represent this information with the service's canonical Smithy model (for example "
                     + "aws-models <service>/smithy/model.json) and remove the file.",
                     diagnostics.get(6).render());
        assertEquals("V2 paginator sidecar format (paginators-1.json)", diagnostics.get(5).element());
        assertEquals("V2 waiter sidecar format (waiters-2.json)", diagnostics.get(7).element());
        assertEquals("V2 example sidecar format (examples-1.json)", diagnostics.get(4).element());
        assertEquals("V2 customization sidecar format (customization.config)", diagnostics.get(0).element());
        assertEquals("V2 endpoint BDD sidecar format (endpoint-bdd-1.json)", diagnostics.get(2).element());
        assertEquals("V2 endpoint test sidecar format (endpoint-tests-1.json)", diagnostics.get(3).element());
    }

    @Test
    void unrecognizedAndUnreadableFilesAreDiagnosedToo() {
        List<Diagnostic> diagnostics = ModelSourceValidator.validate(SERVICE, List.of(
            classify("z.json", "{\"a\": 1}"), classify("a.json", "{")));
        assertEquals(List.of(DiagnosticCode.UNREADABLE_MODEL_SOURCE, DiagnosticCode.UNRECOGNIZED_MODEL_SOURCE),
                     diagnostics.stream().map(Diagnostic::code).toList());
        assertEquals("[UNRECOGNIZED_MODEL_SOURCE] service " + SERVICE + ", source z.json, element unrecognized JSON "
                     + "document: JSON file is neither a Smithy JSON AST model nor a recognized C2J or V2 sidecar "
                     + "format (top-level object without a \"smithy\" version); remove it from the model directory.",
                     diagnostics.get(1).render());
    }

    private static ModelSource classify(String name, String content) {
        return ModelSourceValidator.classify(Path.of(name), content).orElseThrow();
    }

    private static Map<String, ModelSourceFormat> formats(List<ModelSource> sources) {
        Map<String, ModelSourceFormat> result = new LinkedHashMap<>();
        sources.forEach(s -> result.put(s.portablePath(), s.format()));
        return result;
    }
}
