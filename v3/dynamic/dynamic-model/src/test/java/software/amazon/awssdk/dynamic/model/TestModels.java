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

package software.amazon.awssdk.dynamic.model;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;

/** Test models written in Smithy IDL and handed to {@link DynamicModel} as the JSON AST they serialize to. */
final class TestModels {

    private TestModels() {
    }

    /** {@code idl} loaded as a runtime model through its JSON AST. */
    static DynamicModel load(String idl) {
        return DynamicModel.fromJsonAst(jsonAst(idl), "test-model.json");
    }

    /** The Smithy JSON AST of the IDL model {@code idl}, assembled with unknown traits allowed. */
    static String jsonAst(String idl) {
        ClassLoader classLoader = TestModels.class.getClassLoader();
        Model model = Model.assembler(classLoader).discoverModels(classLoader)
                           .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                           .addUnparsedModel("test-model.smithy", idl)
                           .assemble().unwrap();
        return Node.prettyPrintJson(ModelSerializer.builder().build().serialize(model));
    }

    static Node node(String json) {
        return Node.parse(json);
    }
}
