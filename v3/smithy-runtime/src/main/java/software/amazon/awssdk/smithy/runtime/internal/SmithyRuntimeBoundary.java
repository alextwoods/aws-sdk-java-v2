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

package software.amazon.awssdk.smithy.runtime.internal;

import java.util.List;
import java.util.Set;

/**
 * The machine-readable definition of the lightweight Smithy Java runtime boundary that every generated service
 * client inherits through {@code software.amazon.awssdk:smithy-runtime}.
 *
 * <p>The boundary has two layers:
 * <ul>
 *     <li><b>Artifact layer.</b> The runtime dependency closure of {@code smithy-runtime} is exactly the curated
 *     Smithy Java modules in {@link #SMITHY_JAVA_RUNTIME_ARTIFACTS} plus the Smithy artifacts in
 *     {@link #TRANSITIONAL_SMITHY_ARTIFACTS}. Nothing else may be reachable.</li>
 *     <li><b>Class layer.</b> Code running on the generated-client path resolves no class matched by
 *     {@link #isForbiddenRuntimeClass(String)}: Smithy model parsing, assembly, validation, selection, knowledge
 *     indexes, model transforms, model serialization, libraries built on the semantic model (waiters, endpoint
 *     rules, AWS and protocol traits), dynamic model-to-schema conversion and the V3 dynamic layer, or code
 *     generation.</li>
 * </ul>
 *
 * <p>The class layer exists because {@code software.amazon.smithy:smithy-model} is a single artifact containing both
 * the shape-identity/trait/node classes that Smithy Java {@code Schema} exposes and the parser/assembler. Until Smithy
 * Java publishes a lightweight shape-identity/runtime-trait artifact, {@code smithy-model} remains a transitional
 * artifact on the runtime classpath, and the parser/assembler is excluded by proving it is never resolved.
 *
 * <p>This class is internal to the AWS SDK for Java V3 build and architecture tests; it is not a customer API.
 */
public final class SmithyRuntimeBoundary {

    /** Maven group of the Smithy Java runtime modules. */
    public static final String SMITHY_JAVA_GROUP_ID = "software.amazon.smithy.java";

    /** Maven group of the Smithy model and tooling artifacts. */
    public static final String SMITHY_GROUP_ID = "software.amazon.smithy";

    /**
     * The closed set of Smithy Java artifacts (group {@link #SMITHY_JAVA_GROUP_ID}) permitted on the runtime classpath
     * of a generated service client: shape identity and runtime traits are exposed through {@code core}; Schema,
     * SerializableStruct, ShapeBuilder, Document, TypeRegistry and serde live in {@code core}; codecs live in the
     * {@code *-codec} modules; the remaining modules are their required runtime support.
     */
    public static final Set<String> SMITHY_JAVA_RUNTIME_ARTIFACTS = Set.of(
        "core",
        "io",
        "logging",
        "version-spi",
        "retries-api",
        "codec-commons",
        "json-codec",
        "cbor-codec",
        "xml-codec");

    /**
     * Smithy artifacts (group {@link #SMITHY_GROUP_ID}) that Smithy Java {@code core} requires on the runtime
     * classpath. {@code smithy-model} also contains the parser/assembler, whose classes are excluded by the class
     * layer of this boundary. No other {@link #SMITHY_GROUP_ID} artifact is permitted.
     */
    public static final Set<String> TRANSITIONAL_SMITHY_ARTIFACTS = Set.of(
        "smithy-model",
        "smithy-utils");

