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

package software.amazon.awssdk.dynamicclient.schema;

import java.net.URL;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The Smithy models of the converter tests, assembled with the trait definitions on the class path
 * ({@code smithy-aws-traits} and others).
 */
final class TestModels {
    static final String NS = "test.dyn";
    static final String REC = "test.rec";

    private static final Model CONVERTER = assemble("converter-test.smithy", "c2j-marker.smithy");
    private static final Model RECURSIVE = assemble("recursive.smithy");
    private static final Model CUSTOM_PROTOCOL = assemble("custom-protocol.smithy");

    private TestModels() {
    }

    /**
     * A restJson1 and sigv4 service with every simple and aggregate shape type, a resource, an error and a streaming
     * output.
     */
    static Model converter() {
        return CONVERTER;
    }

    /**
     * Self, mutual, list, map and union recursion, and no service.
     */
    static Model recursive() {
        return RECURSIVE;
    }

    /**
     * A service with a custom protocol definition whose traits list names a trait with no typed runtime class.
     */
    static Model customProtocol() {
        return CUSTOM_PROTOCOL;
    }

    static ShapeId id(String name) {
        return ShapeId.fromParts(NS, name);
    }

    static ShapeId rec(String name) {
        return ShapeId.fromParts(REC, name);
    }

    static software.amazon.awssdk.schema.ShapeId sdkId(ShapeId id) {
        return software.amazon.awssdk.schema.ShapeId.from(id.toString());
    }

    private static Model assemble(String... files) {
        ModelAssembler assembler = Model.assembler().discoverModels(Model.class.getClassLoader());
        for (String file : files) {
            URL url = TestModels.class.getResource(file);
            if (url == null) {
                throw new IllegalStateException("Test model " + file + " not found");
            }
            assembler.addImport(url);
        }
        return assembler.assemble().unwrap();
    }
}
