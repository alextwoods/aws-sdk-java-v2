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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * Classifies the files of a service's model directory and rejects every input that is not a canonical Smithy model
 * (Requirements 2.3-2.8).
 *
 * <p>Every {@code *.smithy} file is Smithy IDL. Every {@code *.json} file, every {@code *.config} file and every file
 * carrying a V2 code-generation file name is inspected by content: a JSON object with a top-level {@code smithy}
 * version is a Smithy JSON AST model; objects shaped like a C2J service or documentation model or like a V2 paginator,
 * waiter, endpoint rule-set, endpoint BDD, endpoint test, example, customization or smoke-test sidecar are classified
 * as that legacy format whatever the file is called. The V2 file name is only consulted when the content is not
 * conclusive (for example an empty {@code examples-1.json} or a malformed {@code customization.config}). JSON is read
 * with comments allowed because V2 customization files contain them. Other files (READMEs, notes) are ignored.
 *
 * <p>This class only reads: it never assembles a model and never writes.
 */
public final class ModelSourceValidator {

    /** Top-level properties of V2's {@code CustomizationConfig}; any one of them identifies a customization sidecar. */
    static final Set<String> V2_CUSTOMIZATION_PROPERTIES = Set.of(
        "additionalBuilderMethods", "allowEndpointOverrideForEndpointDiscoveryRequiredOperations",
        "allowedEndpointAuthSchemeParams", "allowedEndpointAuthSchemeParamsConfigured",
        "allowedUnderscoreNames", "asyncClientDecorator", "attachPayloadTraitToMember",
        "batchManagerSupported", "calculateCrc32FromCompressedData", "convenienceTypeOverloads",
        "customClientContextParams", "customErrorCodeFieldName", "customOperationContextParams",
        "customProtocolFactoryFqcn", "customResponseMetadata", "customRetryPolicy", "customRetryStrategy",
        "customSdkShapes", "customServiceMetadata", "defaultEnableSocketTimeout2026",
        "defaultNewRetries2026", "defaultRetryMode", "defaultSimpleMethodTestRegion",
        "delegateAsyncClientClass", "delegateSyncClientClass", "deprecatedOperations", "deprecatedShapes",
        "duplicateAndRenameSharedEvents", "enableEndpointAuthSchemeParams",
        "enableEndpointDiscoveryMethodRequired", "enableEndpointProviderUriCaching",
        "enableEnvironmentBearerToken", "enableFastUnmarshaller", "endpointAuthSchemeConfig",
        "endpointParameters", "excludeClientCreateMethod", "excludedSimpleMethods",
        "generateDirectUnionConstructors", "generateEndpointClientTests", "interceptors",
        "internalPlugins", "modelMarshallerDefaultValueSupplier", "multipartCustomization",
        "operationModifiers", "paginationCustomization", "preClientExecutionRequestCustomizer",
        "presignedUrlExtensionSupported", "renameShapes", "requiredTraitValidationEnabled",
        "rootPackageName", "s3ArnableFields", "s3ExpressAuthSupport", "sdkModeledExceptionBaseClassName",
        "sdkRequestBaseClassName", "sdkResponseBaseClassName", "serviceConfig",
        "serviceSpecificHttpConfig", "shapeModifiers", "shapeSubstitutions", "shareModelConfig",
        "skipEndpointTestGeneration", "skipEndpointTests", "skipSyncClientGeneration",
        "syncClientDecorator", "underscoresInNameBehavior", "useGlobalEndpoint",
        "useLegacyEventGenerationScheme", "usePriorKnowledgeForH2", "useS3ExpressSessionAuth", "userAgent",
        "utilitiesMethod", "verifiedSimpleMethods");

    private static final Map<String, ModelSourceFormat> LEGACY_FILE_NAMES =
        Stream.of(ModelSourceFormat.values())
              .flatMap(f -> f.legacyFileNames().stream().map(name -> Map.entry(name, f)))
              .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    private static final String BY_CONTENT = "identified by content";
    private static final String BY_FILE_NAME = "identified by V2 file name";

    private ModelSourceValidator() {
    }

