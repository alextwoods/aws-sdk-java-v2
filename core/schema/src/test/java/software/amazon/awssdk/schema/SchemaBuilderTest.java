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

package software.amazon.awssdk.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.SensitiveTrait;

/**
 * Tests for {@link SchemaBuilder} validation (Requirement 3.12) and recursive schemas (Requirements 3.9, 3.10, 3.13).
 */
class SchemaBuilderTest {
    private static final String NS = "com.example";

    private static ShapeId id(String name) {
        return ShapeId.fromParts(NS, name);
    }

    // Requirement 3.12

    @Test
    void putMember_duplicateName_throwsNamingShapeAndMember() {
        SchemaBuilder builder = Schema.structureBuilder(id("S")).putMember("a", PreludeSchemas.STRING);

        assertThatThrownBy(() -> builder.putMember("a", PreludeSchemas.INTEGER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#S")
            .hasMessageContaining("'a'");
        assertThat(builder.build().members()).hasSize(1);
    }

    @Test
    void constructor_nonAggregateType_throws() {
        assertThatThrownBy(() -> new SchemaBuilder(id("Str"), ShapeType.STRING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#Str")
            .hasMessageContaining("string");
        assertThatThrownBy(() -> new SchemaBuilder(id("Op"), ShapeType.OPERATION))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_memberId_throws() {
        assertThatThrownBy(() -> Schema.structureBuilder(ShapeId.fromParts(NS, "S", "m")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#S$m");
    }

    @Test
    void list_wrongMemberName_throws() {
        assertThatThrownBy(() -> Schema.listBuilder(id("L")).putMember("item", PreludeSchemas.STRING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#L")
            .hasMessageContaining("'item'");
    }

    @Test
    void list_secondMember_throws() {
        SchemaBuilder builder = Schema.listBuilder(id("L")).putMember("member", PreludeSchemas.STRING);

        assertThatThrownBy(() -> builder.putMember("member", PreludeSchemas.STRING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#L")
            .hasMessageContaining("'member'");
    }

    @Test
    void list_missingMember_throwsOnBuild() {
        assertThatThrownBy(() -> Schema.listBuilder(id("L")).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#L$member");
    }

    @Test
    void map_wrongMemberName_throws() {
        assertThatThrownBy(() -> Schema.mapBuilder(id("M")).putMember("k", PreludeSchemas.STRING))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#M")
            .hasMessageContaining("'k'");
    }

    @Test
    void map_missingKeyOrValue_throwsOnBuild() {
        assertThatThrownBy(() -> Schema.mapBuilder(id("M")).putMember("key", PreludeSchemas.STRING).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#M$value");
        assertThatThrownBy(() -> Schema.mapBuilder(id("M")).putMember("value", PreludeSchemas.STRING).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#M$key");
    }

    @Test
    void map_valueBeforeKey_isAccepted() {
        Schema map = Schema.mapBuilder(id("M"))
                           .putMember("value", PreludeSchemas.INTEGER)
                           .putMember("key", PreludeSchemas.STRING)
                           .build();

        assertThat(map.mapKeyMember().memberTarget()).isSameAs(PreludeSchemas.STRING);
        assertThat(map.mapValueMember().memberTarget()).isSameAs(PreludeSchemas.INTEGER);
    }

    @Test
    void union_withoutMembers_throwsOnBuild() {
        assertThatThrownBy(() -> Schema.unionBuilder(id("U")).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("com.example#U");
    }

    @Test
    void structure_withoutMembers_builds() {
        assertThat(Schema.structureBuilder(id("Empty")).build().members()).isEmpty();
    }

    @Test
    void invalidMemberName_throws() {
        assertThatThrownBy(() -> Schema.structureBuilder(id("S")).putMember("not valid", PreludeSchemas.STRING))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void build_returnsSameSchema_andFreezesBuilder() {
        SchemaBuilder builder = Schema.structureBuilder(id("S")).putMember("a", PreludeSchemas.STRING);
        Schema schema = builder.build();

        assertThat(builder.build()).isSameAs(schema);
        assertThatThrownBy(() -> builder.putMember("b", PreludeSchemas.STRING)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.shapeClass(String.class)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> builder.builderSupplier(TestStruct::builder)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nullArguments_throw() {
        SchemaBuilder builder = Schema.structureBuilder(id("S"));

        assertThatThrownBy(() -> builder.putMember(null, PreludeSchemas.STRING)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.putMember("a", (Schema) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> builder.putMember("a", (SchemaBuilder) null)).isInstanceOf(NullPointerException.class);
    }

    // Requirement 3.9: recursion

    @Test
    void selfRecursiveStructure_resolves() {
        SchemaBuilder nodeBuilder = Schema.structureBuilder(id("Node"));
        Schema node = nodeBuilder.putMember("value", PreludeSchemas.STRING)
                                 .putMember("next", nodeBuilder)
                                 .build()
                                 .resolve();

        assertThat(nodeBuilder.build()).isSameAs(node);
        assertThat(node.resolve()).isSameAs(node);
        Schema next = node.member("next");
        assertThat(next.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThat(next.memberTarget()).isSameAs(node);
        assertThat(next.member("next").memberTarget()).isSameAs(node);
        assertThat(next.members()).isSameAs(node.members());
        assertThat(next.member("value").memberTarget()).isSameAs(PreludeSchemas.STRING);
    }

    @Test
    void mutuallyRecursiveStructures_resolve() {
        // A -> B -> A, built the way the Schema_Generator emits it: a builder only for the cycle-closing target.
        SchemaBuilder aBuilder = Schema.structureBuilder(id("A"));
        Schema b = Schema.structureBuilder(id("B")).putMember("a", aBuilder).build();
        Schema a = aBuilder.putMember("b", b).putMember("name", PreludeSchemas.STRING).build().resolve();

        Schema bFromA = a.member("b").memberTarget();
        assertThat(bFromA).isSameAs(b);
        Schema aFromB = bFromA.member("a").memberTarget();
        assertThat(aFromB).isSameAs(a);
        assertThat(aFromB.id()).isEqualTo(id("A"));
        assertThat(memberNames(aFromB)).containsExactly("b", "name");
    }

    @Test
    void deferredMember_tracksResolutionOfTarget() {
        // Port of smithy-java SchemaTest.memberTargetTracksResolutionOfTarget (534de7824). Our deferred member reads
        // the builder's built schema instead of building it, so B is built (not resolved) before the first access.
        SchemaBuilder aBuilder = Schema.structureBuilder(id("A"));
        SchemaBuilder bBuilder = Schema.structureBuilder(id("B"));
        aBuilder.putMember("b", bBuilder);
        bBuilder.putMember("a", aBuilder);

        Schema a = aBuilder.build().resolve();
        Schema unresolvedB = bBuilder.build();
        assertThat(a.member("b").memberTarget()).isSameAs(unresolvedB);
        Schema b = unresolvedB.resolve();

        assertThat(unresolvedB).isNotSameAs(b);
        assertThat(a.member("b").memberTarget()).isSameAs(b);
        assertThat(a.member("b").memberTarget()).isSameAs(b);
        assertThat(a.member("b").assertMemberTargetIs(b)).isSameAs(a.member("b"));
        assertThat(b.member("a").memberTarget()).isSameAs(a);
    }

    @Test
    void recursionThroughListAndMap_resolves() {
        SchemaBuilder treeBuilder = Schema.structureBuilder(id("Tree"));
        Schema children = Schema.listBuilder(id("Children")).putMember("member", treeBuilder).build();
        Schema named = Schema.mapBuilder(id("Named"))
                             .putMember("key", PreludeSchemas.STRING)
                             .putMember("value", treeBuilder)
                             .build();
        Schema tree = treeBuilder.putMember("children", children)
                                 .putMember("named", named)
                                 .build()
                                 .resolve();

        assertThat(tree.member("children").listMember().memberTarget()).isSameAs(tree);
        assertThat(tree.member("named").mapValueMember().memberTarget()).isSameAs(tree);
        assertThat(children.listMember().type()).isEqualTo(ShapeType.STRUCTURE);
    }

    @Test
    void deferredMember_mergesTargetBuilderTraits() {
        SchemaBuilder nodeBuilder = Schema.structureBuilder(id("Node"), SensitiveTrait.INSTANCE);
        Schema node = nodeBuilder.putMember("next", nodeBuilder, new JsonNameTrait("n")).build().resolve();

        Schema next = node.member("next");
        assertThat(next.hasTrait(TraitKey.SENSITIVE_TRAIT)).isTrue();
        assertThat(next.getDirectTrait(TraitKey.SENSITIVE_TRAIT)).isNull();
        assertThat(next.getDirectTrait(TraitKey.JSON_NAME_TRAIT).getValue()).isEqualTo("n");
    }

    @Test
    void recursiveWalk_withVisitedSet_terminatesAndVisitsEachShapeOnce() {
        SchemaBuilder aBuilder = Schema.structureBuilder(id("A"));
        Schema list = Schema.listBuilder(id("AList")).putMember("member", aBuilder).build();
        Schema b = Schema.structureBuilder(id("B")).putMember("as", list).putMember("s", PreludeSchemas.STRING).build();
        Schema a = aBuilder.putMember("b", b).putMember("self", aBuilder).build().resolve();

        List<ShapeId> visited = new ArrayList<>();
        walk(a, new HashSet<>(), visited);

        assertThat(visited).containsExactlyInAnyOrder(id("A"), id("B"), id("AList"), PreludeSchemas.STRING.id());
    }

    // Requirement 3.13

    @Test
    void deferredTarget_accessedBeforeBuild_throwsNamingTarget() {
        SchemaBuilder unbuilt = Schema.structureBuilder(id("Later"));
        Schema holder = Schema.structureBuilder(id("Holder")).putMember("later", unbuilt).build();
        Schema member = holder.member("later");

        assertThat(member.type()).isEqualTo(ShapeType.STRUCTURE);
        assertThatThrownBy(member::memberTarget)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("com.example#Later");
        assertThatThrownBy(member::members)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("com.example#Later");
        assertThatThrownBy(member::memberTarget)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("com.example#Later");

        Schema later = unbuilt.putMember("x", PreludeSchemas.STRING).build();
        assertThat(member.memberTarget()).isSameAs(later);
        assertThat(member.member("x").memberTarget()).isSameAs(PreludeSchemas.STRING);
    }

    @Test
    void deferredRootMembers_areBuiltOnce_underConcurrentAccess() throws Exception {
        SchemaBuilder nodeBuilder = Schema.structureBuilder(id("Node"));
        Schema deferred = nodeBuilder.putMember("next", nodeBuilder).putMember("v", PreludeSchemas.STRING).build();
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Schema>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit((Callable<Schema>) () -> {
                    start.await();
                    return deferred.member("next");
                }));
            }
            start.countDown();
            Set<Schema> distinct = new HashSet<>();
            for (Future<Schema> result : results) {
                distinct.add(result.get());
            }
            assertThat(distinct).hasSize(1);
            assertThat(deferred.resolve().member("next")).isSameAs(distinct.iterator().next());
        } finally {
            executor.shutdownNow();
        }
    }

    private static void walk(Schema schema, Set<ShapeId> seen, List<ShapeId> visited) {
        Schema target = schema.isMember() ? schema.memberTarget() : schema;
        if (!seen.add(target.id())) {
            return;
        }
        visited.add(target.id());
        for (Schema member : target.members()) {
            walk(member, seen, visited);
        }
    }

    private static List<String> memberNames(Schema schema) {
        List<String> names = new ArrayList<>();
        for (Schema member : schema.members()) {
            names.add(member.memberName());
        }
        return names;
    }
}
