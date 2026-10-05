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

package software.amazon.awssdk.dynamic.client;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SparseTrait;

/**
 * Random input documents of a loaded {@link RandomInputModel}: a conforming document built by traversing the Smithy
 * model's input shape, and copies of it with random schema mutations applied. Documents are built as mutable trees
 * ({@code LinkedHashMap} for structures, unions and maps, {@code ArrayList} for lists, {@link Document} leaves, and
 * {@code null}) so that mutations can edit them in place, then converted with {@link #toDocument(Object)}.
 *
 * <p>Whether a document (mutated or not) conforms is never decided here; the reference validator decides that. The
 * generator only aims for conforming documents before mutation and plausible violations after it.
 */
final class RandomInputDocuments {
    /** The schema mutations a document may receive. */
    enum Mutation {
        /** A value replaced by a value of another document type. */
        TYPE_SWAP,
        /** An integral, bigDecimal or timestamp value replaced by an out-of-range, non-integral or non-finite one. */
        BAD_NUMBER,
        /** A present (preferably {@code @required}) structure member removed. */
        MISSING_REQUIRED,
        /** A member that the structure or union does not have added. */
        UNKNOWN_MEMBER,
        /** A union given zero or two members. */
        UNION_ARITY,
        /** A null element or value added to a list or map (a violation only if it is dense). */
        NULL_IN_DENSE,
        /** A structure or union replaced by a document discriminated as another structure. */
        WRONG_DISCRIMINATOR,
        /** A timestamp replaced by a string that is not an RFC 3339 date-time. */
        BAD_TIMESTAMP,
        /** The recursive tree nested from just under to just over the maximum depth. */
        OVER_DEPTH
    }

    /** A generated document with the mutations it received. */
    record Case(Document document, List<Mutation> mutations) {
    }

    private static final Set<ShapeType> BAD_NUMBER_TARGETS = EnumSet.of(
        ShapeType.BYTE, ShapeType.SHORT, ShapeType.INTEGER, ShapeType.INT_ENUM, ShapeType.LONG, ShapeType.BIG_INTEGER,
        ShapeType.BIG_DECIMAL, ShapeType.TIMESTAMP);
    private static final List<String> STRINGS = List.of("", "a", "Seattle", "héllo wörld", "quote\"and\\slash",
                                                        "\u2603\uD83D\uDE00", "RED", "GREEN", "PURPLE", "abc",
                                                        "TOOLONG", "123");
    /** Map keys, including ones whose path needs escaping. */
    private static final List<String> KEYS = List.of("k", "owner", "", "with space", "q\"uote", "back\\slash", "]",
                                                     "ünï", "k2", "[0]");
    private static final List<String> BAD_DATE_TIMES = List.of(
        "yesterday", "", "2024-01-02", "2024-01-02 03:04:05Z", "1700000000", "2024-01-02T03:04:05",
        "2024-1-2T3:4:5Z", "2024-01-02T03:04Z", "T03:04:05Z", "2024-01-02T03:04:05.1234567890Z",
        "2024-01-02T03:04:05+0200");

    private final Model model;
    private final StructureShape input;
    private final Random random;
    private final Document foreign;

    /**
     * @param foreign a structure document discriminated as a structure no input position targets
     */
    RandomInputDocuments(Model model, StructureShape input, Random random, Document foreign) {
        this.model = model;
        this.input = input;
        this.random = random;
        this.foreign = foreign;
    }

