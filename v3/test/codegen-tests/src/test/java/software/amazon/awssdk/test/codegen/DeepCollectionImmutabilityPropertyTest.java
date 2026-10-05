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

package software.amazon.awssdk.test.codegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;
import net.jqwik.api.arbitraries.ListArbitrary;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.test.codegen.support.CompiledSources;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SerializableShape;
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.schema.ShapeBuilder;
import software.amazon.smithy.java.json.JsonCodec;

/**
 * Property 9 (Requirement 4.10): for any generated value containing arbitrarily nested modeled collections, no
 * mutation through any collection reachable from a public accessor changes the value or succeeds.
 *
 * <p>One hand-written model, generated and compiled once per JVM, has a nested collection of every kind: lists of lists
 * and of maps, maps of lists and of maps of lists, sets ({@code @uniqueItems}) of strings and of lists, sparse lists
 * and maps (with {@code null} elements and values, including sparse collections of collections), enum- and
 * intEnum-valued and enum-keyed collections (including nested ones), lists and maps of structures and unions that
 * contain collections, and a recursive structure and union with collections. (The root structure's name,
 * {@code Holder}, is also the name of the type registry's nested initialization-on-demand class, which the registry
 * must not let it shadow.) jqwik generates a {@link Struct} value of the root structure {@code Holder} in a plain form (members by name; {@link ArrayList} and {@link LinkedHashMap}
 * collections; enum values as their modeled strings and integers) and a list of mutation {@link Action actions}.
 *
 * <p>The generated value is the oracle: the independent deep snapshot taken before {@code build()}, which the
 * generated code never sees. The value is materialized into fresh mutable caller collections (lists, linked lists,
 * linked hash sets, varargs arrays, maps; enum members through both the enum and the string setters), which the test
 * keeps as aliases at every depth. After {@code build()} the test
 * <ul>
 *   <li>mutates the caller's aliases at any depth, which are also the collections the builder still holds;</li>
 *   <li>mutates through accessor results at every reachable level (the collection itself, iterators, list iterators,
 *   sub-lists, reversed views, key/value/entry views, entries, {@code toArray} results), through the enum accessor, the
 *   {@code AsStrings}/{@code AsIntegers} accessor and {@code getMemberValue};</li>
 *   <li>calls setters on the original builder after {@code build()} and on {@code toBuilder()} copies, and mutates the
 *   collections passed to the copies;</li>
 * </ul>
 * and checks that the built value always reads back as the snapshot. Every mutator call on a present collection must
 * throw {@link UnsupportedOperationException}, whether or not it would change a mutable collection (as V2's
 * {@code Collections.unmodifiable*} collections do). The empty auto-construct collections of absent members are V2's
 * {@code DefaultSdkAutoConstructList}/{@code Map}, whose no-op mutators return normally as in V2; they must stay empty.
 * The same mutation sweep runs over the {@code toBuilder().build()} copy and over the value deserialized from its JSON
 * form, which must also read back as the snapshot.
 */
class DeepCollectionImmutabilityPropertyTest {

    private static final String PACKAGE = "software.amazon.awssdk.services.deepimmutable";
    private static final String MODEL_PACKAGE = PACKAGE + ".model.";
    private static final String ROOT = "Holder";
    private static final JsonCodec JSON = JsonCodec.builder().build();
    /** The element, key or value every mutation attempt through an accessor tries to insert. */
    private static final String SENTINEL = "mutated$";
    /** Depth of the generated values: one level of recursion through {@code TreeNode} and {@code Choice}. */
    private static final int DEPTH = 2;

    private static final String MODEL = """
        $version: "2"
        namespace example.immutable

        @aws.protocols#awsJson1_0
        @auth([])
        service DeepImmutable {
            version: "1"
            operations: [Echo]
        }

        operation Echo {
            input := {
                holder: Holder
            }
            output := {
                holder: Holder
            }
        }

        structure Holder {
            strings: StringList
            listOfLists: StringListList
            listOfMaps: ListOfStringMaps
            mapOfLists: MapOfStringLists
            mapOfMapOfLists: MapOfMapOfLists
            stringSet: StringSet
            setOfLists: SetOfStringLists
            sparseList: SparseStringList
            sparseMap: SparseStringMap
            sparseListOfLists: SparseListOfLists
            sparseMapOfMaps: SparseMapOfMaps
            colors: ColorList
            colorSet: ColorSet
            colorKeyed: ColorKeyedMap
            colorMatrix: ColorMatrix
            colorByName: ColorByName
            levels: LevelList
            items: ItemList
            itemsByName: ItemMap
            choices: ChoiceList
            choice: Choice
            node: TreeNode
        }

        structure Item {
            tags: StringList
            matrix: StringListList
            attributes: MapOfStringLists
        }

        union Choice {
            strings: StringList
            maps: ListOfStringMaps
            item: Item
            choices: ChoiceList
        }

        structure TreeNode {
            name: String
            children: TreeNodeList
            childrenByName: TreeNodeMap
            labels: MapOfMapOfLists
        }

        list TreeNodeList {
            member: TreeNode
        }

        map TreeNodeMap {
            key: String
            value: TreeNode
        }

        list StringList {
            member: String
        }

        list StringListList {
            member: StringList
        }

        map StringMap {
            key: String
            value: String
        }

        list ListOfStringMaps {
            member: StringMap
        }

        map MapOfStringLists {
            key: String
            value: StringList
        }

        map MapOfMapOfLists {
            key: String
            value: MapOfStringLists
        }

        @uniqueItems
        list StringSet {
            member: String
        }

        @uniqueItems
        list SetOfStringLists {
            member: StringList
        }

        @sparse
        list SparseStringList {
            member: String
        }

        @sparse
        map SparseStringMap {
            key: String
            value: String
        }

        @sparse
        list SparseListOfLists {
            member: StringList
        }

        @sparse
        map SparseMapOfMaps {
            key: String
            value: SparseStringMap
        }

        enum Color {
            RED = "red"
            GREEN = "green"
            BLUE = "blue"
        }

        intEnum Level {
            LOW = 1
            HIGH = 2
        }

        list ColorList {
            member: Color
        }

        @uniqueItems
        list ColorSet {
            member: Color
        }

        map ColorKeyedMap {
            key: Color
            value: ColorList
        }

        list ColorMatrix {
            member: ColorList
        }

        map ColorByName {
            key: String
            value: Color
        }

        list LevelList {
            member: Level
        }

        list ItemList {
            member: Item
        }

        map ItemMap {
            key: String
            value: Item
        }

        list ChoiceList {
            member: Choice
        }
        """;

