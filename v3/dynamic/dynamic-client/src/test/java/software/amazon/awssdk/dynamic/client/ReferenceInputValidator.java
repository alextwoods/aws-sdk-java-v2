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
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.IdempotencyTokenTrait;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * The oracle of {@link DynamicInputValidationPropertyTest}: an independent statement of which elements of a finite
 * input document do not conform to an operation's input, derived by traversing the loaded Smithy model's shapes (never
 * the runtime {@code Schema} the implementation reads) and Smithy's own {@link NullableIndex}.
 *
 * <p>It follows Smithy semantics and the documented rules of {@code DynamicInputValidator}:
 * <ul>
 *   <li>A structure value is a map or structure document; a union value a map, structure or union document. A
 *       discriminator, if the document has one, must be the shape's ID (otherwise only the shape is reported). Every
 *       name that is not a member is reported as {@code path.name}.</li>
 *   <li>A structure member must be supplied when Smithy's client-mode nullability makes it non-null (so not for
 *       {@code @clientOptional} members or members of {@code @input} structures), unless a {@code @default} or the
 *       pipeline ({@code @idempotencyToken}) fills it. A {@code null} member value is absent.</li>
 *   <li>A union has exactly one non-null member; only set members are checked.</li>
 *   <li>A list is a list document and a map a map document; a {@code null} element or value is reported unless
 *       Smithy's nullability index makes the list member or map value nullable ({@code @sparse}).</li>
 *   <li>Integral shapes (byte, short, integer, intEnum, long, bigInteger) take a finite, integral numeric document
 *       inside the type's range (bigInteger is unbounded); float and double take any numeric document; bigDecimal any
 *       finite one. Range, length and pattern constraint traits are deliberately not checked: like generated clients,
 *       the dynamic client leaves them to the service.</li>
 *   <li>A timestamp is a timestamp document, a finite numeric document of epoch seconds, or a date-time string:
 *       {@code YYYY-MM-DDThh:mm:ss[.f{1,9}](Z|+hh:mm|-hh:mm)} with a valid calendar date and clock time, the RFC 3339
 *       {@code date-time} forms Smithy Java's {@code DATE_TIME} parser accepts.</li>
 *   <li>Strings and enums take string (or enum) documents, as enums are open; booleans boolean documents; blobs blob
 *       documents; a document member any document.</li>
 *   <li>A value nested deeper than {@value #MAX_DEPTH} levels below the input is reported, without checking it.</li>
 * </ul>
 * Paths are {@code input}, then {@code .member}, {@code [index]} and {@code ["key"]} (with {@code \} and {@code "}
 * escaped), as documented on {@link DynamicInvocationException.Violation}.
 */
final class ReferenceInputValidator {
    /** The documented maximum nesting depth. */
    static final int MAX_DEPTH = 100;

    private static final Set<ShapeType> NUMERIC_DOCUMENTS = Set.of(
        ShapeType.BYTE, ShapeType.SHORT, ShapeType.INTEGER, ShapeType.INT_ENUM, ShapeType.LONG, ShapeType.FLOAT,
        ShapeType.DOUBLE, ShapeType.BIG_INTEGER, ShapeType.BIG_DECIMAL);
    private static final Pattern DATE_TIME = Pattern.compile(
        "(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})(\\.\\d{1,9})?(Z|[+-](\\d{2}):(\\d{2}))");

    private final Model model;
    private final NullableIndex nullable;

    ReferenceInputValidator(Model model) {
        this.model = model;
        this.nullable = NullableIndex.of(model);
    }

    /** The sorted paths of every element of {@code document} that does not conform to {@code input}. */
    List<String> violations(StructureShape input, Document document) {
        List<String> out = new ArrayList<>();
        value(input, document, "input", 0, out);
        out.sort(null);
        return out;
    }

    private void value(Shape target, Document document, String path, int depth, List<String> out) {
        if (depth > MAX_DEPTH) {
            out.add(path);
            return;
        }
        if (depth > 0 && target.hasTrait(StreamingTrait.class)) {
            out.add(path);
            return;
        }
        ShapeType found = document.type();
        switch (target.getType()) {
            case STRUCTURE -> structure((StructureShape) target, document, path, depth, out);
            case UNION -> union((UnionShape) target, document, path, depth, out);
            case LIST, SET -> {
                if (found != ShapeType.LIST && found != ShapeType.SET) {
                    out.add(path);
                    return;
                }
                MemberShape member = ((CollectionShape) target).getMember();
                List<Document> elements = document.asList();
                for (int i = 0; i < elements.size(); i++) {
                    element(member, elements.get(i), path + "[" + i + "]", depth, out);
                }
            }
            case MAP -> {
                if (found != ShapeType.MAP) {
                    out.add(path);
                    return;
                }
                MemberShape member = ((MapShape) target).getValue();
                for (Map.Entry<String, Document> entry : document.asStringMap().entrySet()) {
                    String key = entry.getKey().replace("\\", "\\\\").replace("\"", "\\\"");
                    element(member, entry.getValue(), path + "[\"" + key + "\"]", depth, out);
                }
            }
            case BOOLEAN -> expect(found == ShapeType.BOOLEAN, path, out);
            case STRING, ENUM -> expect(found == ShapeType.STRING || found == ShapeType.ENUM, path, out);
            case BLOB -> expect(found == ShapeType.BLOB, path, out);
            case BYTE -> integral(document, Byte.MIN_VALUE, Byte.MAX_VALUE, path, out);
            case SHORT -> integral(document, Short.MIN_VALUE, Short.MAX_VALUE, path, out);
            case INTEGER, INT_ENUM -> integral(document, Integer.MIN_VALUE, Integer.MAX_VALUE, path, out);
            case LONG -> integral(document, Long.MIN_VALUE, Long.MAX_VALUE, path, out);
            case BIG_INTEGER -> {
                if (!NUMERIC_DOCUMENTS.contains(found)) {
                    out.add(path);
                    return;
                }
                BigDecimal exact = exact(document);
                expect(exact != null && isIntegral(exact), path, out);
            }
            case FLOAT, DOUBLE -> expect(NUMERIC_DOCUMENTS.contains(found), path, out);
            case BIG_DECIMAL -> expect(NUMERIC_DOCUMENTS.contains(found) && exact(document) != null, path, out);
            case TIMESTAMP -> {
                if (found == ShapeType.TIMESTAMP) {
                    return;
                }
                if (NUMERIC_DOCUMENTS.contains(found)) {
                    expect(exact(document) != null, path, out);
                } else {
                    expect(found == ShapeType.STRING && isDateTime(document.asString()), path, out);
                }
            }
            case DOCUMENT -> {
                // Any document is a document value.
            }
            default -> out.add(path);
        }
    }

    private void structure(StructureShape structure, Document document, String path, int depth, List<String> out) {
        Map<String, Document> members = aggregate(structure.getId(), document, path, out, ShapeType.STRUCTURE,
                                                  ShapeType.MAP);
        if (members == null) {
            return;
        }
        for (MemberShape member : structure.getAllMembers().values()) {
            String memberPath = path + "." + member.getMemberName();
            Document value = members.get(member.getMemberName());
            if (value != null) {
                value(target(member), value, memberPath, depth + 1, out);
            } else if (mustBeSupplied(member)) {
                out.add(memberPath);
            }
        }
    }

    private void union(UnionShape union, Document document, String path, int depth, List<String> out) {
        Map<String, Document> members = aggregate(union.getId(), document, path, out, ShapeType.UNION,
                                                  ShapeType.STRUCTURE, ShapeType.MAP);
        if (members == null) {
            return;
        }
        List<MemberShape> set = new ArrayList<>();
        for (MemberShape member : union.getAllMembers().values()) {
            if (members.get(member.getMemberName()) != null) {
                set.add(member);
            }
        }
        if (set.size() != 1) {
            out.add(path);
        }
        for (MemberShape member : set) {
            value(target(member), members.get(member.getMemberName()), path + "." + member.getMemberName(),
                  depth + 1, out);
        }
    }

    /**
     * The members of a structure or union document, after reporting its unknown member names; {@code null} (with
     * the value reported) if it is of another type or discriminated as another shape.
     */
    private Map<String, Document> aggregate(ShapeId id, Document document, String path, List<String> out,
                                            ShapeType... accepted) {
        if (!List.of(accepted).contains(document.type())) {
            out.add(path);
            return null;
        }
        ShapeId discriminator = document.discriminator();
        if (discriminator != null && !discriminator.equals(id)) {
            out.add(path);
            return null;
        }
        Map<String, Document> members = document.asStringMap();
        Shape shape = model.expectShape(id);
        for (String name : members.keySet()) {
            if (shape.getMember(name).isEmpty()) {
                out.add(path + "." + name);
            }
        }
        return members;
    }

    private void element(MemberShape member, Document value, String path, int depth, List<String> out) {
        if (value != null) {
            value(target(member), value, path, depth + 1, out);
        } else if (!nullable.isMemberNullable(member)) {
            out.add(path);
        }
    }

    /**
     * Whether an absent {@code member} is invalid: Smithy's client-mode nullability makes it non-null, and neither a
     * default nor the pipeline's idempotency token fills it.
     */
    private boolean mustBeSupplied(MemberShape member) {
        return !nullable.isMemberNullable(member, NullableIndex.CheckMode.CLIENT)
               && !member.hasTrait(DefaultTrait.class) && !member.hasTrait(IdempotencyTokenTrait.class);
    }

    private static void integral(Document document, long min, long max, String path, List<String> out) {
        if (!NUMERIC_DOCUMENTS.contains(document.type())) {
            out.add(path);
            return;
        }
        BigDecimal exact = exact(document);
        expect(exact != null && isIntegral(exact) && exact.compareTo(BigDecimal.valueOf(min)) >= 0
               && exact.compareTo(BigDecimal.valueOf(max)) <= 0, path, out);
    }

    /** The exact value of a numeric document, or {@code null} if it is NaN or infinite. */
    private static BigDecimal exact(Document document) {
        return switch (document.type()) {
            case BYTE -> BigDecimal.valueOf(document.asByte());
            case SHORT -> BigDecimal.valueOf(document.asShort());
            case INTEGER, INT_ENUM -> BigDecimal.valueOf(document.asInteger());
            case LONG -> BigDecimal.valueOf(document.asLong());
            case FLOAT -> Float.isFinite(document.asFloat()) ? new BigDecimal(document.asFloat()) : null;
            case DOUBLE -> Double.isFinite(document.asDouble()) ? new BigDecimal(document.asDouble()) : null;
            case BIG_INTEGER -> new BigDecimal(document.asBigInteger());
            case BIG_DECIMAL -> document.asBigDecimal();
            default -> throw new IllegalArgumentException("Not a numeric document: " + document.type());
        };
    }

    private static boolean isIntegral(BigDecimal value) {
        return value.signum() == 0 || value.stripTrailingZeros().scale() <= 0;
    }

    private static boolean isDateTime(String text) {
        Matcher m = DATE_TIME.matcher(text);
        if (!m.matches()) {
            return false;
        }
        try {
            LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
        } catch (DateTimeException e) {
            return false;
        }
        boolean clock = Integer.parseInt(m.group(4)) < 24 && Integer.parseInt(m.group(5)) < 60
                        && Integer.parseInt(m.group(6)) < 60;
        boolean offset = m.group(9) == null
                         || Integer.parseInt(m.group(9)) <= 18 && Integer.parseInt(m.group(10)) < 60;
        return clock && offset;
    }

    private static void expect(boolean conforms, String path, List<String> out) {
        if (!conforms) {
            out.add(path);
        }
    }

    private Shape target(MemberShape member) {
        return model.expectShape(member.getTarget());
    }
}
