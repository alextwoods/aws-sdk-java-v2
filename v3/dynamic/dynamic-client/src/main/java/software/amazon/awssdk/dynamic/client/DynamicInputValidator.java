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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.awssdk.dynamic.client.DynamicInvocationException.Violation;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.java.core.serde.TimestampFormatter;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.event.EventStreamWriter;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

/**
 * Checks a finite input {@link Document} against the exact input schema of one operation before anything of the call
 * runs (Requirements 14.11, 14.19), and reports every invalid element with its path.
 *
 * <p>A document conforms when, recursively:
 * <ul>
 *   <li>a structure is a map or structure document whose discriminator, if it has one, is exactly the structure's
 *       shape ID; every member it names is a member of the structure; and every {@code @required} member without a
 *       {@code @default} is present, unless it is client optional ({@code @clientOptional}, or a member of an
 *       {@code @input} structure, as Smithy defines), or an {@code @idempotencyToken} member the pipeline fills;</li>
 *   <li>a union is a map, structure or union document (discriminator as for structures) with exactly one member of
 *       the union set;</li>
 *   <li>a list is a list document and a map is a map document; a {@code null} element or value only appears in a
 *       {@code @sparse} one;</li>
 *   <li>a string or enum is a string document (enum values are open, as on the wire); a boolean is a boolean
 *       document; a blob is a blob document;</li>
 *   <li>a byte, short, integer, intEnum, long or bigInteger is a numeric document with an integral value inside the
 *       type's range; a float, double or bigDecimal is a numeric document (a bigDecimal is finite);</li>
 *   <li>a timestamp is a timestamp document, a finite number of epoch seconds, or an RFC 3339 {@code date-time}
 *       string, the representations Smithy Java's schema-guided documents accept;</li>
 *   <li>a document member accepts any document.</li>
 * </ul>
 * {@code null} structure member values count as absent. Length, range, pattern and other constraint traits are not
 * client-side checks of generated clients either, so they are left to the service. The operation's top-level
 * streaming member is never a finite value: a streaming blob member is absent (the call's request body carries it),
 * and an event stream member, if set, is an event stream document wrapping a Smithy Java {@code EventStream} writer,
 * the value a generated input holds for it. A streaming member anywhere else never conforms.
 *
 * <p>Built once per operation binding; immutable and thread safe.
 */
final class DynamicInputValidator {
    /** The deepest nesting a document may have; a deeper (for example self-referencing) document is rejected. */
    static final int MAX_DEPTH = 100;

    private static final Set<ShapeType> NUMBERS = EnumSet.of(ShapeType.BYTE, ShapeType.SHORT, ShapeType.INTEGER,
                                                             ShapeType.INT_ENUM, ShapeType.LONG, ShapeType.FLOAT,
                                                             ShapeType.DOUBLE, ShapeType.BIG_INTEGER,
                                                             ShapeType.BIG_DECIMAL);
    private static final ShapeId INPUT_TRAIT = ShapeId.from("smithy.api#input");
    private static final ShapeId CLIENT_OPTIONAL_TRAIT = ShapeId.from("smithy.api#clientOptional");

    private final Schema input;
    private final String root;
    private final Set<ShapeId> clientOptional;

    private DynamicInputValidator(Schema input, String root, Set<ShapeId> clientOptional) {
        this.input = input;
        this.root = root;
        this.clientOptional = Set.copyOf(clientOptional);
    }

    /**
     * The validator of the input schema {@code input}, whose client-optional members are read from the codegen view
     * {@code model} the schema was built from; violation paths start at {@code input}.
     */
    static DynamicInputValidator of(Model model, Schema input) {
        return of(model, input, "input");
    }

    /**
     * The validator of the structure or union schema {@code input} (an operation input, or an input event stream
     * union), whose client-optional members are read from the codegen view {@code model}; violation paths start at
     * {@code root}.
     */
    static DynamicInputValidator of(Model model, Schema input, String root) {
        Set<ShapeId> clientOptional = new HashSet<>();
        model.getShape(input.id()).ifPresent(start -> {
            for (Shape shape : new Walker(model).walkShapes(start)) {
                if (!shape.isStructureShape()) {
                    continue;
                }
                boolean inputStructure = shape.hasTrait(INPUT_TRAIT);
                for (MemberShape member : shape.members()) {
                    if (inputStructure || member.hasTrait(CLIENT_OPTIONAL_TRAIT)) {
                        clientOptional.add(member.getId());
                    }
                }
            }
        });
        return new DynamicInputValidator(input, root, clientOptional);
    }