    // -----------------------------------------------------------------------------------------------------------------
    // The test's description of the model, mirroring the Smithy shapes above.

    private sealed interface Desc permits Scalar, ListDesc, MapDesc, Ref {
    }

    /** A leaf: a string, an enum (its modeled string) or an intEnum (its modeled integer). */
    private enum Scalar implements Desc {
        STRING, COLOR, LEVEL
    }

    private record ListDesc(Desc member, boolean sparse, boolean unique) implements Desc {
    }

    private record MapDesc(Scalar key, Desc value, boolean sparse) implements Desc {
    }

    /** A structure or union member target, by generated type name. */
    private record Ref(String type) implements Desc {
    }

    private record Shape(String type, boolean union, Map<String, Desc> members) {
    }

    private static final ListDesc STRING_LIST = list(Scalar.STRING);
    private static final ListDesc STRING_LIST_LIST = list(STRING_LIST);
    private static final ListDesc LIST_OF_STRING_MAPS = list(map(Scalar.STRING, Scalar.STRING));
    private static final MapDesc MAP_OF_STRING_LISTS = map(Scalar.STRING, STRING_LIST);
    private static final MapDesc MAP_OF_MAP_OF_LISTS = map(Scalar.STRING, MAP_OF_STRING_LISTS);
    private static final ListDesc COLOR_LIST = list(Scalar.COLOR);

    private static final Map<String, Shape> SHAPES = Map.of(
        ROOT, shape(ROOT, false,
                    "strings", STRING_LIST,
                    "listOfLists", STRING_LIST_LIST,
                    "listOfMaps", LIST_OF_STRING_MAPS,
                    "mapOfLists", MAP_OF_STRING_LISTS,
                    "mapOfMapOfLists", MAP_OF_MAP_OF_LISTS,
                    "stringSet", new ListDesc(Scalar.STRING, false, true),
                    "setOfLists", new ListDesc(STRING_LIST, false, true),
                    "sparseList", new ListDesc(Scalar.STRING, true, false),
                    "sparseMap", new MapDesc(Scalar.STRING, Scalar.STRING, true),
                    "sparseListOfLists", new ListDesc(STRING_LIST, true, false),
                    "sparseMapOfMaps", new MapDesc(Scalar.STRING, new MapDesc(Scalar.STRING, Scalar.STRING, true),
                                                   true),
                    "colors", COLOR_LIST,
                    "colorSet", new ListDesc(Scalar.COLOR, false, true),
                    "colorKeyed", map(Scalar.COLOR, COLOR_LIST),
                    "colorMatrix", list(COLOR_LIST),
                    "colorByName", map(Scalar.STRING, Scalar.COLOR),
                    "levels", list(Scalar.LEVEL),
                    "items", list(new Ref("Item")),
                    "itemsByName", map(Scalar.STRING, new Ref("Item")),
                    "choices", list(new Ref("Choice")),
                    "choice", new Ref("Choice"),
                    "node", new Ref("TreeNode")),
        "Item", shape("Item", false,
                      "tags", STRING_LIST,
                      "matrix", STRING_LIST_LIST,
                      "attributes", MAP_OF_STRING_LISTS),
        "Choice", shape("Choice", true,
                        "strings", STRING_LIST,
                        "maps", LIST_OF_STRING_MAPS,
                        "item", new Ref("Item"),
                        "choices", list(new Ref("Choice"))),
        "TreeNode", shape("TreeNode", false,
                          "name", Scalar.STRING,
                          "children", list(new Ref("TreeNode")),
                          "childrenByName", map(Scalar.STRING, new Ref("TreeNode")),
                          "labels", MAP_OF_MAP_OF_LISTS));

    private static ListDesc list(Desc member) {
        return new ListDesc(member, false, false);
    }

    private static MapDesc map(Scalar key, Desc value) {
        return new MapDesc(key, value, false);
    }

    private static Shape shape(String type, boolean union, Object... members) {
        Map<String, Desc> byName = new LinkedHashMap<>();
        for (int i = 0; i < members.length; i += 2) {
            byName.put((String) members[i], (Desc) members[i + 1]);
        }
        return new Shape(type, union, Collections.unmodifiableMap(byName));
    }

    private static boolean isCollection(Desc desc) {
        return desc instanceof ListDesc || desc instanceof MapDesc;
    }

    private static boolean hasRef(Desc desc) {
        return switch (desc) {
            case Scalar s -> false;
            case ListDesc l -> hasRef(l.member());
            case MapDesc m -> hasRef(m.value());
            case Ref r -> true;
        };
    }

