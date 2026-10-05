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

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Generation phase 9: emits the model provenance and the generation manifest of a service into its generated
 * resource tree, under {@code META-INF/aws-sdk-java-v3/services/<package>/} (Requirements 2.12, 22.2).
 *
 * <ul>
 *   <li>{@value #PROVENANCE_FILE}: the exact service shape ID, the untruncated immutable model revision and its kind,
 *       the model digest and the digest of every model file. It is read at run time by {@code aws-core}'s
 *       {@code ModelProvenance}.</li>
 *   <li>{@value #MANIFEST_FILE}: the generator identity (coordinates and Smithy version), the JDK identity, the
 *       configuration identity (normalized settings and their digest), the SHA-256 digest of every other generated
 *       file (sources and resources, including the provenance), and the required semantic inventory: service
 *       version, protocols, effective auth schemes, every operation with its static descriptor class, the operation
 *       table, and the shape closure (IDs and shape types only).</li>
 * </ul>
 *
 * <p>Both documents are {@link CanonicalJson}: sorted keys, explicitly sorted arrays (except the effective auth
 * scheme list, whose order is the modeled priority), {@code \n} line endings, and no timestamps or absolute paths,
 * so identical inputs on an identical JDK produce identical bytes. Their formats are specified by the JSON Schemas
 * {@value #PROVENANCE_SCHEMA} and {@value #MANIFEST_SCHEMA} shipped in {@code aws-codegen}.
 *
 * <p>The inventory lists identities, never shape definitions, members, targets or traits, so it cannot serve as a
 * second semantic representation of the service.
 */
final class GenerationManifestGenerator {

    /** Resource directory prefix of every service's generated metadata; the Java package follows. */
    static final String RESOURCE_DIRECTORY = "META-INF/aws-sdk-java-v3/services/";
    static final String PROVENANCE_FILE = "model-provenance.json";
    static final String MANIFEST_FILE = "generation-manifest.json";
    static final int FORMAT_VERSION = 1;
    static final String PROVENANCE_SCHEMA = "software/amazon/awssdk/awscodegen/schema/model-provenance-1.schema.json";
    static final String MANIFEST_SCHEMA = "software/amazon/awssdk/awscodegen/schema/generation-manifest-1.schema.json";

    /** {@code root} of files in the generated source tree. */
    static final String SOURCES_ROOT = "sources";
    /** {@code root} of files in the generated resource tree. */
    static final String RESOURCES_ROOT = "resources";

    /** Namespace of the Smithy prelude, whose shapes are not part of any service's own closure. */
    private static final String PRELUDE_NAMESPACE = "smithy.api";

    private GenerationManifestGenerator() {
    }

    /**
     * Digest of one canonical model file.
     *
     * @param path path relative to the model directory, with {@code /} separators
     * @param digest {@code sha256:<hex>} of the file content
     */
    record ModelFile(String path, String digest) {
    }

    /** Directory of the generated metadata of the service generated into {@code packageName}. */
    static String resourceDirectory(String packageName) {
        return RESOURCE_DIRECTORY + packageName + "/";
    }

    /**
     * Stages the provenance and then the manifest into {@code resources}. Must run after every source file has been
     * staged in {@code sources}, because the manifest digests them.
     */
    static void generate(AwsCodegenContext context, List<ModelFile> modelFiles, GeneratorIdentity generator,
                         StagedFileManifest sources, StagedFileManifest resources) {
        String directory = resourceDirectory(context.settings().packageName());
        String provenancePath = directory + PROVENANCE_FILE;
        resources.writeFile(Path.of(provenancePath), CanonicalJson.write(provenance(context, modelFiles)));

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("formatVersion", FORMAT_VERSION);
        manifest.put("service", context.service().getId().toString());
        manifest.put("provenance", provenancePath);
        manifest.put("generator", Map.of("groupId", generator.groupId(),
                                         "artifactId", generator.artifactId(),
                                         "version", generator.version(),
                                         "smithyVersion", generator.smithyVersion()));
        manifest.put("jdk", Map.of("specificationVersion", generator.jdkSpecificationVersion(),
                                   "vendor", generator.jdkVendor()));
        manifest.put("configuration", configuration(context));
        manifest.put("files", fileDigests(sources, resources));
        manifest.put("inventory", inventory(context));
        resources.writeFile(Path.of(directory + MANIFEST_FILE), CanonicalJson.write(manifest));
    }

    private static Map<String, Object> provenance(AwsCodegenContext context, List<ModelFile> modelFiles) {
        List<Map<String, Object>> files = modelFiles.stream()
                                                    .sorted(Comparator.comparing(ModelFile::path))
                                                    .map(f -> Map.<String, Object>of("path", f.path(),
                                                                                     "digest", f.digest()))
                                                    .toList();
        Map<String, Object> model = Map.of("revision", context.revision().value(),
                                           "revisionKind", context.revision().kind(),
                                           "digest", context.modelDigest(),
                                           "files", files);
        return Map.of("formatVersion", FORMAT_VERSION,
                      "service", context.service().getId().toString(),
                      "model", model);
    }

    /** Normalized settings: everything that configures generation, and nothing that depends on the checkout. */
    private static Map<String, Object> configuration(AwsCodegenContext context) {
        Map<String, Object> settings = Map.of(
            "service", context.settings().service().toString(),
            "packageName", context.settings().packageName(),
            "integrations", context.integrations().stream().map(AwsCodegenIntegration::identifier).toList());
        return Map.of("digest", sha256(CanonicalJson.bytes(settings)), "settings", settings);
    }

    /** Digest of every staged source and resource so far (the manifest itself is not staged yet). */
    private static List<Map<String, Object>> fileDigests(StagedFileManifest sources, StagedFileManifest resources) {
        List<Map<String, Object>> files = new ArrayList<>();
        for (Path file : resources.relativeFiles()) {
            files.add(Map.of("root", RESOURCES_ROOT, "path", StagedFileManifest.portable(file),
                             "digest", sha256(resources.content(file))));
        }
        for (Path file : sources.relativeFiles()) {
            files.add(Map.of("root", SOURCES_ROOT, "path", StagedFileManifest.portable(file),
                             "digest", sha256(sources.content(file))));
        }
        // "resources" sorts before "sources"; within a root, relativeFiles() is already sorted by portable path.
        return files;
    }

    private static Map<String, Object> inventory(AwsCodegenContext context) {
        Model model = context.model();
        ServiceShape service = context.service();
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        List<Map<String, Object>> operations = TopDownIndex.of(model).getContainedOperations(service).stream()
            .sorted()
            .map(operation -> Map.<String, Object>of(
                "id", operation.getId().toString(),
                "descriptor", context.symbolProvider().toSymbol(operation).getFullName()))
            .toList();
        List<Map<String, Object>> closure = new Walker(model).walkShapes(service).stream()
            .filter(shape -> !shape.isMemberShape() && !PRELUDE_NAMESPACE.equals(shape.getId().getNamespace()))
            .sorted(Comparator.comparing(Shape::getId))
            .map(shape -> Map.<String, Object>of("id", shape.getId().toString(),
                                                 "type", shape.getType().toString()))
            .toList();
        return Map.of(
            "serviceVersion", service.getVersion(),
            "protocols", serviceIndex.getProtocols(service).keySet().stream().map(ShapeId::toString).sorted().toList(),
            // Effective order is the modeled auth priority, so it is kept rather than sorted.
            "authSchemes", serviceIndex.getEffectiveAuthSchemes(service).keySet().stream()
                                       .map(ShapeId::toString).toList(),
            "operations", operations,
            "operationTable", AwsSymbolProvider.operationTable(service, context.settings()).getFullName(),
            "shapeClosure", closure);
    }

    /** {@code sha256:<lower-case hex>} of {@code content}. */
    static String sha256(byte[] content) {
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }
}