    /** The input schema. */
    Schema input() {
        return input;
    }

    /** Every violation of {@code document}, in traversal order; empty if it conforms. */
    List<Violation> validate(Document document) {
        List<Violation> violations = new ArrayList<>();
        value(input, document, root, 0, violations);
        return violations;
    }

    private void value(Schema schema, Document document, String path, int depth, List<Violation> out) {
        if (depth > MAX_DEPTH) {
            out.add(new Violation(path, "the document nests deeper than " + MAX_DEPTH + " levels"));
            return;
        }
        ShapeType type = schema.type();
        // The root is the validated shape itself (an input event stream union is @streaming); below it, a streaming
        // member is never a finite value.
        if (depth > 0 && schema.hasTrait(TraitKey.STREAMING_TRAIT)) {
            out.add(new Violation(path, "is a streaming member, which a finite document cannot carry"));
            return;
        }
        switch (type) {
            case STRUCTURE -> structure(schema, document, path, depth, out);
            case UNION -> union(schema, document, path, depth, out);
            case LIST, SET -> list(schema, document, path, depth, out);
            case MAP -> map(schema, document, path, depth, out);
            case BOOLEAN -> expect(schema, document, path, out, ShapeType.BOOLEAN);
            case STRING, ENUM -> expect(schema, document, path, out, ShapeType.STRING, ShapeType.ENUM);
            case BLOB -> expect(schema, document, path, out, ShapeType.BLOB);
            case BYTE, SHORT, INTEGER, INT_ENUM, LONG, BIG_INTEGER, FLOAT, DOUBLE, BIG_DECIMAL ->
                number(schema, document, path, out);
            case TIMESTAMP -> timestamp(schema, document, path, out);
            case DOCUMENT -> {
                // Any document is a document value.
            }
            default -> out.add(new Violation(path, "targets " + type + " " + targetId(schema)
                                                   + ", which has no finite document form"));
        }
    }

    private void structure(Schema schema, Document document, String path, int depth, List<Violation> out) {
        if (!aggregate(schema, document, path, out, ShapeType.STRUCTURE, ShapeType.MAP)) {
            return;
        }
        Map<String, Document> members = document.asStringMap();
        unknownMembers(schema, members, path, out);
        for (Schema member : schema.members()) {
            Document value = members.get(member.memberName());
            String memberPath = path + "." + member.memberName();
            if (depth == 0 && member.hasTrait(TraitKey.STREAMING_TRAIT)) {
                // The operation's one streaming member (Smithy allows streams only as top-level input members).
                streamMember(member, value, memberPath, out);
                continue;
            }
            if (value == null) {
                if (enforcedRequired(member)) {
                    out.add(new Violation(memberPath, "is required by " + targetId(schema) + " and is missing"));
                }
            } else {
                value(member, value, memberPath, depth + 1, out);
            }
        }
    }

    /**
     * The top-level streaming member: a streaming blob is never in the document (the call's request body carries
     * it), and an event stream is an event stream document wrapping the writer of the input events.
     */
    private void streamMember(Schema member, Document value, String path, List<Violation> out) {
        if (member.type() == ShapeType.BLOB) {
            if (value != null) {
                out.add(new Violation(path, "is the streaming blob member " + targetId(member) + ", which the "
                                            + "call's request body carries, not the document"));
            }
            return;
        }
        if (value == null) {
            if (enforcedRequired(member)) {
                out.add(new Violation(path, "is the required event stream " + targetId(member) + " and is missing"));
            }
            return;
        }
        EventStream<?> events;
        try {
            events = value.asEventStream();
        } catch (RuntimeException e) {
            events = null;
        }
        if (events == null) {
            out.add(mismatch(member, value, path, "an event stream document (Document.ofObject of an "
                                                  + "EventStream writer)"));
        } else if (!(events instanceof EventStreamWriter<?>)) {
            out.add(new Violation(path, "is an event stream reader; the input event stream " + targetId(member)
                                        + " is a writer of the events to send"));
        }
    }