    /** The enum leaf of a member (all enum leaves of one member of this model are of one kind), or {@code null}. */
    private static Scalar enumLeaf(Desc desc) {
        return switch (desc) {
            case Scalar s -> s == Scalar.STRING ? null : s;
            case ListDesc l -> enumLeaf(l.member());
            case MapDesc m -> m.key() != Scalar.STRING ? m.key() : enumLeaf(m.value());
            case Ref r -> null;
        };
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Oracle values and actions.

    /** A structure or union value in plain form: present members by name. */
    record Struct(String type, Map<String, Object> members) {
    }

    enum ActionKind {
        /** Mutate one of the caller's (and so the builder's) collections at any depth. */
        ALIAS,
        /** Attempt one mutation through a collection reachable from an accessor of the built value. */
        ACCESSOR,
        /** Call a setter of the original builder after {@code build()}. */
        BUILDER_SETTER,
        /** Set a member of a {@code toBuilder()} copy, build it, and mutate the collections passed to the copy. */
        TO_BUILDER
    }

    record Action(ActionKind kind, int target, int operation) {
    }

    @Provide
    Arbitrary<Struct> holders() {
        return shapeValue(ROOT, DEPTH).map(Struct.class::cast);
    }

    @Provide
    Arbitrary<List<Action>> actions() {
        return Combinators.combine(Arbitraries.of(ActionKind.class), Arbitraries.integers().between(0, 1 << 20),
                                   Arbitraries.integers().between(0, 1 << 10))
                          .as(Action::new)
                          .list().ofMinSize(1).ofMaxSize(16);
    }

    private static Arbitrary<Object> plainValue(Desc desc, int depth) {
        return switch (desc) {
            case Scalar s -> scalarValue(s);
            case ListDesc l -> {
                Arbitrary<Object> member = plainValue(l.member(), depth);
                ListArbitrary<Object> lists = (l.sparse() ? member.injectNull(0.2) : member).list().ofMaxSize(3);
                yield (l.unique() ? lists.uniqueElements() : lists).map(ArrayList::new).map(Object.class::cast);
            }
            case MapDesc m -> {
                Arbitrary<Object> value = plainValue(m.value(), depth);
                yield Arbitraries.maps(scalarValue(m.key()), m.sparse() ? value.injectNull(0.2) : value)
                                 .ofMaxSize(3).map(LinkedHashMap::new).map(Object.class::cast);
            }
            case Ref r -> shapeValue(r.type(), depth - 1);
        };
    }

    private static Arbitrary<Object> scalarValue(Scalar scalar) {
        // Small alphabets, so that sets and map keys collide and unknown enum values ("purple", 7) appear.
        return switch (scalar) {
            case STRING -> Arbitraries.of("", "a", "b", "c", "a b", "\u00e9").map(Object.class::cast);
            case COLOR -> Arbitraries.of("red", "green", "blue", "purple").map(Object.class::cast);
            case LEVEL -> Arbitraries.of(1, 2, 7).map(Object.class::cast);
        };
    }

    /** A structure with each member present or absent, or a union with exactly one member; no references at depth 0. */
    private static Arbitrary<Object> shapeValue(String type, int depth) {
        Shape shape = SHAPES.get(type);
        List<String> names = new ArrayList<>();
        List<Arbitrary<Object>> values = new ArrayList<>();
        shape.members().forEach((name, desc) -> {
            if (depth > 0 || !hasRef(desc)) {
                names.add(name);
                values.add(plainValue(desc, depth));
            }
        });
        if (shape.union()) {
            List<Arbitrary<? extends Object>> options = new ArrayList<>();
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                options.add(values.get(i).map(v -> new Struct(type, members(name, v))));
            }
            return Arbitraries.<Object>oneOf(options);
        }
        List<Arbitrary<Object>> optional = values.stream().map(v -> v.injectNull(0.35)).toList();
        return Combinators.combine(optional).as(present -> {
            Map<String, Object> members = new LinkedHashMap<>();
            for (int i = 0; i < names.size(); i++) {
                if (present.get(i) != null) {
                    members.put(names.get(i), present.get(i));
                }
            }
            return new Struct(type, members);
        });
    }

    private static Map<String, Object> members(String name, Object value) {
        Map<String, Object> members = new LinkedHashMap<>();
        members.put(name, value);
        return members;
    }

    /** A fixed plain value of {@code desc}, used for alias mutations and setter calls after {@code build()}. */
    private static Object sample(Desc desc) {
        return switch (desc) {
            case Scalar s -> sampleScalar(s);
            case ListDesc l -> new ArrayList<>(List.of(sample(l.member())));
            case MapDesc m -> new LinkedHashMap<>(Map.of(sampleScalar(m.key()), sample(m.value())));
            case Ref r -> new Struct(r.type(), new LinkedHashMap<>());
        };
    }

    private static Object sampleScalar(Scalar scalar) {
        return switch (scalar) {
            case STRING -> "sample";
            case COLOR -> "green";
            case LEVEL -> 2;
        };
    }

    // -----------------------------------------------------------------------------------------------------------------
    // The property.

    // Feature: aws-sdk-java-v3, Property 9: Generated collection state is deeply immutable
    @Property(tries = 1_000)
    void generatedCollectionStateIsDeeplyImmutable(@ForAll("holders") Struct snapshot,
                                                   @ForAll("actions") List<Action> actions,
                                                   @ForAll Random random) {
        Model model = Model.INSTANCE;
        List<Alias> aliases = new ArrayList<>();
        Object builder = model.builder(snapshot, aliases, random);
        Object built = model.build(ROOT, builder);
        model.assertReadsAs(built, snapshot, "built value");

        for (Action action : actions) {
            switch (action.kind()) {
                case ALIAS -> {
                    if (!aliases.isEmpty()) {
                        aliases.get(action.target() % aliases.size()).mutate(model, action.operation(), random);
                    }
                }
                case ACCESSOR -> {
                    List<Reached> reached = model.reach(built, ROOT);
                    Reached target = reached.get(action.target() % reached.size());
                    List<CollectionOp> ops = applicable(target);
                    attempt(target, ops.get(action.operation() % ops.size()));
                }
                case BUILDER_SETTER -> {
                    String member = rootMember(action.target());
                    Desc desc = SHAPES.get(ROOT).members().get(member);
                    if (action.operation() % 4 == 0) {
                        model.setAbsent(builder, ROOT, member, desc);
                    } else {
                        model.set(builder, ROOT, member, desc, sample(desc), new ArrayList<>(), random);
                    }
                }
                case TO_BUILDER -> toBuilderCopy(model, built, snapshot, action, random);
            }
            model.assertReadsAs(built, snapshot, "built value after " + action);
        }

        // Every alias at once, including nested ones inside outer collections the test has already cleared.
        for (Alias alias : aliases) {
            alias.mutate(model, random.nextInt(1 << 10), random);
        }
        model.assertReadsAs(built, snapshot, "built value after mutating every caller alias");

        // Every applicable mutation through every reachable collection: of the built value, of a toBuilder() copy, and
        // of the value deserialized from JSON.
        sweep(model, built, snapshot, "built value");
        sweep(model, model.build(ROOT, model.toBuilder(built)), snapshot, "toBuilder().build() copy");
        Object deserialized = JSON.deserializeShape(JSON.serializeToString((SerializableShape) built),
                                                    (ShapeBuilder<?>) model.newBuilder(ROOT));
        sweep(model, deserialized, snapshot, "deserialized value");
        model.assertReadsAs(built, snapshot, "built value after every sweep");

        Statistics.label("caller aliases").collect(bucket(aliases.size()));
        Statistics.label("reachable collections").collect(bucket(model.reach(built, ROOT).size()));
    }