    /** A conforming document followed by {@code mutated} documents with 1 to 3 random mutations each. */
    List<Case> cases(int mutated) {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case(toDocument(value(input, 0, 0)), List.of()));
        for (int i = 0; i < mutated; i++) {
            Object[] root = {value(input, 0, 0)};
            List<Mutation> applied = new ArrayList<>();
            int count = 1 + random.nextInt(3);
            for (int m = 0; m < count; m++) {
                Mutation mutation = Mutation.values()[random.nextInt(Mutation.values().length)];
                applied.add(apply(mutation, root));
            }
            cases.add(new Case(toDocument(root[0]), applied));
        }
        return cases;
    }

    /** The document of a mutable tree. */
    @SuppressWarnings("unchecked")
    static Document toDocument(Object node) {
        if (node == null || node instanceof Document) {
            return (Document) node;
        }
        if (node instanceof Map<?, ?> map) {
            Map<String, Document> members = new LinkedHashMap<>();
            ((Map<String, Object>) map).forEach((name, value) -> members.put(name, toDocument(value)));
            return Document.of(members);
        }
        List<Document> elements = new ArrayList<>();
        for (Object element : (List<Object>) node) {
            elements.add(toDocument(element));
        }
        return Document.of(elements);
    }

    // Conforming values, by traversal of the Smithy shapes.

    private Object value(Shape target, int depth, int trees) {
        return switch (target.getType()) {
            case STRUCTURE -> structure(target.asStructureShape().orElseThrow(), depth, trees);
            case UNION -> {
                List<MemberShape> members = new ArrayList<>(target.members());
                MemberShape member = members.get(random.nextInt(members.size()));
                Map<String, Object> union = new LinkedHashMap<>();
                union.put(member.getMemberName(), value(target(member), depth + 1, trees));
                yield union;
            }
            case LIST, SET -> {
                CollectionShape list = (CollectionShape) target;
                boolean sparse = list.hasTrait(SparseTrait.class);
                List<Object> elements = new ArrayList<>();
                int n = depth >= 5 ? 0 : random.nextInt(4);
                for (int i = 0; i < n; i++) {
                    elements.add(sparse && random.nextInt(4) == 0 ? null
                                                                 : value(target(list.getMember()), depth + 1, trees));
                }
                yield elements;
            }
            case MAP -> {
                MapShape map = (MapShape) target;
                boolean sparse = map.hasTrait(SparseTrait.class);
                Map<String, Object> entries = new LinkedHashMap<>();
                int n = depth >= 5 ? 0 : random.nextInt(4);
                for (int i = 0; i < n; i++) {
                    entries.put(pick(KEYS), sparse && random.nextInt(4) == 0 ? null
                                                                            : value(target(map.getValue()), depth + 1,
                                                                                    trees));
                }
                yield entries;
            }
            default -> scalar(target);
        };
    }

    private Map<String, Object> structure(StructureShape structure, int depth, int trees) {
        boolean tree = structure.getId().equals(RandomInputModel.TREE);
        int nested = tree ? trees + 1 : trees;
        Map<String, Object> members = new LinkedHashMap<>();
        for (MemberShape member : structure.getAllMembers().values()) {
            boolean required = member.hasTrait(RequiredTrait.class);
            boolean exempt = member.hasTrait(DefaultTrait.class) || member.hasTrait(IdempotencyTokenTrait.class)
                             || member.hasTrait(ClientOptionalTrait.class) || structure.hasTrait(InputTrait.class);
            boolean recursive = member.getTarget().equals(RandomInputModel.TREE)
                                || member.getTarget().equals(RandomInputModel.TREE_LIST);
            boolean include;
            if (required && !exempt) {
                include = true;
            } else if (recursive && nested >= 3) {
                include = false;
            } else {
                include = random.nextDouble() < (depth < 3 ? 0.6 : 0.25);
            }
            if (include) {
                members.put(member.getMemberName(), value(target(member), depth + 1, nested));
            } else if (!required && random.nextInt(12) == 0) {
                // An explicit null member value counts as absent.
                members.put(member.getMemberName(), null);
            }
        }
        return members;
    }

    private Document scalar(Shape target) {
        String name = target.getId().getName();
        return switch (target.getType()) {
            case BYTE -> integral(bounded(Byte.MIN_VALUE, Byte.MAX_VALUE));
            case SHORT -> integral(bounded(Short.MIN_VALUE, Short.MAX_VALUE));
            case INTEGER -> integral("Ranged".equals(name) && random.nextBoolean()
                                     ? BigInteger.valueOf(random.nextInt(-5, 16))
                                     : bounded(Integer.MIN_VALUE, Integer.MAX_VALUE));
            case INT_ENUM -> integral(random.nextBoolean() ? BigInteger.valueOf(random.nextBoolean() ? 1 : 10)
                                                           : bounded(Integer.MIN_VALUE, Integer.MAX_VALUE));
            case LONG -> integral(bounded(Long.MIN_VALUE, Long.MAX_VALUE));
            case BIG_INTEGER -> integral(random.nextBoolean() ? bounded(Long.MIN_VALUE, Long.MAX_VALUE)
                                                              : new BigInteger(100, random).subtract(
                                                                  BigInteger.ONE.shiftLeft(99)));
            case FLOAT, DOUBLE -> switch (random.nextInt(6)) {
                case 0 -> Document.of(random.nextFloat() * 100 - 50);
                case 1 -> Document.of(random.nextInt(-1000, 1000));
                case 2 -> Document.of(Double.NaN);
                case 3 -> Document.of(new BigDecimal("12.5"));
                default -> Document.of(random.nextDouble() * 1e6 - 5e5);
            };
            case BIG_DECIMAL -> switch (random.nextInt(4)) {
                case 0 -> Document.of(new BigDecimal(new BigInteger(80, random), random.nextInt(-5, 20)));
                case 1 -> Document.of(random.nextDouble() * 1e3);
                case 2 -> Document.of(random.nextLong());
                default -> Document.of(new BigDecimal("-0.000000000000000000000001"));
            };
            case BOOLEAN -> Document.of(random.nextBoolean());
            case STRING, ENUM -> Document.of(pick(STRINGS));
            case BLOB -> {
                byte[] bytes = new byte[random.nextInt(8)];
                random.nextBytes(bytes);
                yield Document.of(bytes);
            }
            case TIMESTAMP -> timestamp();
            case DOCUMENT -> anyDocument(2);
            default -> throw new IllegalStateException("No scalar generator for " + target);
        };
    }

    /** A value of [min, max], at a boundary or zero 30% of the time. */
    private BigInteger bounded(long min, long max) {
        return switch (random.nextInt(10)) {
            case 0 -> BigInteger.valueOf(min);
            case 1 -> BigInteger.valueOf(max);
            case 2 -> BigInteger.ZERO;
            default -> BigInteger.valueOf(max == Long.MAX_VALUE ? random.nextLong() : random.nextLong(min, max + 1));
        };
    }

    /** {@code value} as a random numeric document type able to hold it exactly. */
    private Document integral(BigInteger value) {
        List<Document> forms = new ArrayList<>();
        forms.add(Document.of(value));
        forms.add(Document.of(new BigDecimal(value).setScale(random.nextInt(3))));
        if (value.bitLength() < 8) {
            forms.add(Document.of(value.byteValue()));
        }
        if (value.bitLength() < 16) {
            forms.add(Document.of(value.shortValue()));
        }
        if (value.bitLength() < 32) {
            forms.add(Document.of(value.intValue()));
            forms.add(Document.of(value.intValue()));
        }
        if (value.bitLength() < 64) {
            forms.add(Document.of(value.longValue()));
        }
        if (value.bitLength() <= 53) {
            forms.add(Document.of(value.doubleValue()));
        }
        if (value.bitLength() <= 24) {
            forms.add(Document.of(value.floatValue()));
        }
        return forms.get(random.nextInt(forms.size()));
    }

    private Document timestamp() {
        long seconds = random.nextLong(0, 4_102_444_800L);
        return switch (random.nextInt(7)) {
            case 0 -> Document.of(Instant.ofEpochSecond(seconds, random.nextInt(1_000_000_000)));
            case 1 -> Document.of(seconds);
            case 2 -> Document.of((int) (seconds % Integer.MAX_VALUE));
            case 3 -> Document.of(seconds + 0.5);
            case 4 -> Document.of(new BigDecimal(seconds).add(new BigDecimal("0.125")));
            default -> Document.of(dateTime(true));
        };
    }

    /**
     * A random date-time string: an RFC 3339 one (with fraction digits and an offset, as the documented parser
     * accepts) when {@code valid}, otherwise one with an out-of-range calendar or clock field.
     */
    private String dateTime(boolean valid) {
        int year = random.nextInt(1970, 2100);
        int month = random.nextInt(1, 13);
        int day = random.nextInt(1, 29);
        int hour = random.nextInt(24);
        int minute = random.nextInt(60);
        int second = random.nextInt(60);
        if (!valid) {
            switch (random.nextInt(6)) {
                case 0 -> month = random.nextBoolean() ? 0 : 13;
                case 1 -> day = random.nextBoolean() ? 0 : 32;
                case 2 -> {
                    month = 2;
                    day = 30;
                }
                case 3 -> hour = 25;
                case 4 -> minute = 61;
                default -> second = 61;
            }
        } else if (random.nextInt(4) == 0) {
            month = 2;
            day = 29;
            year = random.nextBoolean() ? 2024 : 2000;
        }
        StringBuilder text = new StringBuilder(String.format("%04d-%02d-%02dT%02d:%02d:%02d", year, month, day, hour,
                                                             minute, second));
        if (random.nextBoolean()) {
            text.append('.');
            int digits = 1 + random.nextInt(9);
            for (int i = 0; i < digits; i++) {
                text.append(random.nextInt(10));
            }
        }
        if (random.nextInt(3) == 0) {
            text.append(String.format("%s%02d:%02d", random.nextBoolean() ? "+" : "-", random.nextInt(15),
                                      random.nextBoolean() ? 0 : 30));
        } else {
            text.append('Z');
        }
        return text.toString();
    }

    private Document anyDocument(int depth) {
        int kind = random.nextInt(depth > 0 ? 6 : 4);
        return switch (kind) {
            case 0 -> Document.of(pick(STRINGS));
            case 1 -> Document.of(random.nextInt());
            case 2 -> Document.of(random.nextBoolean());
            case 3 -> Document.of(random.nextDouble());
            case 4 -> Document.of(List.of(anyDocument(depth - 1), anyDocument(depth - 1)));
            default -> {
                Map<String, Document> members = new LinkedHashMap<>();
                members.put("x", anyDocument(depth - 1));
                members.put("y", anyDocument(depth - 1));
                yield Document.of(members);
            }
        };
    }

    // Mutations.

    /** A position of the tree: its shape, its value and how to replace it. */
    private record Site(Shape target, int depth, Object node, Consumer<Object> set) {
    }

    /** Applies {@code mutation} to {@code root[0]}, falling back to a type swap where it has no applicable site. */
    @SuppressWarnings("unchecked")
    private Mutation apply(Mutation mutation, Object[] root) {
        List<Site> sites = new ArrayList<>();
        collect(input, root[0], 0, value -> root[0] = value, sites);
        Map<String, Object> top = root[0] instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
        switch (mutation) {
            case BAD_NUMBER -> {
                Site site = pickSite(sites, s -> BAD_NUMBER_TARGETS.contains(s.target().getType()));
                if (site != null) {
                    site.set().accept(badNumber(site.target().getType()));
                    return mutation;
                }
                if (top != null) {
                    top.put("fInteger", badNumber(ShapeType.INTEGER));
                    return mutation;
                }
            }
            case MISSING_REQUIRED -> {
                Site site = pickSite(sites, s -> s.target().isStructureShape() && s.node() instanceof Map<?, ?> m
                                                 && !m.isEmpty());
                if (site != null) {
                    Map<String, Object> members = (Map<String, Object>) site.node();
                    List<String> required = new ArrayList<>();
                    for (MemberShape member : site.target().members()) {
                        if (member.hasTrait(RequiredTrait.class) && members.containsKey(member.getMemberName())) {
                            required.add(member.getMemberName());
                        }
                    }
                    List<String> names = required.isEmpty() ? new ArrayList<>(members.keySet()) : required;
                    members.remove(pick(names));
                    return mutation;
                }
            }
            case UNKNOWN_MEMBER -> {
                Site site = pickSite(sites, s -> (s.target().isStructureShape() || s.target().isUnionShape())
                                                 && s.node() instanceof Map);
                if (site != null) {
                    Map<String, Object> members = (Map<String, Object>) site.node();
                    List<MemberShape> known = new ArrayList<>(site.target().members());
                    String name = random.nextBoolean() || known.isEmpty()
                                  ? "zz" + random.nextInt(100)
                                  : capitalize(known.get(random.nextInt(known.size())).getMemberName());
                    members.put(name, random.nextBoolean() ? Document.of(1) : null);
                    return mutation;
                }
            }
            case UNION_ARITY -> {
                Site site = pickSite(sites, s -> s.target().isUnionShape() && s.node() instanceof Map);
                if (site != null) {
                    Map<String, Object> members = (Map<String, Object>) site.node();
                    List<MemberShape> unset = new ArrayList<>();
                    for (MemberShape member : site.target().members()) {
                        if (members.get(member.getMemberName()) == null) {
                            unset.add(member);
                        }
                    }
                    if (unset.isEmpty() || random.nextBoolean()) {
                        members.clear();
                    } else {
                        MemberShape member = unset.get(random.nextInt(unset.size()));
                        members.put(member.getMemberName(), value(target(member), site.depth() + 1, 2));
                    }
                    return mutation;
                }
            }
            case NULL_IN_DENSE -> {
                Site site = pickSite(sites, s -> (s.target() instanceof CollectionShape && s.node() instanceof List)
                                                 || (s.target().isMapShape() && s.node() instanceof Map));
                if (site != null) {
                    if (site.node() instanceof List<?> list) {
                        ((List<Object>) list).add(random.nextInt(list.size() + 1), null);
                    } else {
                        ((Map<String, Object>) site.node()).put(pick(KEYS), null);
                    }
                    return mutation;
                }
            }
            case WRONG_DISCRIMINATOR -> {
                Site site = pickSite(sites, s -> s.target().isStructureShape() || s.target().isUnionShape());
                site.set().accept(foreign);
                return mutation;
            }
            case BAD_TIMESTAMP -> {
                Document bad = Document.of(random.nextBoolean() ? pick(BAD_DATE_TIMES) : dateTime(false));
                Site site = pickSite(sites, s -> s.target().getType() == ShapeType.TIMESTAMP);
                if (site != null) {
                    site.set().accept(bad);
                    return mutation;
                }
                if (top != null) {
                    top.put("fTimestamp", bad);
                    return mutation;
                }
            }
            case OVER_DEPTH -> {
                if (top != null) {
                    top.put("tree", chain(DynamicInputValidator.MAX_DEPTH - 4 + random.nextInt(9)));
                    return mutation;
                }
            }
            default -> {
            }
        }
        Site site = sites.get(random.nextInt(sites.size()));
        site.set().accept(swap());
        return Mutation.TYPE_SWAP;
    }

    @SuppressWarnings("unchecked")
    private void collect(Shape target, Object node, int depth, Consumer<Object> set, List<Site> out) {
        out.add(new Site(target, depth, node, set));
        if ((target.isStructureShape() || target.isUnionShape()) && node instanceof Map<?, ?> map) {
            Map<String, Object> members = (Map<String, Object>) map;
            for (MemberShape member : target.members()) {
                String name = member.getMemberName();
                Object value = members.get(name);
                if (value != null) {
                    collect(target(member), value, depth + 1, v -> members.put(name, v), out);
                }
            }
        } else if (target instanceof CollectionShape list && node instanceof List<?> elements) {
            List<Object> values = (List<Object>) elements;
            for (int i = 0; i < values.size(); i++) {
                int index = i;
                if (values.get(i) != null) {
                    collect(target(list.getMember()), values.get(i), depth + 1, v -> values.set(index, v), out);
                }
            }
        } else if (target instanceof MapShape map && node instanceof Map<?, ?> entries) {
            Map<String, Object> values = (Map<String, Object>) entries;
            for (Map.Entry<String, Object> entry : List.copyOf(values.entrySet())) {
                if (entry.getValue() != null) {
                    collect(target(map.getValue()), entry.getValue(), depth + 1, v -> values.put(entry.getKey(), v),
                            out);
                }
            }
        }
    }

    /**
     * A tree of {@code levels} nested {@code Tree} structures, each through {@code child} or a one-element
     * {@code children} list, so its deepest value sits just under or just over the maximum depth.
     */
    private Object chain(int levels) {
        Map<String, Object> top = new LinkedHashMap<>();
        Map<String, Object> current = top;
        int depth = 1;
        while (depth < levels) {
            current.put("value", Document.of(depth));
            Map<String, Object> next = new LinkedHashMap<>();
            if (random.nextInt(4) == 0) {
                List<Object> children = new ArrayList<>();
                children.add(next);
                current.put("children", children);
                depth += 2;
            } else {
                current.put("child", next);
                depth++;
            }
            current = next;
        }
        current.put("value", Document.of(depth));
        return top;
    }

    private Document badNumber(ShapeType type) {
        return switch (type) {
            case BYTE -> pick(List.of(Document.of(128), Document.of(-129), Document.of(1.5), Document.of(300L),
                                      Document.of(Byte.MAX_VALUE)));
            case SHORT -> pick(List.of(Document.of(32_768), Document.of(-32_769L), Document.of(0.25f),
                                       Document.of(Short.MIN_VALUE)));
            case INTEGER, INT_ENUM -> pick(List.of(Document.of(2_147_483_648L), Document.of(-2_147_483_649L),
                                                   Document.of(1e10), Document.of(2.5),
                                                   Document.of(new BigDecimal("7.000001")),
                                                   Document.of(Integer.MAX_VALUE), Document.of(Float.NaN)));
            case LONG -> pick(List.of(Document.of(BigInteger.ONE.shiftLeft(63)), Document.of(1e19),
                                      Document.of(new BigDecimal("1.25")), Document.of(-9.3e18),
                                      Document.of(Long.MIN_VALUE), Document.of(Double.POSITIVE_INFINITY)));
            case BIG_INTEGER -> pick(List.of(Document.of(0.5), Document.of(new BigDecimal("10.01")),
                                             Document.of(Double.NaN), Document.of(1e300)));
            case BIG_DECIMAL -> pick(List.of(Document.of(Double.NaN), Document.of(Float.POSITIVE_INFINITY),
                                             Document.of(-1e-300)));
            default -> pick(List.of(Document.of(Double.NaN), Document.of(Double.NEGATIVE_INFINITY),
                                    Document.of(Float.NaN), Document.of(-1.5)));
        };
    }

    private Document swap() {
        return switch (random.nextInt(9)) {
            case 0 -> Document.of("swapped");
            case 1 -> Document.of(true);
            case 2 -> Document.of(7);
            case 3 -> Document.of(3.25);
            case 4 -> Document.of(List.of(Document.of("x")));
            case 5 -> Document.of(Map.of("x", Document.of(1)));
            case 6 -> Document.of(new byte[] {1, 2});
            case 7 -> Document.of(Instant.EPOCH);
            default -> Document.of(BigInteger.TEN);
        };
    }

    private Site pickSite(List<Site> sites, Predicate<Site> filter) {
        List<Site> matching = sites.stream().filter(filter).toList();
        return matching.isEmpty() ? null : matching.get(random.nextInt(matching.size()));
    }

    private <T> T pick(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    private Shape target(MemberShape member) {
        return model.expectShape(member.getTarget());
    }

    private static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
