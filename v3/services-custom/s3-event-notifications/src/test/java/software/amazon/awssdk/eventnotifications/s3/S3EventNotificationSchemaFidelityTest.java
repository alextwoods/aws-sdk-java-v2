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

package software.amazon.awssdk.eventnotifications.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;

/**
 * Schema fidelity (Requirement 19.3) of the hand-written event notification schemas: the published schemas are
 * exactly the shapes of {@code src/main/smithy/s3-event-notifications.smithy}, with the same shape IDs and types,
 * members in modeled order with the same targets, and the runtime-relevant traits (jsonName, timestampFormat,
 * sparse). The model is read here, in the test, only: the library never loads a Smithy model.
 */
class S3EventNotificationSchemaFidelityTest {
    private static final Path MODEL = Path.of(System.getProperty("v3.smithyModel.dir", "src/main/smithy"))
                                          .resolve("s3-event-notifications.smithy");

    @Test
    void schemas_whenComparedWithTheSmithyModel_matchExactly() {
        assertThat(MODEL).isRegularFile();
        Model model = Model.assembler().addImport(MODEL).assemble().unwrap();
        String namespace = S3EventNotificationMetadata.namespace();

        Set<ShapeId> modeled = new TreeSet<>();
        List<String> problems = new ArrayList<>();
        for (Shape shape : model.toSet()) {
            if (!shape.getId().getNamespace().equals(namespace) || shape.isMemberShape()) {
                continue;
            }
            modeled.add(shape.getId());
            Optional<Schema> published = S3EventNotificationMetadata.schema(shape.getId());
            if (published.isEmpty()) {
                problems.add(shape.getId() + ": no published schema");
                continue;
            }
            compare(shape, published.get(), problems);
        }
        Set<ShapeId> publishedIds = new TreeSet<>();
        S3EventNotificationMetadata.schemas().forEach(s -> publishedIds.add(s.id()));

        assertThat(problems).isEmpty();
        assertThat(publishedIds).isEqualTo(modeled);
    }

    @Test
    void model_whenRead_isTheOnlySmithySourceOfTheModule() throws Exception {
        try (var files = Files.list(MODEL.getParent())) {
            assertThat(files.map(p -> p.getFileName().toString())).containsExactly("s3-event-notifications.smithy");
        }
    }

    private static void compare(Shape shape, Schema schema, List<String> problems) {
        ShapeId id = shape.getId();
        if (schema.type() != shape.getType()) {
            problems.add(id + ": type " + schema.type() + " != " + shape.getType());
        }
        compareTrait(id, "sparse", shape.hasTrait(SparseTrait.class), schema.hasTrait(TraitKey.SPARSE_TRAIT),
                     problems);
        List<MemberShape> members = new ArrayList<>(shape.members());
        List<Schema> memberSchemas = schema.members();
        if (members.size() != memberSchemas.size()) {
            problems.add(id + ": " + memberSchemas.size() + " members != " + members.size());
            return;
        }
        for (int i = 0; i < members.size(); i++) {
            MemberShape member = members.get(i);
            Schema memberSchema = memberSchemas.get(i);
            ShapeId memberId = member.getId();
            if (!memberSchema.memberName().equals(member.getMemberName())) {
                problems.add(memberId + ": member " + i + " is " + memberSchema.memberName());
                continue;
            }
            if (memberSchema.memberIndex() != i) {
                problems.add(memberId + ": member index " + memberSchema.memberIndex());
            }
            if (!memberSchema.memberTarget().id().equals(member.getTarget())) {
                problems.add(memberId + ": target " + memberSchema.memberTarget().id() + " != " + member.getTarget());
            }
            String jsonName = member.getTrait(JsonNameTrait.class).map(JsonNameTrait::getValue).orElse(null);
            JsonNameTrait schemaJsonName = memberSchema.getTrait(TraitKey.JSON_NAME_TRAIT);
            String publishedJsonName = schemaJsonName == null ? null : schemaJsonName.getValue();
            if (!Objects.equals(jsonName, publishedJsonName)) {
                problems.add(memberId + ": jsonName " + publishedJsonName + " != " + jsonName);
            }
            String format = member.getTrait(TimestampFormatTrait.class).map(TimestampFormatTrait::getValue).orElse(null);
            TimestampFormatTrait schemaFormat = memberSchema.getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT);
            String publishedFormat = schemaFormat == null ? null : schemaFormat.getValue();
            if (!Objects.equals(format, publishedFormat)) {
                problems.add(memberId + ": timestampFormat " + publishedFormat + " != " + format);
            }
        }
    }

    private static void compareTrait(ShapeId id, String trait, boolean modeled, boolean published,
                                     List<String> problems) {
        if (modeled != published) {
            problems.add(id + ": @" + trait + " modeled " + modeled + ", published " + published);
        }
    }
}