    private static String bucket(int count) {
        return count == 0 ? "0" : count < 10 ? "1-9" : count < 50 ? "10-49" : count < 200 ? "50-199" : "200+";
    }

    private static String rootMember(int index) {
        List<String> members = List.copyOf(SHAPES.get(ROOT).members().keySet());
        return members.get(index % members.size());
    }

    /**
     * Sets one member of a {@code toBuilder()} copy (to a fresh caller collection, or absent), builds it, and mutates
     * the collections passed to the copy: the copy reads as the snapshot with that member replaced, and the original is
     * unaffected (checked by the caller).
     */
    private static void toBuilderCopy(Model model, Object built, Struct snapshot, Action action, Random random) {
        Object copyBuilder = model.toBuilder(built);
        String member = rootMember(action.target());
        Desc desc = SHAPES.get(ROOT).members().get(member);
        Map<String, Object> expectedMembers = new LinkedHashMap<>(snapshot.members());
        List<Alias> copyAliases = new ArrayList<>();
        if (action.operation() % 4 == 0) {
            model.setAbsent(copyBuilder, ROOT, member, desc);
            expectedMembers.remove(member);
        } else {
            Object value = sample(desc);
            model.set(copyBuilder, ROOT, member, desc, value, copyAliases, random);
            expectedMembers.put(member, sample(desc));
        }
        Struct expected = new Struct(ROOT, expectedMembers);
        Object copy = model.build(ROOT, copyBuilder);
        model.assertReadsAs(copy, expected, "toBuilder() copy with " + member + " replaced");
        for (Alias alias : copyAliases) {
            alias.mutate(model, action.operation(), random);
        }
        model.assertReadsAs(copy, expected, "toBuilder() copy after mutating the collections passed to it");
    }