    private void union(Schema schema, Document document, String path, int depth, List<Violation> out) {
        if (!aggregate(schema, document, path, out, ShapeType.UNION, ShapeType.STRUCTURE, ShapeType.MAP)) {
            return;
        }
        Map<String, Document> members = document.asStringMap();
        unknownMembers(schema, members, path, out);
        List<Schema> set = new ArrayList<>();
        for (Schema member : schema.members()) {
            if (members.get(member.memberName()) != null) {
                set.add(member);
            }
        }
        if (set.size() != 1) {
            out.add(new Violation(path, "a union " + targetId(schema) + " has exactly one member set, not "
                                        + set.size() + (set.isEmpty() ? "" : " " + set.stream()
                                                                                    .map(Schema::memberName)
                                                                                    .toList())));
        }
        for (Schema member : set) {
            value(member, members.get(member.memberName()), path + "." + member.memberName(), depth + 1, out);
        }
    }

    private void list(Schema schema, Document document, String path, int depth, List<Violation> out) {
        if (!expect(schema, document, path, out, ShapeType.LIST, ShapeType.SET)) {
            return;
        }
        boolean sparse = sparse(schema);
        Schema element = schema.listMember();
        List<Document> elements = document.asList();
        for (int i = 0; i < elements.size(); i++) {
            Document value = elements.get(i);
            String elementPath = path + "[" + i + "]";
            if (value == null) {
                if (!sparse) {
                    out.add(new Violation(elementPath, "is null in the dense list " + targetId(schema)));
                }
            } else {
                value(element, value, elementPath, depth + 1, out);
            }
        }
    }

    private void map(Schema schema, Document document, String path, int depth, List<Violation> out) {
        if (!expect(schema, document, path, out, ShapeType.MAP)) {
            return;
        }
        boolean sparse = sparse(schema);
        Schema valueMember = schema.mapValueMember();
        for (Map.Entry<String, Document> entry : document.asStringMap().entrySet()) {
            String entryPath = path + "[\"" + entry.getKey().replace("\\", "\\\\").replace("\"", "\\\"") + "\"]";
            if (entry.getValue() == null) {
                if (!sparse) {
                    out.add(new Violation(entryPath, "is null in the dense map " + targetId(schema)));
                }
            } else {
                value(valueMember, entry.getValue(), entryPath, depth + 1, out);
            }
        }
    }

    private void number(Schema schema, Document document, String path, List<Violation> out) {
        if (!expect(schema, document, path, out, NUMBERS.toArray(ShapeType[]::new))) {
            return;
        }
        BigDecimal value;
        try {
            value = decimal(document.type() == ShapeType.INT_ENUM ? document.asInteger() : document.asNumber());
        } catch (RuntimeException e) {
            out.add(new Violation(path, "is not a readable number: " + e.getMessage()));
            return;
        }
        ShapeType type = schema.type();
        if (value == null) {
            if (type != ShapeType.FLOAT && type != ShapeType.DOUBLE) {
                out.add(new Violation(path, "is not a finite number, which " + type + " " + targetId(schema)
                                            + " requires"));
            }
            return;
        }
        if (type == ShapeType.FLOAT || type == ShapeType.DOUBLE || type == ShapeType.BIG_DECIMAL) {
            return;
        }
        if (value.signum() != 0 && value.stripTrailingZeros().scale() > 0) {
            out.add(new Violation(path, "is " + value.toPlainString() + ", not an integral value, which " + type + " "
                                        + targetId(schema) + " requires"));
            return;
        }
        BigInteger integral = value.toBigIntegerExact();
        long min;
        long max;
        switch (type) {
            case BYTE -> {
                min = Byte.MIN_VALUE;
                max = Byte.MAX_VALUE;
            }
            case SHORT -> {
                min = Short.MIN_VALUE;
                max = Short.MAX_VALUE;
            }
            case INTEGER, INT_ENUM -> {
                min = Integer.MIN_VALUE;
                max = Integer.MAX_VALUE;
            }
            case LONG -> {
                min = Long.MIN_VALUE;
                max = Long.MAX_VALUE;
            }
            default -> {
                return;
            }
        }
        if (integral.compareTo(BigInteger.valueOf(min)) < 0 || integral.compareTo(BigInteger.valueOf(max)) > 0) {
            out.add(new Violation(path, "is " + integral + ", outside the range [" + min + ", " + max + "] of "
                                        + type + " " + targetId(schema)));
        }
    }

