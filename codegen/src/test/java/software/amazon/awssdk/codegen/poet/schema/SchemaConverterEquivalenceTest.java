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

package software.amazon.awssdk.codegen.poet.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.poet.model.ModelPackageCompiler;
import software.amazon.awssdk.codegen.schema.RuntimeCompiler;
import software.amazon.awssdk.codegen.schema.TraitInclusionPolicy;
import software.amazon.awssdk.dynamicclient.schema.SchemaConverter;
import software.amazon.awssdk.dynamicclient.schema.internal.RuntimeTraitInclusion;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaIndex;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A spot check of Requirement 25.7 (Property 42 is task 13.2): the dynamic client's Schema_Converter and the
 * Schema_Generator give equal schemas for one operation of a prototype service, SQS {@code SendMessage}, compared
 * shape by shape through the input and output closures.
 */
class SchemaConverterEquivalenceTest {
    private static final String SQS_NS = "com.amazonaws.sqs";

    @TempDir
    Path tmp;

    @Test
    void sqsSendMessage_convertedSchemasEqualTheGeneratedSchemas() throws Exception {
        IntermediateModel model = PrototypeServices.prepare("sqs", tmp);
        Model smithyModel = model.smithyModel();
        ServiceShape service = smithyModel.expectShape(model.serviceId(), ServiceShape.class);
        SchemaCodegen codegen = SchemaCodegen.create(model, PrototypeServices.initializers());
        SchemaConverter converter = new SchemaConverter(smithyModel, service);

        // The runtime port of the policy gives codegen's effective set (SQS has no schemaTraits configuration).
        assertThat(RuntimeTraitInclusion.forService(smithyModel, service).effectiveTraits())
            .isEqualTo(TraitInclusionPolicy.forService(model).effectiveTraits());

        OperationShape sendMessage = smithyModel.expectShape(ShapeId.fromParts(SQS_NS, "SendMessage"),
                                                             OperationShape.class);
        Path classes = ModelPackageCompiler.compile(model, codegen, tmp.resolve("compile"));
        try (URLClassLoader loader = RuntimeCompiler.classLoader(classes)) {
            SchemaIndex generated = index(codegen, loader);
            Set<String> compared = new LinkedHashSet<>();
            for (ShapeId root : Arrays.asList(sendMessage.getInputShape(), sendMessage.getOutputShape())) {
                Schema converted = converter.getSchema(root);
                Schema expected = generated.getSchema(converted.id());
                assertThat(expected).as("generated schema of %s", converted.id()).isNotNull();
                assertEquivalent(expected, converted, compared);
            }
            // The service and operation schemas, with the protocol, auth and operation traits.
            Class<?> serviceSchemas = Class.forName(codegen.serviceSchemasClass().reflectionName(), true, loader);
            assertEquivalent((Schema) serviceSchemas.getField("SERVICE").get(null),
                             converter.getSchema(service.getId()), compared);
            assertEquivalent((Schema) serviceSchemas.getField("SEND_MESSAGE").get(null),
                             converter.getSchema(sendMessage.getId()), compared);
            assertThat(converter.getSchema(service.getId()).traits()).isNotEmpty();

            // Guards against a vacuous walk: the closure has structures, maps, lists, an enum, blobs and integers.
            assertThat(compared).contains(SQS_NS + "#SendMessageRequest", SQS_NS + "#SendMessageResult",
                                          SQS_NS + "#MessageAttributeValue", SQS_NS + "#MessageBodyAttributeMap",
                                          SQS_NS + "#BinaryList", SQS_NS + "#MessageSystemAttributeNameForSends",
                                          SQS_NS + "#Binary", SQS_NS + "#NullableInteger");
        }
    }

    private static void assertEquivalent(Schema expected, Schema actual, Set<String> compared) {
        String id = expected.id().toString();
        if (!compared.add(id)) {
            return;
        }
        assertThat(actual.id()).as("ShapeId").isEqualTo(expected.id());
        assertThat(actual.type()).as("type of %s", id).isEqualTo(expected.type());
        assertThat(new HashSet<>(actual.traits())).as("traits of %s", id).isEqualTo(new HashSet<>(expected.traits()));
        assertThat(new ArrayList<>(actual.stringEnumValues())).as("enum values of %s", id)
                                                               .isEqualTo(new ArrayList<>(expected.stringEnumValues()));
        assertThat(new ArrayList<>(actual.intEnumValues())).as("intEnum values of %s", id)
                                                            .isEqualTo(new ArrayList<>(expected.intEnumValues()));
        assertThat(names(actual.members())).as("members of %s", id).isEqualTo(names(expected.members()));
        for (int i = 0; i < expected.members().size(); i++) {
            Schema expectedMember = expected.members().get(i);
            Schema actualMember = actual.members().get(i);
            String memberId = expectedMember.id().toString();
            assertThat(actualMember.id()).as("member ShapeId").isEqualTo(expectedMember.id());
            assertThat(actualMember.memberIndex()).as("index of %s", memberId).isEqualTo(expectedMember.memberIndex());
            assertThat(new HashSet<>(actualMember.traits())).as("traits of %s", memberId)
                                                             .isEqualTo(new HashSet<>(expectedMember.traits()));
            assertThat(actualMember.memberTarget().id()).as("target of %s", memberId)
                                                        .isEqualTo(expectedMember.memberTarget().id());
            assertEquivalent(expectedMember.memberTarget(), actualMember.memberTarget(), compared);
        }
    }

    private static List<String> names(List<Schema> members) {
        List<String> names = new ArrayList<>();
        for (Schema member : members) {
            names.add(member.memberName());
        }
        return names;
    }

    private static SchemaIndex index(SchemaCodegen codegen, ClassLoader loader) throws Exception {
        Class<?> type = Class.forName(codegen.schemaIndexClass().reflectionName(), true, loader);
        return (SchemaIndex) type.getConstructor().newInstance();
    }
}