    private static void sweep(Model model, Object subject, Struct snapshot, String label) {
        model.assertReadsAs(subject, snapshot, label);
        for (Reached reached : model.reach(subject, label)) {
            for (CollectionOp op : applicable(reached)) {
                attempt(reached, op);
            }
        }
        model.assertReadsAs(subject, snapshot, label + " after every mutation attempt");
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Mutation attempts through accessor results.

    /**
     * A collection reachable from a public accessor. {@code strict} unless it is the auto-construct collection of an
     * absent member (V2's {@code DefaultSdkAutoConstructList}/{@code Map}).
     */
    private record Reached(Object collection, String path, boolean strict) {
    }

    /**
     * One mutator call. {@code needsElement} calls run only on nonempty collections (they need an element or an entry
     * to act on). A {@code probe} is not a mutator (it writes into a copy the collection hands out) and must return
     * normally.
     */
    private record CollectionOp(String name, boolean needsElement, boolean probe, Consumer<Object> run) {
    }

    private static List<CollectionOp> applicable(Reached reached) {
        boolean list = reached.collection() instanceof List;
        boolean empty = list ? ((List<?>) reached.collection()).isEmpty() : ((Map<?, ?>) reached.collection()).isEmpty();
        return (list ? LIST_OPS : MAP_OPS).stream().filter(op -> !op.needsElement() || !empty).toList();
    }

    private static void attempt(Reached reached, CollectionOp op) {
        Object before = shallowCopy(reached.collection());
        Throwable thrown = null;
        try {
            op.run().accept(reached.collection());
        } catch (RuntimeException e) {
            thrown = e;
        }
        String where = reached.path() + " (" + reached.collection().getClass().getName() + "): " + op.name();
        if (!before.equals(shallowCopy(reached.collection()))) {
            throw new AssertionError("Mutation changed " + where + "; before " + before + ", after "
                                     + reached.collection(), thrown);
        }
        if (op.probe()) {
            if (thrown != null) {
                throw new AssertionError("Non-mutating probe failed on " + where, thrown);
            }
        } else if (reached.strict() ? !(thrown instanceof UnsupportedOperationException)
                                    : thrown != null && !(thrown instanceof UnsupportedOperationException)) {
            throw new AssertionError("Expected UnsupportedOperationException from " + where + ", but "
                                     + (thrown == null ? "it returned normally" : "it threw " + thrown), thrown);
        }
    }

    private static Object shallowCopy(Object collection) {
        return collection instanceof List<?> list ? new ArrayList<>(list) : new LinkedHashMap<>((Map<?, ?>) collection);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object collection) {
        return (List<Object>) collection;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> asMap(Object collection) {
        return (Map<Object, Object>) collection;
    }

    private static Object firstKey(Object map) {
        return asMap(map).keySet().iterator().next();
    }

    private static CollectionOp op(String name, Consumer<Object> run) {
        return new CollectionOp(name, false, false, run);
    }

    private static CollectionOp onElement(String name, Consumer<Object> run) {
        return new CollectionOp(name, true, false, run);
    }

    private static final List<CollectionOp> LIST_OPS = List.of(
        op("add(e)", c -> asList(c).add(SENTINEL)),
        op("add(0, e)", c -> asList(c).add(0, SENTINEL)),
        op("addAll([e])", c -> asList(c).addAll(List.of(SENTINEL))),
        op("addAll([])", c -> asList(c).addAll(List.of())),
        op("addAll(0, [e])", c -> asList(c).addAll(0, List.of(SENTINEL))),
        op("addFirst(e)", c -> asList(c).addFirst(SENTINEL)),
        op("addLast(e)", c -> asList(c).addLast(SENTINEL)),
        onElement("set(0, e)", c -> asList(c).set(0, SENTINEL)),
        onElement("remove(0)", c -> asList(c).remove(0)),
        onElement("removeFirst()", c -> asList(c).removeFirst()),
        onElement("removeLast()", c -> asList(c).removeLast()),
        onElement("remove(first element)", c -> asList(c).remove(asList(c).get(0))),
        op("remove(absent element)", c -> asList(c).remove((Object) SENTINEL)),
        onElement("removeAll([first element])", c -> asList(c).removeAll(Collections.singletonList(asList(c).get(0)))),
        op("removeAll([])", c -> asList(c).removeAll(List.of())),
        op("retainAll([])", c -> asList(c).retainAll(List.of())),
        op("retainAll(all elements)", c -> asList(c).retainAll(new ArrayList<>(asList(c)))),
        op("removeIf(true)", c -> asList(c).removeIf(e -> true)),
        op("removeIf(false)", c -> asList(c).removeIf(e -> false)),
        op("clear()", c -> asList(c).clear()),
        op("replaceAll(identity)", c -> asList(c).replaceAll(UnaryOperator.identity())),
        op("sort(all equal)", c -> asList(c).sort((a, b) -> 0)),
        onElement("iterator().remove()", c -> {
            Iterator<Object> iterator = asList(c).iterator();
            iterator.next();
            iterator.remove();
        }),
        onElement("listIterator().set(e)", c -> {
            ListIterator<Object> iterator = asList(c).listIterator();
            iterator.next();
            iterator.set(SENTINEL);
        }),
        op("listIterator().add(e)", c -> asList(c).listIterator().add(SENTINEL)),
        onElement("listIterator(size).previous().remove()", c -> {
            ListIterator<Object> iterator = asList(c).listIterator(asList(c).size());
            iterator.previous();
            iterator.remove();
        }),
        op("subList(0, size).clear()", c -> asList(c).subList(0, asList(c).size()).clear()),
        op("subList(0, size).add(e)", c -> asList(c).subList(0, asList(c).size()).add(SENTINEL)),
        onElement("subList(0, size).set(0, e)", c -> asList(c).subList(0, asList(c).size()).set(0, SENTINEL)),
        op("subList(0, size).removeIf(true)", c -> asList(c).subList(0, asList(c).size()).removeIf(e -> true)),
        op("reversed().add(e)", c -> asList(c).reversed().add(SENTINEL)),
        op("reversed().clear()", c -> asList(c).reversed().clear()),
        onElement("reversed().removeFirst()", c -> asList(c).reversed().removeFirst()),
        new CollectionOp("toArray()[0] = e", true, true, c -> asList(c).toArray()[0] = SENTINEL),
        new CollectionOp("toArray(T[])[0] = e", true, true, c -> asList(c).toArray(new Object[0])[0] = SENTINEL));

    private static final List<CollectionOp> MAP_OPS = List.of(
        op("put(new key, v)", c -> asMap(c).put(SENTINEL, SENTINEL)),
        onElement("put(existing key, v)", c -> asMap(c).put(firstKey(c), SENTINEL)),
        op("putAll({new key: v})", c -> asMap(c).putAll(Map.of(SENTINEL, SENTINEL))),
        op("putAll({})", c -> asMap(c).putAll(Map.of())),
        op("putIfAbsent(new key, v)", c -> asMap(c).putIfAbsent(SENTINEL, SENTINEL)),
        onElement("putIfAbsent(existing key, v)", c -> asMap(c).putIfAbsent(firstKey(c), SENTINEL)),
        onElement("remove(existing key)", c -> asMap(c).remove(firstKey(c))),
        op("remove(absent key)", c -> asMap(c).remove(SENTINEL)),
        onElement("remove(existing key, its value)", c -> asMap(c).remove(firstKey(c), asMap(c).get(firstKey(c)))),
        op("remove(absent key, v)", c -> asMap(c).remove(SENTINEL, SENTINEL)),
        op("clear()", c -> asMap(c).clear()),
        onElement("replace(existing key, v)", c -> asMap(c).replace(firstKey(c), SENTINEL)),
        op("replace(absent key, v)", c -> asMap(c).replace(SENTINEL, SENTINEL)),
        onElement("replace(existing key, its value, v)",
                  c -> asMap(c).replace(firstKey(c), asMap(c).get(firstKey(c)), SENTINEL)),
        op("replaceAll(identity)", c -> asMap(c).replaceAll((k, v) -> v)),
        op("compute(new key)", c -> asMap(c).compute(SENTINEL, (k, v) -> SENTINEL)),
        op("computeIfAbsent(new key, v)", c -> asMap(c).computeIfAbsent(SENTINEL, k -> SENTINEL)),
        op("computeIfAbsent(new key, null)", c -> asMap(c).computeIfAbsent(SENTINEL, k -> null)),
        onElement("computeIfPresent(existing key, null)", c -> asMap(c).computeIfPresent(firstKey(c), (k, v) -> null)),
        op("computeIfPresent(absent key)", c -> asMap(c).computeIfPresent(SENTINEL, (k, v) -> SENTINEL)),
        op("merge(new key, v)", c -> asMap(c).merge(SENTINEL, SENTINEL, (a, b) -> b)),
        onElement("keySet().remove(existing key)", c -> asMap(c).keySet().remove(firstKey(c))),
        op("keySet().clear()", c -> asMap(c).keySet().clear()),
        op("keySet().removeIf(true)", c -> asMap(c).keySet().removeIf(k -> true)),
        op("keySet().retainAll([])", c -> asMap(c).keySet().retainAll(Set.of())),
        onElement("keySet().iterator().remove()", c -> {
            Iterator<Object> iterator = asMap(c).keySet().iterator();
            iterator.next();
            iterator.remove();
        }),
        op("values().clear()", c -> asMap(c).values().clear()),
        onElement("values().remove(existing value)", c -> asMap(c).values().remove(asMap(c).get(firstKey(c)))),
        op("values().removeIf(true)", c -> asMap(c).values().removeIf(v -> true)),
        op("values().retainAll([])", c -> asMap(c).values().retainAll(Set.of())),
        onElement("values().iterator().remove()", c -> {
            Iterator<Object> iterator = asMap(c).values().iterator();
            iterator.next();
            iterator.remove();
        }),
        op("entrySet().clear()", c -> asMap(c).entrySet().clear()),
        op("entrySet().removeIf(true)", c -> asMap(c).entrySet().removeIf(e -> true)),
        onElement("entrySet().iterator().remove()", c -> {
            Iterator<Map.Entry<Object, Object>> iterator = asMap(c).entrySet().iterator();
            iterator.next();
            iterator.remove();
        }),
        onElement("entrySet().iterator().next().setValue(v)",
                  c -> asMap(c).entrySet().iterator().next().setValue(SENTINEL)),
        onElement("entrySet().forEach(setValue(v))", c -> asMap(c).entrySet().forEach(e -> e.setValue(SENTINEL))),
        onElement("entrySet().stream().findFirst().setValue(v)",
                  c -> asMap(c).entrySet().stream().findFirst().orElseThrow().setValue(SENTINEL)),
        onElement("entrySet().toArray()[0].setValue(v)",
                  c -> ((Map.Entry<?, Object>) asMap(c).entrySet().toArray()[0]).setValue(SENTINEL)),
        onElement("entrySet().toArray(T[])[0].setValue(v)",
                  c -> asMap(c).entrySet().toArray(new Map.Entry[0])[0].setValue(SENTINEL)));

    // -----------------------------------------------------------------------------------------------------------------
    // Caller aliases.

    /**
     * A mutable collection or varargs array the caller passed to a builder (directly or nested), with its description
     * and whether it holds enum constants rather than modeled strings or integers.
     */
    private record Alias(Object target, Desc desc, boolean typed) {

        /** Mutates the alias; every mutation of a caller collection succeeds. */
        void mutate(Model model, int operation, Random random) {
            switch (target) {
                case Object[] array -> {
                    if (array.length > 0) {
                        array[operation % array.length] = fresh(model, ((ListDesc) desc).member(), random);
                    }
                }
                case List<?> list -> mutateList(asList(list), model, operation, random);
                case Collection<?> set -> {
                    @SuppressWarnings("unchecked")
                    Collection<Object> elements = (Collection<Object>) set;
                    switch (operation % 3) {
                        case 0 -> elements.add(fresh(model, ((ListDesc) desc).member(), random));
                        case 1 -> elements.clear();
                        default -> {
                            if (!elements.isEmpty()) {
                                elements.remove(elements.iterator().next());
                            }
                        }
                    }
                }
                case Map<?, ?> map -> mutateMap(asMap(map), model, operation, random);
                default -> throw new AssertionError("Unexpected alias " + target);
            }
        }

        private void mutateList(List<Object> list, Model model, int operation, Random random) {
            Desc member = ((ListDesc) desc).member();
            switch (operation % 6) {
                case 0 -> list.add(fresh(model, member, random));
                case 1 -> list.clear();
                case 2 -> list.add(0, fresh(model, member, random));
                case 3 -> {
                    if (!list.isEmpty()) {
                        list.set(list.size() - 1, fresh(model, member, random));
                    }
                }
                case 4 -> {
                    if (!list.isEmpty()) {
                        list.remove(0);
                    }
                }
                default -> Collections.reverse(list);
            }
        }

        private void mutateMap(Map<Object, Object> map, Model model, int operation, Random random) {
            MapDesc mapDesc = (MapDesc) desc;
            switch (operation % 5) {
                case 0 -> map.put(model.input(mapDesc.key(), "blue", typed, new ArrayList<>(), random),
                                  fresh(model, mapDesc.value(), random));
                case 1 -> map.clear();
                case 2 -> {
                    if (!map.isEmpty()) {
                        map.remove(map.keySet().iterator().next());
                    }
                }
                case 3 -> {
                    if (!map.isEmpty()) {
                        map.entrySet().iterator().next().setValue(fresh(model, mapDesc.value(), random));
                    }
                }
                default -> map.values().clear();
            }
        }

        private Object fresh(Model model, Desc member, Random random) {
            return model.input(member, sample(member), typed, new ArrayList<>(), random);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    // The generated model, compiled once, driven by reflection.

    private static final class Model {
        static final Model INSTANCE = new Model();

        private final ClassLoader loader;
        private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();
        private final Map<String, Method> methods = new ConcurrentHashMap<>();

        private Model() {
            try {
                Path work = Files.createTempDirectory("deep-collection-immutability-");
                Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteRecursively(work)));
                Path models = Files.createDirectories(work.resolve("model"));
                Files.writeString(models.resolve("immutable.smithy"), MODEL, StandardCharsets.UTF_8);
                AwsCodegenResult result = AwsCodegen.generate(
                    new AwsCodegenRequest("example.immutable#DeepImmutable", PACKAGE, models, work.resolve("out")),
                    List.of());
                this.loader = CompiledSources.compile(result.sourceRoot(), work.resolve("classes")).loader();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        Class<?> type(String name) {
            return classes.computeIfAbsent(name, n -> {
                try {
                    return Class.forName(MODEL_PACKAGE + n, true, loader);
                } catch (ClassNotFoundException e) {
                    throw new AssertionError(e);
                }
            });
        }

        // Building.

        Object newBuilder(String type) {
            return invoke(method(type(type), "builder"), null);
        }

        Object build(String type, Object builder) {
            return invoke(method(type(type + "$Builder"), "build"), builder);
        }

        Object toBuilder(Object value) {
            return invoke(method(value.getClass(), "toBuilder"), value);
        }

        /** A builder of {@code value}'s type with every present member set from fresh caller collections. */
        Object builder(Struct value, List<Alias> aliases, Random random) {
            Object builder = newBuilder(value.type());
            Map<String, Desc> members = SHAPES.get(value.type()).members();
            value.members().forEach((name, v) -> set(builder, value.type(), name, members.get(name), v, aliases,
                                                     random));
            return builder;
        }

        /**
         * Sets a member from a plain value, through a randomly chosen setter: the collection or varargs setter, and for
         * enum members the enum setter (if every value is known) or the string/integer one.
         */
        void set(Object builder, String type, String member, Desc desc, Object value, List<Alias> aliases,
                 Random random) {
            Scalar enumLeaf = enumLeaf(desc);
            boolean typed = enumLeaf != null && allKnown(desc, value) && random.nextBoolean();
            String name = setterName(member, enumLeaf, typed);
            Method varargs = desc instanceof ListDesc && random.nextInt(3) == 0 ? varargsSetter(type, name) : null;
            if (varargs != null) {
                List<?> elements = (List<?>) value;
                Object array = Array.newInstance(varargs.getParameterTypes()[0].getComponentType(), elements.size());
                for (int i = 0; i < elements.size(); i++) {
                    Object element = elements.get(i);
                    Array.set(array, i, element == null ? null
                                                        : input(((ListDesc) desc).member(), element, typed, aliases,
                                                                random));
                }
                aliases.add(new Alias(array, desc, typed));
                invoke(varargs, builder, array);
            } else {
                Object argument = input(desc, value, typed, aliases, random);
                invoke(setter(type, name, argument.getClass()), builder, argument);
            }
        }

        /** Sets a member absent, through its collection, structure or string setter. */
        void setAbsent(Object builder, String type, String member, Desc desc) {
            Class<?> parameter = switch (desc) {
                case Scalar s -> String.class;
                case ListDesc l -> ArrayList.class;
                case MapDesc m -> LinkedHashMap.class;
                case Ref r -> type(r.type());
            };
            invoke(setter(type, setterName(member, enumLeaf(desc), false), parameter), builder, (Object) null);
        }

        /**
         * A fresh caller value of a plain value: new mutable collections (recorded as aliases), enum constants if
         * {@code typed}, and structures built from fresh builders.
         */
        Object input(Desc desc, Object value, boolean typed, List<Alias> aliases, Random random) {
            return switch (desc) {
                case Scalar s -> typed && s != Scalar.STRING ? enumConstant(s, value) : value;
                case ListDesc l -> {
                    Collection<Object> collection = l.unique() && l.member() instanceof Scalar && random.nextBoolean()
                                                    ? new LinkedHashSet<>()
                                                    : random.nextBoolean() ? new ArrayList<>() : new LinkedList<>();
                    for (Object element : (List<?>) value) {
                        collection.add(element == null ? null : input(l.member(), element, typed, aliases, random));
                    }
                    aliases.add(new Alias(collection, l, typed));
                    yield collection;
                }
                case MapDesc m -> {
                    Map<Object, Object> map = new LinkedHashMap<>();
                    ((Map<?, ?>) value).forEach((k, v) -> map.put(input(m.key(), k, typed, aliases, random),
                                                                  v == null ? null
                                                                            : input(m.value(), v, typed, aliases,
                                                                                    random)));
                    aliases.add(new Alias(map, m, typed));
                    yield map;
                }
                case Ref r -> build(r.type(), builder((Struct) value, aliases, random));
            };
        }

        private Object enumConstant(Scalar scalar, Object value) {
            return scalar == Scalar.COLOR
                   ? invoke(method(type("Color"), "fromValue", String.class), null, value)
                   : invoke(method(type("Level"), "fromValue", Integer.class), null, value);
        }

        private static boolean allKnown(Desc desc, Object value) {
            return switch (desc) {
                case Scalar s -> switch (s) {
                    case STRING -> true;
                    case COLOR -> Set.of("red", "green", "blue").contains(value);
                    case LEVEL -> Set.of(1, 2).contains(value);
                };
                case ListDesc l -> ((List<?>) value).stream().allMatch(e -> e == null || allKnown(l.member(), e));
                case MapDesc m -> ((Map<?, ?>) value).entrySet().stream().allMatch(
                    e -> allKnown(m.key(), e.getKey()) && (e.getValue() == null || allKnown(m.value(), e.getValue())));
                case Ref r -> true;
            };
        }

        private static String setterName(String member, Scalar enumLeaf, boolean typed) {
            if (enumLeaf == null || typed) {
                return member;
            }
            return member + (enumLeaf == Scalar.LEVEL ? "WithIntegers" : "WithStrings");
        }

        private Method setter(String type, String name, Class<?> argument) {
            Class<?> builder = type(type + "$Builder");
            return methods.computeIfAbsent(builder.getName() + "#" + name + "(" + argument.getName() + ")", key -> {
                for (Method method : builder.getMethods()) {
                    if (method.getName().equals(name) && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(argument)) {
                        return method;
                    }
                }
                throw new AssertionError("No setter " + name + "(" + argument.getName() + ") on " + builder);
            });
        }

        private Method varargsSetter(String type, String name) {
            Class<?> builder = type(type + "$Builder");
            for (Method method : builder.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 1 && method.isVarArgs()
                    && !Consumer.class.isAssignableFrom(method.getParameterTypes()[0].getComponentType())) {
                    return method;
                }
            }
            return null;
        }

        // Reading.

        /** Fails unless {@code value} reads, through its accessors, exactly as {@code expected}. */
        void assertReadsAs(Object value, Struct expected, String label) {
            Object actual = view(new Ref(expected.type()), value, expected.type());
            if (!expected.equals(actual)) {
                throw new AssertionError(label + " does not read as its snapshot.\nexpected: " + expected
                                         + "\nactual:   " + actual);
            }
        }

        /** The plain form of a value read through its accessors; enum state through the string/integer accessors. */
        private Object view(Desc desc, Object value, String path) {
            return switch (desc) {
                case Scalar s -> value;
                case ListDesc l -> {
                    if (!(value instanceof List<?> list)) {
                        throw new AssertionError(path + " is not a List: " + value);
                    }
                    List<Object> plain = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        Object e = list.get(i);
                        plain.add(e == null ? null : view(l.member(), e, path + "[" + i + "]"));
                    }
                    yield plain;
                }
                case MapDesc m -> {
                    if (!(value instanceof Map<?, ?> map)) {
                        throw new AssertionError(path + " is not a Map: " + value);
                    }
                    Map<Object, Object> plain = new LinkedHashMap<>();
                    map.forEach((k, v) -> plain.put(k, v == null ? null : view(m.value(), v, path + "[" + k + "]")));
                    yield plain;
                }
                case Ref r -> {
                    Map<String, Object> members = new LinkedHashMap<>();
                    SHAPES.get(r.type()).members().forEach((name, memberDesc) -> {
                        Object member = call(value, plainAccessor(name, memberDesc));
                        String memberPath = path + "." + name;
                        if (isCollection(memberDesc)) {
                            if (!(Boolean) call(value, "has" + Character.toUpperCase(name.charAt(0))
                                                        + name.substring(1))) {
                                if (!(member instanceof SdkAutoConstructList || member instanceof SdkAutoConstructMap)) {
                                    throw new AssertionError(memberPath + " is absent but reads as " + member);
                                }
                                return;
                            }
                        } else if (member == null) {
                            return;
                        }
                        members.put(name, view(memberDesc, member, memberPath));
                    });
                    yield new Struct(r.type(), members);
                }
            };
        }

        private static String plainAccessor(String member, Desc desc) {
            Scalar enumLeaf = enumLeaf(desc);
            return enumLeaf == null ? member : member + (enumLeaf == Scalar.LEVEL ? "AsIntegers" : "AsStrings");
        }

        /**
         * Every collection reachable from the public accessors of {@code value}: each collection member through its
         * accessor, its {@code AsStrings}/{@code AsIntegers} accessor and {@code getMemberValue}, and every nested
         * collection, structure and union.
         */
        List<Reached> reach(Object value, String path) {
            List<Reached> reached = new ArrayList<>();
            reachShape(value, path, reached);
            return reached;
        }

        private void reachShape(Object value, String path, List<Reached> out) {
            String type = value.getClass().getSimpleName();
            Schema schema = ((SerializableStruct) value).schema();
            SHAPES.get(type).members().forEach((name, desc) -> {
                String memberPath = path + "." + name;
                if (desc instanceof Ref) {
                    Object member = call(value, name);
                    if (member != null) {
                        reachShape(member, memberPath, out);
                    }
                } else if (isCollection(desc)) {
                    reachCollection(desc, call(value, name), memberPath + "()", true, out);
                    if (enumLeaf(desc) != null) {
                        String accessor = plainAccessor(name, desc);
                        reachCollection(desc, call(value, accessor), path + "." + accessor + "()", false, out);
                    }
                    Object state = ((SerializableStruct) value).getMemberValue(schema.member(name));
                    if (state != null) {
                        out.add(new Reached(state, path + ".getMemberValue(" + name + ")", true));
                    }
                }
            });
        }

        private void reachCollection(Desc desc, Object collection, String path, boolean shapes, List<Reached> out) {
            boolean autoConstruct = collection instanceof SdkAutoConstructList
                                    || collection instanceof SdkAutoConstructMap;
            out.add(new Reached(collection, path, !autoConstruct));
            if (desc instanceof ListDesc l) {
                List<?> list = (List<?>) collection;
                for (int i = 0; i < list.size(); i++) {
                    reachElement(l.member(), list.get(i), path + "[" + i + "]", shapes, out);
                }
            } else {
                MapDesc m = (MapDesc) desc;
                ((Map<?, ?>) collection).forEach((k, v) -> reachElement(m.value(), v, path + "[" + k + "]", shapes,
                                                                        out));
            }
        }

        private void reachElement(Desc desc, Object element, String path, boolean shapes, List<Reached> out) {
            if (element == null) {
                return;
            }
            if (isCollection(desc)) {
                reachCollection(desc, element, path, shapes, out);
            } else if (desc instanceof Ref && shapes) {
                reachShape(element, path, out);
            }
        }

        // Reflection.

        private Object call(Object target, String accessor) {
            return invoke(method(target.getClass(), accessor), target);
        }

        private Method method(Class<?> type, String name, Class<?>... parameters) {
            return methods.computeIfAbsent(type.getName() + "#" + name + Arrays.toString(parameters), key -> {
                try {
                    return type.getMethod(name, parameters);
                } catch (NoSuchMethodException e) {
                    throw new AssertionError(e);
                }
            });
        }

        private static Object invoke(Method method, Object target, Object... arguments) {
            try {
                return method.invoke(target, arguments);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new AssertionError(method + " failed", e.getCause());
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        private static void deleteRecursively(Path root) {
            try (Stream<Path> walk = Files.walk(root)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            } catch (IOException e) {
                // Best effort: the directory is under the system temporary directory.
            }
        }
    }
}
