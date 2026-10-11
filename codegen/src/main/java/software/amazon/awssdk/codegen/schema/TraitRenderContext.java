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

package software.amazon.awssdk.codegen.schema;

import com.squareup.javapoet.CodeBlock;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Rendering helpers passed to a {@link TraitInitializer}. The Schema_Generator can override them, for example to
 * reference a per-package namespace constant instead of a namespace string literal.
 */
@SdkProtectedApi
public interface TraitRenderContext {
    /**
     * @return a context with the standard renderings below
     */
    static TraitRenderContext defaultContext() {
        return DefaultTraitRenderContext.INSTANCE;
    }

    /**
     * Renders a runtime {@code software.amazon.awssdk.schema.ShapeId}. The default is
     * {@code ShapeId.fromParts("namespace", "Name")}, which skips parsing at class initialization.
     *
     * @param id a shape ID without a member
     * @return the expression
     */
    default CodeBlock shapeId(ShapeId id) {
        if (id.hasMember()) {
            return CodeBlock.of("$T.fromParts($S, $S, $S)", SchemaRuntimeType.SHAPE_ID, id.getNamespace(), id.getName(),
                                id.getMember().get());
        }
        return CodeBlock.of("$T.fromParts($S, $S)", SchemaRuntimeType.SHAPE_ID, id.getNamespace(), id.getName());
    }

    /**
     * Renders a node as a runtime {@code software.amazon.awssdk.schema.serde.document.Document} with the mapping in
     * {@link DocumentLiterals}. A null node renders as {@code null}.
     *
     * @param node the node value
     * @return the expression
     */
    default CodeBlock document(Node node) {
        return DocumentLiterals.render(node);
    }

    /**
     * Renders a {@code java.util.List<String>} expression.
     *
     * @param values the elements
     * @return the expression
     */
    default CodeBlock stringList(List<String> values) {
        List<CodeBlock> elements = new ArrayList<>(values.size());
        for (String value : values) {
            elements.add(CodeBlock.of("$S", value));
        }
        return DocumentLiterals.list(String.class, elements);
    }

    /**
     * Renders a {@code java.util.List<ShapeId>} expression of runtime shape IDs.
     *
     * @param ids the elements
     * @return the expression
     */
    default CodeBlock shapeIdList(List<ShapeId> ids) {
        List<CodeBlock> elements = new ArrayList<>(ids.size());
        for (ShapeId id : ids) {
            elements.add(shapeId(id));
        }
        return DocumentLiterals.list(SchemaRuntimeType.SHAPE_ID, elements);
    }
}
