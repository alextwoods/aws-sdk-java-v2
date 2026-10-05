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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SmithyRuntimeBoundaryTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "software.amazon.smithy.model.Model",
        "software.amazon.smithy.model.Model$Builder",
        "software.amazon.smithy.model.loader.ModelAssembler",
        "software.amazon.smithy.model.loader.IdlModelLoader",
        "software.amazon.smithy.model.loader.Prelude",
        "software.amazon.smithy.model.loader.ParserUtilsHelper",
        "software.amazon.smithy.model.validation.ValidatedResult",
        "software.amazon.smithy.model.knowledge.HttpBindingIndex",
        "software.amazon.smithy.model.selector.Selector",
        "software.amazon.smithy.model.transform.ModelTransformer",
        "software.amazon.smithy.model.shapes.ModelSerializer",
        "software.amazon.smithy.codegen.core.SymbolProvider",
        "software.amazon.smithy.build.SmithyBuild",
        "software.amazon.smithy.java.codegen.CodeGenerationContext",
        "software.amazon.smithy.java.dynamicschemas.SchemaConverter",
        "software.amazon.smithy.java.dynamicclient.DynamicClient",
        "software.amazon.smithy.framework.knowledge.ImplicitErrorIndex",
        "software.amazon.smithy.waiters.WaitableTrait",
        "software.amazon.smithy.rulesengine.language.EndpointRuleSet",
        "software.amazon.smithy.rulesengine.aws.language.functions.AwsPartition",
        "software.amazon.smithy.model.jmespath.LiteralExpressionJmespathRuntime",
        "software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait",
        "software.amazon.smithy.protocol.traits.Rpcv2CborTrait",
        "software.amazon.awssdk.dynamic.model.DynamicModel",
        "software.amazon.awssdk.dynamic.client.DynamicClient$Builder"
    })
    void isForbiddenRuntimeClass_parserAssemblerOrCodegenClass_isForbidden(String className) {
        assertThat(SmithyRuntimeBoundary.isForbiddenRuntimeClass(className)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "software.amazon.smithy.model.shapes.ShapeId",
        "software.amazon.smithy.model.shapes.ShapeType",
        "software.amazon.smithy.model.traits.RequiredTrait",
        "software.amazon.smithy.model.traits.TimestampFormatTrait$Format",
        "software.amazon.smithy.model.node.Node",
        "software.amazon.smithy.model.loader.ParserUtils",
        "software.amazon.smithy.model.loader.ModelSyntaxException",
        "software.amazon.smithy.model.loader.ModelSyntaxException$Builder",
        "software.amazon.smithy.model.SourceLocation",
        "software.amazon.smithy.model.ModelSomethingElse",
        "software.amazon.smithy.utils.SmithyBuilder",
        "software.amazon.smithy.java.core.schema.Schema",
        "software.amazon.smithy.java.core.serde.document.Document",
        "software.amazon.smithy.java.json.JsonCodec",
        "software.amazon.smithy.jmespath.JmespathExpression",
        "software.amazon.awssdk.dynamicity.Example",
        "software.amazon.awssdk.pipeline.AwsCallEngine",
        "software.amazon.awssdk.core.waiters.Waiter"
    })
    void isForbiddenRuntimeClass_runtimeBoundaryClass_isAllowed(String className) {
        assertThat(SmithyRuntimeBoundary.isForbiddenRuntimeClass(className)).isFalse();
    }
}
