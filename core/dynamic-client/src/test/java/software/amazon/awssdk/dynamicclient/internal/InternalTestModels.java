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

package software.amazon.awssdk.dynamicclient.internal;

import java.net.URL;
import java.util.Map;
import software.amazon.awssdk.dynamicclient.schema.SchemaConverter;
import software.amazon.awssdk.dynamicclient.schema.StructDocument;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SimpleApiOperation;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Models and operations of the resolution tests. The operations stand in for task 13.7's {@code DynamicOperation}:
 * their schemas come from the Schema_Converter.
 */
final class InternalTestModels {
    private InternalTestModels() {
    }

    static Model assemble(String file) {
        URL url = InternalTestModels.class.getResource(file);
        if (url == null) {
            throw new IllegalStateException("Test model " + file + " not found");
        }
        return Model.assembler().discoverModels(Model.class.getClassLoader()).addImport(url).assemble().unwrap();
    }

    static Model assembleIdl(String idl) {
        return Model.assembler()
                    .discoverModels(Model.class.getClassLoader())
                    .addUnparsedModel("inline.smithy", idl)
                    .assemble()
                    .unwrap();
    }

    static ServiceShape onlyService(Model model) {
        return model.getServiceShapes().iterator().next();
    }

    /**
     * An operation of a model, with the converted schemas of the operation, its input and output, and its service.
     */
    static ApiOperation<StructDocument, StructDocument> operation(Model model, ShapeId operationId) {
        ServiceShape service = onlyService(model);
        SchemaConverter converter = new SchemaConverter(model, service);
        OperationShape operation = model.expectShape(operationId, OperationShape.class);
        Schema serviceSchema = converter.getSchema(service);
        return SimpleApiOperation.<StructDocument, StructDocument>builder()
                                 .schema(converter.getSchema(operation))
                                 .inputSchema(() -> converter.getSchema(operation.getInputShape()))
                                 .outputSchema(() -> converter.getSchema(operation.getOutputShape()))
                                 .service(() -> serviceSchema)
                                 .build();
    }

    /**
     * An input document of an operation, with members keyed by Smithy member name.
     */
    static StructDocument input(ApiOperation<?, ?> operation, Map<String, Document> members) {
        return StructDocument.of(operation.inputSchema(), Document.of(members));
    }
}
