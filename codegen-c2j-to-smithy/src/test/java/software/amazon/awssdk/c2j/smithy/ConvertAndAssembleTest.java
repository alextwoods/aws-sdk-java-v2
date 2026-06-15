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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.model.node.Node;

/**
 * Standalone main: convert a C2J service-2.json to a Smithy Model, re-assemble it through Smithy's
 * validating ModelAssembler (proving the produced model is well-formed), and print a summary +
 * the converted model as Smithy JSON AST for inspection.
 */
public final class ConvertAndAssembleTest {
    public static void main(String[] args) {
        Path c2j = Path.of(args[0]);
        Model converted = C2jToSmithyConverter.convert(c2j);
        System.out.println("Converted shapes: " + converted.toSet().size());

        // Re-assemble through the validating assembler to prove well-formedness.
        String json = Node.printJson(ModelSerializer.builder().build().serialize(converted));
        // discoverModels() loads the AWS protocol trait definitions (smithy-aws-traits) from the
        // classpath so aws.protocols#* traits resolve during validation.
        Model reassembled = Model.assembler(ConvertAndAssembleTest.class.getClassLoader())
                .discoverModels(ConvertAndAssembleTest.class.getClassLoader())
                .addUnparsedModel("converted.json", json)
                .assemble()
                .unwrap();
        System.out.println("Re-assembled OK. shapes=" + reassembled.toSet().size());

        if (args.length > 1 && args[1].equals("--dump")) {
            System.out.println(json);
        }
    }
}