    /**
     * Classifies every relevant file under {@code modelDirectory}, sorted by {@link ModelSource#portablePath()}.
     * Unreadable files are returned as {@link ModelSourceFormat#UNREADABLE}, never skipped.
     *
     * @throws UncheckedIOException if the directory tree cannot be listed
     */
    public static List<ModelSource> scan(Path modelDirectory) {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(modelDirectory)) {
            files = walk.filter(Files::isRegularFile).map(modelDirectory::relativize).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to list model files under " + modelDirectory, e);
        }
        List<ModelSource> result = new ArrayList<>();
        for (Path relative : files) {
            if (!isCandidate(relative)) {
                continue;
            }
            if (fileName(relative).endsWith(".smithy")) {
                result.add(new ModelSource(relative, ModelSourceFormat.SMITHY_IDL, "Smithy IDL file"));
                continue;
            }
            String content;
            try {
                content = Files.readString(modelDirectory.resolve(relative), StandardCharsets.UTF_8);
            } catch (IOException | UncheckedIOException e) {
                result.add(new ModelSource(relative, ModelSourceFormat.UNREADABLE,
                                           "cannot be read as UTF-8: " + e.getMessage()));
                continue;
            }
            classify(relative, content).ifPresent(result::add);
        }
        result.sort(Comparator.comparing(ModelSource::portablePath));
        return List.copyOf(result);
    }

    /**
     * Classifies one file from its model-directory-relative path and content. Returns empty for files that are not
     * model inputs: {@code *.config} files that are neither JSON objects with V2 customization properties nor named
     * {@code customization.config}.
     */
    public static Optional<ModelSource> classify(Path relativePath, String content) {
        String name = fileName(relativePath);
        if (name.endsWith(".smithy")) {
            return Optional.of(new ModelSource(relativePath, ModelSourceFormat.SMITHY_IDL, "Smithy IDL file"));
        }
        boolean json = name.endsWith(".json");
        ModelSourceFormat byName = LEGACY_FILE_NAMES.get(name);
        Node node;
        try {
            node = Node.parseJsonWithComments(content, portable(relativePath));
        } catch (SourceException e) {
            if (byName != null) {
                return Optional.of(new ModelSource(relativePath, byName, BY_FILE_NAME + "; content is not valid JSON"));
            }
            return json ? Optional.of(new ModelSource(relativePath, ModelSourceFormat.UNREADABLE,
                                                      "not valid JSON: " + firstLine(e.getMessage())))
                        : Optional.empty();
        }
        Optional<ModelSource> byContent = node.asObjectNode().flatMap(o -> classifyObject(relativePath, o));
        if (byContent.isPresent()) {
            return byContent;
        }
        if (byName != null) {
            return Optional.of(new ModelSource(relativePath, byName, BY_FILE_NAME));
        }
        return json ? Optional.of(new ModelSource(relativePath, ModelSourceFormat.UNRECOGNIZED_JSON,
                                                  "top-level " + node.getType().toString().toLowerCase(Locale.ROOT)
                                                  + " without a \"smithy\" version"))
                    : Optional.empty();
    }

    /**
     * One diagnostic, in deterministic order, for every source that is not a canonical Smithy model. Each diagnostic
     * names {@code service}, the file, and the exact format ({@link ModelSourceFormat#description()}).
     */
    public static List<Diagnostic> validate(String service, List<ModelSource> sources) {
        List<Diagnostic> result = new ArrayList<>();
        for (ModelSource source : sources) {
            ModelSourceFormat format = source.format();
            format.rejection().ifPresent(code -> result.add(
                Diagnostic.forSource(code, service, source.portablePath(), format.description(), message(source))));
        }
        return result.stream().sorted().toList();
    }

    /** Relative path with {@code /} separators on every platform. */
    static String portable(Path relative) {
        return StreamSupport.stream(relative.spliterator(), false).map(Path::toString)
                            .collect(Collectors.joining("/"));
    }

    private static String message(ModelSource source) {
        ModelSourceFormat format = source.format();
        if (format.isLegacy()) {
            return format.description() + " is not a canonical Smithy model input (" + source.detail() + "). V3 "
                   + "generates only from canonical Smithy models: represent this information with "
                   + format.canonicalReplacement().orElseThrow() + " and remove the file.";
        }
        if (format == ModelSourceFormat.UNRECOGNIZED_JSON) {
            return "JSON file is neither a Smithy JSON AST model nor a recognized C2J or V2 sidecar format ("
                   + source.detail() + "); remove it from the model directory.";
        }
        return "Model file cannot be used: " + source.detail() + ".";
    }

    private static Optional<ModelSource> classifyObject(Path relativePath, ObjectNode o) {
        ModelSourceFormat format;
        String detail = BY_CONTENT;
        if (o.containsMember("smithy")) {
            return Optional.of(new ModelSource(relativePath, ModelSourceFormat.SMITHY_JSON_AST,
                                               "Smithy JSON AST file"));
        } else if (isObject(o, "metadata") && (o.containsMember("operations") || o.containsMember("shapes"))) {
            format = ModelSourceFormat.C2J_SERVICE;
            detail = BY_CONTENT + c2jIdentity(o.expectObjectMember("metadata"));
        } else if (o.containsMember("service") && isObject(o, "operations") && isObject(o, "shapes")) {
            format = ModelSourceFormat.C2J_DOCUMENTATION;
        } else if (isObject(o, "pagination")) {
            format = ModelSourceFormat.V2_PAGINATORS;
        } else if (isObject(o, "waiters")) {
            format = ModelSourceFormat.V2_WAITERS;
        } else if (isObject(o, "parameters") && isArray(o, "rules")) {
            format = ModelSourceFormat.V2_ENDPOINT_RULE_SET;
        } else if (isObject(o, "parameters")
                   && (o.containsMember("nodes") || o.containsMember("root") || o.containsMember("conditions")
                       || o.containsMember("results"))) {
            format = ModelSourceFormat.V2_ENDPOINT_BDD;
        } else if (isArray(o, "testCases")) {
            format = isSmokeTests(o) ? ModelSourceFormat.V2_SMOKE_TESTS : ModelSourceFormat.V2_ENDPOINT_TESTS;
        } else if (isObject(o, "examples")) {
            format = ModelSourceFormat.V2_EXAMPLES;
        } else if (o.getMembers().keySet().stream().map(StringNode::getValue)
                    .anyMatch(V2_CUSTOMIZATION_PROPERTIES::contains)) {
            format = ModelSourceFormat.V2_CUSTOMIZATION;
        } else {
            return Optional.empty();
        }
        return Optional.of(new ModelSource(relativePath, format, detail));
    }

    /** Smoke tests carry a default region or operation-name test cases; endpoint tests carry params/expect. */
    private static boolean isSmokeTests(ObjectNode o) {
        if (o.containsMember("defaultRegion")) {
            return true;
        }
        ArrayNode cases = o.expectArrayMember("testCases");
        return !cases.isEmpty() && cases.get(0).flatMap(Node::asObjectNode)
                                        .map(c -> c.containsMember("operationName")).orElse(false);
    }

    /** The service identity a C2J model declares, for the diagnostic. */
    private static String c2jIdentity(ObjectNode metadata) {
        List<String> parts = new ArrayList<>();
        for (String key : List.of("serviceId", "uid", "protocol")) {
            metadata.getStringMember(key).ifPresent(v -> parts.add(key + " \"" + v.getValue() + "\""));
        }
        return parts.isEmpty() ? "" : "; declares " + String.join(", ", parts);
    }

    private static boolean isCandidate(Path relative) {
        String name = fileName(relative);
        return name.endsWith(".smithy") || name.endsWith(".json") || name.endsWith(".config")
               || LEGACY_FILE_NAMES.containsKey(name);
    }

    private static boolean isObject(ObjectNode o, String member) {
        return o.getMember(member).map(Node::isObjectNode).orElse(false);
    }

    private static boolean isArray(ObjectNode o, String member) {
        return o.getMember(member).map(Node::isArrayNode).orElse(false);
    }

    private static String fileName(Path relative) {
        Path name = relative.getFileName();
        return name == null ? "" : name.toString();
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "unknown syntax error";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