    private void timestamp(Schema schema, Document document, String path, List<Violation> out) {
        ShapeType found = document.type();
        if (found == ShapeType.TIMESTAMP) {
            return;
        }
        if (NUMBERS.contains(found)) {
            BigDecimal seconds;
            try {
                seconds = decimal(found == ShapeType.INT_ENUM ? document.asInteger() : document.asNumber());
            } catch (RuntimeException e) {
                seconds = null;
            }
            if (seconds == null) {
                out.add(new Violation(path, "is not a finite number of epoch seconds for timestamp "
                                            + targetId(schema)));
            }
            return;
        }
        if (found == ShapeType.STRING) {
            String text = document.asString();
            try {
                TimestampFormatter.Prelude.DATE_TIME.readFromString(text, false);
            } catch (RuntimeException e) {
                out.add(new Violation(path, "\"" + text + "\" is not an RFC 3339 date-time for timestamp "
                                            + targetId(schema)));
            }
            return;
        }
        out.add(mismatch(schema, document, path, "a timestamp, epoch-seconds number or date-time string"));
    }

    /** Reports every member name of {@code members} that is not a member of {@code schema}, sorted. */
    private static void unknownMembers(Schema schema, Map<String, Document> members, String path,
                                       List<Violation> out) {
        Set<String> unknown = new TreeSet<>();
        for (String name : members.keySet()) {
            if (schema.member(name) == null) {
                unknown.add(name);
            }
        }
        for (String name : unknown) {
            out.add(new Violation(path + "." + name, "is not a member of " + targetId(schema)));
        }
    }

    /** Checks the type and discriminator of a structure or union document. */
    private static boolean aggregate(Schema schema, Document document, String path, List<Violation> out,
                                     ShapeType... accepted) {
        if (!expect(schema, document, path, out, accepted)) {
            return false;
        }
        ShapeId discriminator;
        try {
            discriminator = document.discriminator();
        } catch (RuntimeException e) {
            out.add(new Violation(path, "has an unreadable discriminator: " + e.getMessage()));
            return false;
        }
        if (discriminator != null && !discriminator.equals(targetId(schema))) {
            out.add(new Violation(path, "is a document of " + discriminator + ", not of " + targetId(schema)));
            return false;
        }
        return true;
    }

    private static boolean expect(Schema schema, Document document, String path, List<Violation> out,
                                  ShapeType... accepted) {
        ShapeType found = document.type();
        for (ShapeType type : accepted) {
            if (type == found) {
                return true;
            }
        }
        out.add(mismatch(schema, document, path, withArticle(schema.type() + " value")));
        return false;
    }

    private static Violation mismatch(Schema schema, Document document, String path, String expected) {
        return new Violation(path, "expected " + expected + " for " + targetId(schema) + ", found "
                                   + withArticle(document.type() + " document"));
    }

    /** {@code noun} with its indefinite article ({@code "an integer value"}, {@code "a string document"}). */
    private static String withArticle(String noun) {
        return ("aeiou".indexOf(Character.toLowerCase(noun.charAt(0))) >= 0 ? "an " : "a ") + noun;
    }

    private boolean enforcedRequired(Schema member) {
        return member.hasTrait(TraitKey.REQUIRED_TRAIT) && !member.hasTrait(TraitKey.DEFAULT_TRAIT)
               && !member.hasTrait(TraitKey.IDEMPOTENCY_TOKEN) && !clientOptional.contains(member.id());
    }

    private static boolean sparse(Schema schema) {
        return schema.hasTrait(TraitKey.SPARSE_TRAIT)
               || schema.isMember() && schema.memberTarget().hasTrait(TraitKey.SPARSE_TRAIT);
    }

    /** The shape ID of the shape {@code schema} is or targets. */
    private static ShapeId targetId(Schema schema) {
        return schema.isMember() ? schema.memberTarget().id() : schema.id();
    }

    /** {@code number} as an exact decimal, or {@code null} if it is NaN or infinite. */
    private static BigDecimal decimal(Number number) {
        if (number instanceof BigDecimal decimal) {
            return decimal;
        }
        if (number instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }
        if (number instanceof Float value) {
            return Float.isFinite(value) ? new BigDecimal(Float.toString(value)) : null;
        }
        if (number instanceof Double value) {
            return Double.isFinite(value) ? BigDecimal.valueOf(value) : null;
        }
        return BigDecimal.valueOf(number.longValue());
    }
}