    /**
     * Binary-name prefixes of classes that must never be resolved on the generated-client path. A prefix ending in
     * {@code .} matches a package and its subpackages; any other prefix matches one class and its nested classes.
     */
    public static final List<String> FORBIDDEN_CLASS_PREFIXES = List.of(
        // Smithy model parsing and assembly: IDL/JSON AST parsers, ModelAssembler, prelude and trait loading.
        "software.amazon.smithy.model.loader.",
        // The assembled semantic model graph and its tooling.
        "software.amazon.smithy.model.Model",
        "software.amazon.smithy.model.validation.",
        "software.amazon.smithy.model.selector.",
        "software.amazon.smithy.model.knowledge.",
        "software.amazon.smithy.model.neighbor.",
        "software.amazon.smithy.model.transform.",
        "software.amazon.smithy.model.shapes.ModelSerializer",
        "software.amazon.smithy.model.shapes.SmithyIdlModelSerializer",
        // Smithy build, CLI, syntax, diff and code-generation toolchains.
        "software.amazon.smithy.build.",
        "software.amazon.smithy.cli.",
        "software.amazon.smithy.syntax.",
        "software.amazon.smithy.diff.",
        "software.amazon.smithy.codegen.",
        "software.amazon.smithy.traitcodegen.",
        // Artifacts built on the semantic model (the smithy-model-parser artifact class): the model-level trait,
        // validator and loader libraries of waiters, endpoint rules, JMESPath, AWS and protocol traits. Generated
        // clients carry their waiters, endpoint rules and traits as precomputed runtime metadata instead.
        "software.amazon.smithy.waiters.",
        "software.amazon.smithy.rulesengine.",
        "software.amazon.smithy.model.jmespath.",
        "software.amazon.smithy.aws.traits.",
        "software.amazon.smithy.aws.iam.",
        "software.amazon.smithy.aws.cloudformation.",
        "software.amazon.smithy.protocol.traits.",
        "software.amazon.smithy.protocoltests.",
        "software.amazon.smithy.smoketests.",
        // Smithy Java code generation, runtime model-to-schema conversion, model bundles and the dynamic client.
        "software.amazon.smithy.framework.",
        "software.amazon.smithy.java.codegen.",
        "software.amazon.smithy.java.dynamicschemas.",
        "software.amazon.smithy.java.dynamicclient.",
        "software.amazon.smithy.modelbundle.",
        // The optional V3 dynamic layer (dynamic-model and dynamic-client), which parses runtime models.
        "software.amazon.awssdk.dynamic.");

    /**
     * Shape-identity classes that {@code smithy-model} places in its {@code loader} package and that are therefore
     * exempt from {@link #FORBIDDEN_CLASS_PREFIXES}. {@code ShapeId.from} validates namespaces and identifiers with
     * {@code ParserUtils} (lexical helpers over {@code smithy-utils}' {@code SimpleParser}) and reports invalid shape
     * IDs with {@code ModelSyntaxException}. Neither class parses or assembles a model, and neither references another
     * {@code loader} class. A lightweight shape-identity artifact must carry both. Matching follows the same rules
     * as {@link #FORBIDDEN_CLASS_PREFIXES}.
     */
    public static final List<String> SHAPE_IDENTITY_LOADER_CLASSES = List.of(
        "software.amazon.smithy.model.loader.ParserUtils",
        "software.amazon.smithy.model.loader.ModelSyntaxException");

    private SmithyRuntimeBoundary() {
    }

    /**
     * Returns whether the class with the given binary name (for example {@code a.b.Outer$Inner}) is outside the
     * runtime boundary and must never be resolved on the generated-client path.
     *
     * @param binaryName the binary class name.
     * @return {@code true} if the class belongs to Smithy model parsing, assembly, or code generation.
     */
    public static boolean isForbiddenRuntimeClass(String binaryName) {
        for (String allowed : SHAPE_IDENTITY_LOADER_CLASSES) {
            if (matches(binaryName, allowed)) {
                return false;
            }
        }
        for (String prefix : FORBIDDEN_CLASS_PREFIXES) {
            if (matches(binaryName, prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String binaryName, String prefix) {
        if (prefix.endsWith(".")) {
            return binaryName.startsWith(prefix);
        }
        return binaryName.equals(prefix) || binaryName.startsWith(prefix + "$");
    }
}
