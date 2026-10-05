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

package software.amazon.awssdk.test.architecture;

import java.util.List;

/**
 * The forbidden generation paths that {@link GenerationPathInspector} detects, each with the requirements it enforces.
 * {@link GenerationPathInspectionTest} proves that every rule is triggered by at least one negative fixture.
 */
enum GenerationPathRule {

    // One AWS generator with one entry point (Requirements 2.1, 3.1).

    /** A {@code DirectedCodegen} implementation other than the AWS directed code generator, or none at all. */
    SINGLE_DIRECTED_CODEGEN("single-directed-codegen", "2.1", "3.1"),
    /** A {@code CodegenDirector} created, run or subclassed outside the generator entry point, or never run. */
    SINGLE_CODEGEN_DIRECTOR("single-codegen-director", "3.1"),
    /** A smithy-build plugin or projection run: an alternative generation entry point. */
    SMITHY_BUILD_PLUGIN("smithy-build-plugin", "3.1"),
    /** Smithy Java's own code generator, a second Java generator for the same models. */
    SECOND_GENERATOR_TOOLCHAIN("second-generator-toolchain", "3.1"),
    /** A {@code main} method in a code-generation module: a command-line generator entry point. */
    CODEGEN_MAIN_ENTRY_POINT("codegen-main-entry-point", "3.1"),
    /** A caller outside the generator using a member of the entry class other than the single entry method. */
    GENERATOR_ENTRY_POINT("generator-entry-point", "3.1"),
    /** The Maven plugin traversing the model, using writers or generator internals, or writing files itself. */
    PLUGIN_EMITTER("plugin-emitter", "3.1"),

    // No SDK intermediate model or serialized downstream representation (Requirements 2.1, 3.3, 3.4).

    /** V2 code-generation types: the intermediate model, C2J models, customization config, V2 emitters. */
    V2_CODEGEN_MODEL("v2-codegen-model", "2.1", "3.3", "3.4"),
    /** A code-generation type named like a V2 whole-service intermediate-model type. */
    INTERMEDIATE_MODEL_TYPE("intermediate-model-type", "3.3"),
    /** Static code-generation state holding Smithy semantic model types, which outlives one generation. */
    STATIC_SEMANTIC_MODEL_STATE("static-semantic-model-state", "3.3"),
    /** Smithy model serializers or Java object serialization in code generation. */
    MODEL_SERIALIZATION("model-serialization", "3.4"),
    /** A resource or string constant holding a serialized whole-service semantic representation. */
    SERIALIZED_SEMANTIC_REPRESENTATION("serialized-semantic-representation", "2.1", "3.3", "3.4"),

    // C2J and V2 sidecar consumers (Requirements 2.3-2.8).

    /** A C2J or V2 sidecar file name outside the legacy-input classifier that rejects those files. */
    LEGACY_INPUT_FILE_NAME("legacy-input-file-name", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8"),
    /** JSON parsing in code generation outside the legacy-input classifier. */
    LEGACY_INPUT_PARSING("legacy-input-parsing", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8"),
    /** The legacy-input classifier reaching the generator, exposing parsed content, writing files, or missing. */
    LEGACY_CLASSIFIER_PURITY("legacy-classifier-purity", "2.3", "2.4", "2.5", "2.6", "2.7", "2.8"),

    // No per-operation protocol transformers (Requirements 5.3, 5.4).

    /** A generated marshaller, unmarshaller or per-operation transformer class, or a {@code transform} package. */
    PER_OPERATION_TRANSFORMER("per-operation-transformer", "5.3", "5.4"),
    /** A generated per-operation class coupled to a protocol-specific runtime or codec. */
    PER_OPERATION_PROTOCOL_COUPLING("per-operation-protocol-coupling", "5.3", "5.4"),
    /** V2 marshaller, unmarshaller, response-handler, {@code SdkField} or V2 protocol-factory types. */
    V2_PROTOCOL_MARSHALLER("v2-protocol-marshaller", "5.3", "5.4"),

    // Static descriptors and a reflection-free, model-free, V2-free hot path.

    /** A generated operation descriptor that is not reached through a generated static reference. */
    NON_STATIC_DESCRIPTOR_REFERENCE("non-static-descriptor-reference", "3.8"),
    /**
     * Generated code resolving an operation through the operation table's by-ID lookup, or through the service
     * metadata's tooling lookup that forwards to it under the same name and descriptor (the forwarder itself is
     * allowed). Type-registry lookups by shape ID resolve modeled types, not operations, and are not this rule.
     */
    RUNTIME_OPERATION_LOOKUP("runtime-operation-lookup", "3.9", "10.12", "20.22"),
    /** Class-name lookup: {@code Class.forName}, class-loader class loading, {@code Lookup.findClass}. */
    CLASS_NAME_LOOKUP("class-name-lookup", "3.9", "10.12", "20.22"),
    /** Reflective member access: {@code java.lang.reflect}, reflective {@code Class} queries, handle lookup. */
    REFLECTIVE_MEMBER_ACCESS("reflective-member-access", "20.22"),
    /** {@code ServiceLoader} discovery outside an explicitly allowed construction-time site. */
    SERVICE_DISCOVERY("service-discovery", "10.12", "20.22"),
    /** Smithy model parsing or assembly, node parsing, or model resource loading on the generated-client path. */
    RUNTIME_MODEL_LOADING("runtime-model-loading", "2.1", "20.22"),
    /** V2 execution-pipeline, client-handler or Smithy Java SDK V2 bridge types: a V2 fallback path. */
    V2_EXECUTION_PIPELINE("v2-execution-pipeline", "10.5"),
    /** V2-only HTTP client and request/response adapter types. */
    V2_HTTP_ADAPTER("v2-http-adapter", "12.15"),
    /** A V3 class whose name a V2 marker rule matches; the class-name rules would then be ambiguous. */
    V2_MARKER_DEFINED_BY_V3("v2-marker-defined-by-v3", "3.3", "5.3", "5.4", "10.5", "12.15");

    private final String id;
    private final List<String> requirements;

    GenerationPathRule(String id, String... requirements) {
        this.id = id;
        this.requirements = List.of(requirements);
    }

    String id() {
        return id;
    }

    List<String> requirements() {
        return requirements;
    }
}
