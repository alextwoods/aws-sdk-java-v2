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

package software.amazon.awssdk.c2j.smithy;

import java.nio.file.Path;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.build.PluginContext;
import software.amazon.smithy.build.SmithyBuildPlugin;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;

/**
 * Runs smithy-java's own type/schema code generator on a model produced by {@link C2jToSmithyConverter}.
 *
 * <p>This is the {@code smithy -> java} step, and it deliberately <b>reuses smithy-java's published
 * code generator</b> ({@code JavaCodegenPlugin} in {@code types} mode) rather than re-implementing
 * schema-literal emission in the v2 codegen. The full pipeline is:
 *
 * <pre>C2J service-2.json --(C2jToSmithyConverter)--&gt; Smithy Model --(JavaCodegenPlugin types)--&gt;
 * native smithy-java POJOs (with native $SCHEMA literals, Schemas holder, SharedSerde, and
 * switch(memberIndex) builders).</pre>
 *
 * <p>Because we reuse the upstream generator, the generated types inherit document types, type
 * registries, and runtime protocol selection as smithy-java adds them.
 */
public final class SmithyTypesCodegenRunner {

    private SmithyTypesCodegenRunner() {
    }

    /**
     * Convert {@code serviceJson} and generate Smithy-java types into {@code outputDir} under the
     * given Java package {@code namespace}.
     */
    public static void generate(Path serviceJson, Path outputDir, String namespace) {
        Model model = C2jToSmithyConverter.convert(serviceJson);
        generate(model, outputDir, namespace);
    }

    /** Generate Smithy-java types for an already-converted {@link Model}. */
    public static void generate(Model model, Path outputDir, String namespace) {
        FileManifest manifest = FileManifest.create(outputDir);

        // In types mode the generator uses a synthetic service with no protocol trait, so the
        // protocol-declared serialization traits aren't auto-retained in the generated Schemas.
        // List them explicitly as runtimeTraits so HTTP binding + wire-name traits survive into the
        // schema literals (otherwise members serialize as plain body fields and HTTP binding breaks).
        ObjectNode settings = ObjectNode.builder()
                .withMember("name", namespace + ".types")
                .withMember("namespace", namespace)
                .withMember("modes", Node.fromStrings("types"))
                .withMember("runtimeTraits", Node.fromStrings(
                        "smithy.api#httpLabel",
                        "smithy.api#httpHeader",
                        "smithy.api#httpQuery",
                        "smithy.api#httpQueryParams",
                        "smithy.api#httpPayload",
                        "smithy.api#httpPrefixHeaders",
                        "smithy.api#httpResponseCode",
                        "smithy.api#jsonName",
                        "smithy.api#xmlName",
                        "smithy.api#xmlAttribute",
                        "smithy.api#xmlFlattened",
                        "smithy.api#xmlNamespace",
                        "smithy.api#timestampFormat",
                        "smithy.api#mediaType",
                        "smithy.api#hostLabel",
                        "smithy.api#sparse",
                        "smithy.api#required"))
                .build();

        PluginContext context = PluginContext.builder()
                .model(model)
                .fileManifest(manifest)
                .settings(settings)
                .build();

        SmithyBuildPlugin plugin = new software.amazon.smithy.java.codegen.JavaCodegenPlugin();
        plugin.execute(context);
    }

    /** CLI: {@code <service-2.json> <outputDir> <javaNamespace>}. */
    public static void main(String[] args) {
        generate(Path.of(args[0]), Path.of(args[1]), args[2]);
        System.out.println("Generated Smithy-java types into " + args[1] + " (package " + args[2] + ")");
    }
}
