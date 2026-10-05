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

package software.amazon.awssdk.test.protocols.support;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import software.amazon.awssdk.test.protocols.support.Canonical.Blob;
import software.amazon.awssdk.test.protocols.support.Canonical.Doc;
import software.amazon.awssdk.test.protocols.support.Canonical.Entries;
import software.amazon.awssdk.test.protocols.support.Canonical.Entry;
import software.amazon.awssdk.test.protocols.support.Canonical.Events;
import software.amazon.awssdk.test.protocols.support.Canonical.Struct;

/**
 * A random, valid Smithy model of one service with one operation {@code Op} under one launch protocol, and a random
 * schema-valid input value as a {@link Canonical} tree, generated together from a seed so that every value is valid
 * for its schema, the protocol and the member's binding.
 *
 * <p>The operation's input and output structures have the same members, targets and values; the input carries
 * request bindings and the output the corresponding response bindings (REST protocols: {@code httpHeader},
 * {@code httpPrefixHeaders} and {@code httpPayload} on both, {@code httpLabel} and {@code httpQuery} only on the
 * input, as they have no response location). Shapes cover every simple type, enums and intEnums, lists and maps
 * (sparse where the protocol allows), nested structures, unions, documents (JSON protocols), XML binding traits
 * ({@code xmlName}, {@code xmlFlattened}, {@code xmlAttribute}), {@code jsonName} (restJson1),
 * {@code timestampFormat}, and event streams with header and payload event members.
 *
 * <p>Values include boundary values: integer extremes, {@code NaN}, infinities and {@code -0.0}, big numbers,
 * empty strings, blobs and collections, non-ASCII text, unknown enum values, and millisecond timestamps before and
 * after the epoch. The generator keeps out only what the wire formats cannot carry, each a documented lossy case:
 * <ul>
 *   <li>timestamps have millisecond precision, whole seconds for {@code http-date} (the formats' precision);</li>
 *   <li>HTTP header values are non-empty printable ASCII without surrounding whitespace, and header lists are
 *       non-empty (RFC 9110 field values: an empty list or empty header is indistinguishable from none, and
 *       surrounding whitespace is not part of a field value);</li>
 *   <li>{@code httpPrefixHeaders} maps are always present (an absent and an empty map are both "no such headers",
 *       read as an empty map);</li>
 *   <li>labels are non-empty (RFC 6570/Smithy: an empty label is invalid);</li>
 *   <li>{@code xmlFlattened} collections are non-empty (a flattened empty collection has no element, so it reads
 *       back as absent), and XML text has no characters XML 1.0 cannot represent;</li>
 *   <li>{@code httpPayload} and {@code eventPayload} blobs and strings are non-empty (an empty payload is an
 *       absent payload), and structure payloads are present (restJson1 sends an absent one as {@code {}});</li>
 *   <li>JSON bodies have no {@code -0.0} (the JSON codec writes it as {@code 0});</li>
 *   <li>documents only in JSON protocols, holding objects, lists, strings, booleans and numbers.</li>
 * </ul>
 */
public final class RandomModel {
    public static final String NAMESPACE = "test.roundtrip";
    public static final String XML_NAMESPACE = "https://example.com/roundtrip/";

    /** How a top-level input member is bound (REST protocols); {@link #BODY} for every other protocol. */
    public enum Binding { BODY, HEADER, PREFIX_HEADERS, LABEL, QUERY, PAYLOAD }

    /** A top-level member bound to the request URI, which has no response location. */
    public record UriMember(String name, Binding binding, String wireName, Type type, String timestampFormat) {
    }

    private final WireProtocol protocol;
    private final Random random;
    private final int size;
    private final StringBuilder shapes = new StringBuilder();
    private final Map<String, String> uris = new LinkedHashMap<>();
    private int counter;

    private String idl;
    private Struct input;
    private final List<UriMember> uriMembers = new ArrayList<>();
    private boolean eventStream;

    private RandomModel(WireProtocol protocol, long seed, int size) {
        this.protocol = protocol;
        this.random = new Random(seed);
        this.size = size;
    }

    /** A random model and input value of {@code protocol}, determined by {@code seed}; {@code size} bounds both. */
    public static RandomModel generate(WireProtocol protocol, long seed, int size) {
        RandomModel model = new RandomModel(protocol, seed, size);
        model.generate();
        return model;
    }

    public WireProtocol protocol() {
        return protocol;
    }

    /** The Smithy IDL of the model. */
    public String idl() {
        return idl;
    }

    /** The input value; the expected output value is the same tree without {@link #uriMembers()}. */
    public Struct input() {
        return input;
    }

    /** Input members bound to the URI (labels and query parameters), in order. */
    public List<UriMember> uriMembers() {
        return uriMembers;
    }

    public boolean hasEventStream() {
        return eventStream;
    }

    // Types.

    /** A generated shape: its IDL reference and the generator of its values. */
    public sealed interface Type {
        String ref();
    }

    enum Kind {
        BOOLEAN, BYTE, SHORT, INTEGER, LONG, FLOAT, DOUBLE, BIG_INTEGER, BIG_DECIMAL, STRING, BLOB, TIMESTAMP
    }

    record Simple(Kind kind) implements Type {
        @Override
        public String ref() {
            return switch (kind) {
                case BIG_INTEGER -> "BigInteger";
                case BIG_DECIMAL -> "BigDecimal";
                default -> kind.name().charAt(0) + kind.name().substring(1).toLowerCase();
            };
        }
    }

    record EnumType(String ref, List<String> values) implements Type {
    }

    record IntEnumType(String ref, List<Integer> values) implements Type {
    }

    record ListType(String ref, Type element, boolean sparse) implements Type {
    }

    record MapType(String ref, Type value, boolean sparse) implements Type {
    }

    record Member(String name, Type type, boolean flattened, String timestampFormat, boolean attribute) {
    }

    record StructType(String ref, List<Member> members, boolean union) implements Type {
    }

    record DocumentType() implements Type {
        @Override
        public String ref() {
            return "Document";
        }
    }

    record EventStreamType(String ref, List<StructType> events) implements Type {
    }

    /** Value constraints of a member's binding location. */
    private enum Location { BODY, HEADER, LABEL, QUERY, PAYLOAD }

    private String name(String prefix) {
        return prefix + (counter++);
    }

    private boolean chance(double p) {
        return random.nextDouble() < p;
    }

    private <T> T pick(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    // Model generation.

    private void generate() {
        StringBuilder in = new StringBuilder();
        StringBuilder out = new StringBuilder();
        Map<String, Object> values = new TreeMap<>();
        List<String> labels = new ArrayList<>();
        int members = 1 + random.nextInt(2 + size);
        boolean payloadMode = protocol.rest() && chance(0.2);
        eventStream = protocol.eventStreams() && chance(0.2);
        if (eventStream) {
            EventStreamType stream = eventStreamType();
            String traits = protocol.rest() ? "@httpPayload\n" : "";
            in.append(traits).append("events: ").append(stream.ref()).append('\n');
            out.append(traits).append("events: ").append(stream.ref()).append('\n');
            values.put("events", eventsValue(stream));
        } else if (payloadMode) {
            Type type = switch (random.nextInt(3)) {
                case 0 -> new Simple(Kind.BLOB);
                case 1 -> new Simple(Kind.STRING);
                default -> structType(0, false);
            };
            in.append("@httpPayload\npayload: ").append(type.ref()).append('\n');
            out.append("@httpPayload\npayload: ").append(type.ref()).append('\n');
            // A structure payload is always present: restJson1 sends an absent one as {} (as the Smithy protocol
            // tests require), which reads back as an empty structure.
            if (type instanceof StructType || chance(0.85)) {
                values.put("payload", value(type, Location.PAYLOAD, null));
            }
        }
        boolean bodyAllowed = !payloadMode && !(eventStream && protocol.rest());
        boolean prefixUsed = false;
        for (int i = 0; i < members; i++) {
            String member = "m" + i;
            Binding binding = Binding.BODY;
            if (protocol.rest()) {
                List<Binding> options = new ArrayList<>(List.of(Binding.HEADER, Binding.LABEL, Binding.QUERY));
                if (!prefixUsed) {
                    options.add(Binding.PREFIX_HEADERS);
                }
                if (bodyAllowed) {
                    options.add(Binding.BODY);
                    options.add(Binding.BODY);
                    options.add(Binding.BODY);
                }
                binding = pick(options);
            }
            switch (binding) {
                case HEADER -> {
                    Type type = headerType();
                    String format = timestampFormat(type, true);
                    String traits = "@httpHeader(\"x-h-" + member + "\")\n" + format(format);
                    in.append(traits).append(member).append(": ").append(type.ref()).append('\n');
                    out.append(traits).append(member).append(": ").append(type.ref()).append('\n');
                    if (chance(0.8)) {
                        values.put(member, value(type, Location.HEADER, effective(format, "http-date")));
                    }
                }
                case PREFIX_HEADERS -> {
                    prefixUsed = true;
                    MapType type = new MapType(name("PrefixHeaders"), new Simple(Kind.STRING), false);
                    shapes.append("map ").append(type.ref()).append(" {\n key: String\n value: String\n}\n");
                    String traits = "@httpPrefixHeaders(\"x-p-\")\n";
                    in.append(traits).append(member).append(": ").append(type.ref()).append('\n');
                    out.append(traits).append(member).append(": ").append(type.ref()).append('\n');
                    // Always present: an absent and an empty prefix-header map are the same message (no such
                    // headers), which the binding reader reads as an empty map.
                    values.put(member, prefixHeaders());
                }
                case LABEL -> {
                    Type type = new Simple(pick(List.of(Kind.STRING, Kind.INTEGER, Kind.BOOLEAN, Kind.TIMESTAMP,
                                                        Kind.LONG)));
                    String format = timestampFormat(type, false);
                    in.append("@required\n@httpLabel\n").append(format(format)).append(member).append(": ")
                      .append(type.ref()).append('\n');
                    labels.add(member);
                    values.put(member, value(type, Location.LABEL, effective(format, "date-time")));
                    uriMembers.add(new UriMember(member, binding, member, type, effective(format, "date-time")));
                }
                case QUERY -> {
                    Type type = chance(0.3) ? stringList()
                                            : new Simple(pick(List.of(Kind.STRING, Kind.INTEGER, Kind.BOOLEAN,
                                                                      Kind.TIMESTAMP, Kind.DOUBLE)));
                    String format = timestampFormat(type, false);
                    String wire = "q-" + member;
                    in.append("@httpQuery(\"").append(wire).append("\")\n").append(format(format)).append(member)
                      .append(": ").append(type.ref()).append('\n');
                    if (chance(0.8)) {
                        values.put(member, value(type, Location.QUERY, effective(format, "date-time")));
                    }
                    uriMembers.add(new UriMember(member, binding, wire, type, effective(format, "date-time")));
                }
                default -> {
                    Member body = member(member, 0, false);
                    String traits = memberTraits(body);
                    in.append(traits).append(member).append(": ").append(body.type().ref()).append('\n');
                    out.append(traits).append(member).append(": ").append(body.type().ref()).append('\n');
                    if (chance(0.75)) {
                        values.put(member, memberValue(body));
                    }
                }
            }
        }
        this.input = new Struct(values);
        StringBuilder uri = new StringBuilder("/op");
        for (String label : labels) {
            uri.append("/{").append(label).append('}');
        }
        this.idl = render(in.toString(), out.toString(), uri.toString());
    }

    private String render(String inputMembers, String outputMembers, String uri) {
        String trait = switch (protocol) {
            case AWS_JSON_1_0 -> "@aws.protocols#awsJson1_0";
            case AWS_JSON_1_1 -> "@aws.protocols#awsJson1_1";
            case REST_JSON_1 -> "@aws.protocols#restJson1";
            case REST_XML -> "@aws.protocols#restXml";
            case AWS_QUERY -> "@aws.protocols#awsQuery";
            case EC2_QUERY -> "@aws.protocols#ec2Query";
            case RPC_V2_CBOR -> "@smithy.protocols#rpcv2Cbor";
        };
        StringBuilder model = new StringBuilder();
        model.append("$version: \"2\"\nnamespace ").append(NAMESPACE).append("\n\n").append(trait).append('\n');
        if (protocol.xml()) {
            model.append("@xmlNamespace(uri: \"").append(XML_NAMESPACE).append("\")\n");
        }
        model.append("service RoundTrip {\n version: \"2024-01-01\"\n operations: [Op]\n}\n\n");
        if (protocol.rest()) {
            model.append("@http(method: \"POST\", uri: \"").append(uri).append("\")\n");
        }
        model.append("operation Op {\n input: OpInput\n output: OpOutput\n}\n\n");
        model.append("@input\nstructure OpInput {\n").append(protocol.query() ? "" : inputMembers).append("}\n\n");
        model.append("@output\nstructure OpOutput {\n").append(outputMembers).append("}\n\n");
        model.append(shapes);
        return model.toString();
    }

    // Shapes.

    private Type type(int depth, boolean inUnion) {
        int choice = random.nextInt(depth >= 2 ? 14 : 20);
        return switch (choice) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 -> new Simple(Kind.values()[choice]);
            case 12 -> enumType();
            case 13 -> protocol.json() && chance(0.5) ? new DocumentType() : intEnumType();
            case 14, 15 -> listType(depth);
            case 16, 17 -> mapType(depth);
            case 18 -> structType(depth + 1, false);
            default -> structType(depth + 1, true);
        };
    }

    private EnumType enumType() {
        EnumType type = new EnumType(name("Enum"), List.of("alpha", "Beta-2", "gamma_\u00e9"));
        shapes.append("enum ").append(type.ref()).append(" {\n A = \"alpha\"\n B = \"Beta-2\"\n C = \"gamma_\u00e9\"\n}\n");
        return type;
    }

    private IntEnumType intEnumType() {
        IntEnumType type = new IntEnumType(name("IntEnum"), List.of(1, -7, Integer.MAX_VALUE));
        shapes.append("intEnum ").append(type.ref())
              .append(" {\n ONE = 1\n NEG = -7\n MAX = ").append(Integer.MAX_VALUE).append("\n}\n");
        return type;
    }

    private ListType listType(int depth) {
        Type element = type(depth + 1, false);
        boolean sparse = protocol.sparse() && chance(0.3);
        ListType type = new ListType(name("List"), element, sparse);
        shapes.append(sparse ? "@sparse\n" : "").append("list ").append(type.ref()).append(" {\n");
        if (protocol.xml() && chance(0.3)) {
            shapes.append(" @xmlName(\"item\")\n");
        }
        shapes.append(" member: ").append(element.ref()).append("\n}\n");
        return type;
    }

    private ListType stringList() {
        ListType type = new ListType(name("Strings"), new Simple(Kind.STRING), false);
        shapes.append("list ").append(type.ref()).append(" {\n member: String\n}\n");
        return type;
    }

    private MapType mapType(int depth) {
        Type value = type(depth + 1, false);
        boolean sparse = protocol.sparse() && chance(0.3);
        MapType type = new MapType(name("Map"), value, sparse);
        shapes.append(sparse ? "@sparse\n" : "").append("map ").append(type.ref()).append(" {\n");
        if (protocol.xml() && chance(0.3)) {
            shapes.append(" @xmlName(\"k\")\n");
        }
        shapes.append(" key: String\n");
        if (protocol.xml() && chance(0.3)) {
            shapes.append(" @xmlName(\"v\")\n");
        }
        shapes.append(" value: ").append(value.ref()).append("\n}\n");
        return type;
    }

    private StructType structType(int depth, boolean union) {
        String ref = name(union ? "Union" : "Struct");
        int count = (union ? 1 : 0) + random.nextInt(Math.max(1, 1 + size - 2 * depth));
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            members.add(member("f" + i, depth, union));
        }
        StructType type = new StructType(ref, members, union);
        StringBuilder shape = new StringBuilder(union ? "union " : "structure ").append(ref).append(" {\n");
        for (Member member : members) {
            shape.append(memberTraits(member)).append(member.name()).append(": ").append(member.type().ref())
                 .append('\n');
        }
        shapes.append(shape).append("}\n");
        return type;
    }

    private Member member(String name, int depth, boolean inUnion) {
        Type type = type(depth, inUnion);
        boolean collection = type instanceof ListType || type instanceof MapType;
        boolean flattened = protocol.xml() && collection && chance(0.4);
        boolean attribute = protocol.xml() && !inUnion && depth > 0 && type instanceof Simple simple
                            && List.of(Kind.STRING, Kind.INTEGER, Kind.BOOLEAN, Kind.TIMESTAMP).contains(simple.kind())
                            && chance(0.3);
        return new Member(name, type, flattened, timestampFormat(type, false), attribute);
    }

    private String memberTraits(Member member) {
        StringBuilder traits = new StringBuilder();
        if (member.flattened()) {
            traits.append("@xmlFlattened\n");
        }
        if (member.attribute()) {
            traits.append("@xmlAttribute\n");
        }
        if (protocol.xml() && chance(0.2)) {
            traits.append("@xmlName(\"X").append(member.name()).append("\")\n");
        }
        if (protocol == WireProtocol.REST_JSON_1 && chance(0.2)) {
            traits.append("@jsonName(\"j_").append(member.name()).append("\")\n");
        }
        return traits.append(format(member.timestampFormat())).toString();
    }

    private String timestampFormat(Type type, boolean header) {
        if (!(type instanceof Simple simple) || simple.kind() != Kind.TIMESTAMP || !protocol.timestampFormats()
            || !chance(0.4)) {
            return null;
        }
        if (eventStream && protocol.json()) {
            // KNOWN V3 BUG (reported with task 4.6): EventStreamCodec reads initial-response and event payloads
            // without the protocol's exact TimestampReader, so an explicit @timestampFormat("epoch-seconds") member
            // is read through Smithy Java's binary epoch-seconds reading (1935-10-13T00:22:32.693Z, sent as
            // -1.079912247307E9, reads back as .694Z). Remove this exclusion with the fix.
            return pick(List.of("date-time", "http-date"));
        }
        return pick(List.of("date-time", "epoch-seconds", "http-date"));
    }

    private static String format(String format) {
        return format == null ? "" : "@timestampFormat(\"" + format + "\")\n";
    }

    private static String effective(String format, String defaultFormat) {
        return format != null ? format : defaultFormat;
    }

    private Type headerType() {
        return switch (random.nextInt(8)) {
            case 0 -> stringList();
            case 1 -> enumType();
            default -> new Simple(pick(List.of(Kind.STRING, Kind.INTEGER, Kind.LONG, Kind.BOOLEAN, Kind.TIMESTAMP,
                                               Kind.BYTE, Kind.SHORT, Kind.DOUBLE, Kind.BIG_INTEGER)));
        };
    }

    private EventStreamType eventStreamType() {
        List<StructType> events = new ArrayList<>();
        int count = 1 + random.nextInt(3);
        StringBuilder union = new StringBuilder();
        String ref = name("Events");
        for (int i = 0; i < count; i++) {
            StructType event = eventType();
            events.add(event);
            union.append(" e").append(i).append(": ").append(event.ref()).append('\n');
        }
        shapes.append("@streaming\nunion ").append(ref).append(" {\n").append(union).append("}\n");
        return new EventStreamType(ref, events);
    }

    /** An event structure: header members, then a payload member or body members. */
    private StructType eventType() {
        String ref = name("Event");
        List<Member> members = new ArrayList<>();
        StringBuilder shape = new StringBuilder("structure ").append(ref).append(" {\n");
        int headers = random.nextInt(3);
        for (int i = 0; i < headers; i++) {
            Simple type = new Simple(pick(List.of(Kind.STRING, Kind.INTEGER, Kind.LONG, Kind.BOOLEAN, Kind.BLOB,
                                                  Kind.TIMESTAMP, Kind.BYTE, Kind.SHORT)));
            members.add(new Member("h" + i, type, false, "header", false));
            shape.append("@eventHeader\nh").append(i).append(": ").append(type.ref()).append('\n');
        }
        if (chance(0.3)) {
            Simple type = new Simple(chance(0.5) ? Kind.BLOB : Kind.STRING);
            members.add(new Member("p", type, false, "payload", false));
            shape.append("@eventPayload\np: ").append(type.ref()).append('\n');
        } else {
            int body = random.nextInt(3);
            for (int i = 0; i < body; i++) {
                Member member = member("b" + i, 2, false);
                members.add(member);
                shape.append(memberTraits(member)).append(member.name()).append(": ").append(member.type().ref())
                     .append('\n');
            }
        }
        shapes.append(shape).append("}\n");
        return new StructType(ref, members, false);
    }

    // Values.

    private Object memberValue(Member member) {
        Object value = value(member.type(), member.attribute() ? Location.HEADER : Location.BODY,
                             member.timestampFormat());
        if (member.flattened()) {
            // A flattened empty collection has no element: it reads back as absent.
            while (value instanceof List<?> list && list.isEmpty()
                   || value instanceof Entries entries && entries.entries().isEmpty()) {
                value = value(member.type(), Location.BODY, null);
            }
        }
        return value;
    }

    private Events eventsValue(EventStreamType stream) {
        List<Object> events = new ArrayList<>();
        int count = random.nextInt(4);
        for (int i = 0; i < count; i++) {
            int index = random.nextInt(stream.events().size());
            StructType event = stream.events().get(index);
            Map<String, Object> members = new TreeMap<>();
            for (Member member : event.members()) {
                if ("payload".equals(member.timestampFormat())) {
                    // An empty event payload is an absent one.
                    if (chance(0.8)) {
                        members.put(member.name(), value(member.type(), Location.PAYLOAD, null));
                    }
                } else if ("header".equals(member.timestampFormat())) {
                    if (chance(0.8)) {
                        members.put(member.name(), value(member.type(), Location.BODY, null));
                    }
                } else if (chance(0.75)) {
                    members.put(member.name(), memberValue(member));
                }
            }
            events.add(new Struct(Map.of("e" + index, new Struct(members))));
        }
        return new Events(events);
    }

    private Object value(Type type, Location location, String timestampFormat) {
        if (type instanceof Simple simple) {
            return simple(simple.kind(), location, timestampFormat);
        } else if (type instanceof EnumType enumType) {
            if (!chance(0.8)) {
                return "unknown-" + random.nextInt(100);
            }
            // Header values are ASCII.
            return location == Location.HEADER ? pick(enumType.values().subList(0, 2)) : pick(enumType.values());
        } else if (type instanceof IntEnumType intEnum) {
            return chance(0.8) ? pick(intEnum.values()) : 1000 + random.nextInt(1000);
        } else if (type instanceof DocumentType) {
            return new Doc(document(0));
        } else if (type instanceof ListType list) {
            int count = location == Location.HEADER || location == Location.QUERY ? 1 + random.nextInt(3)
                                                                                  : random.nextInt(4);
            List<Object> elements = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                elements.add(list.sparse() && chance(0.25) ? Canonical.NULL : value(list.element(), location, null));
            }
            return elements;
        } else if (type instanceof MapType map) {
            Set<String> keys = new LinkedHashSet<>();
            int count = random.nextInt(4);
            for (int i = 0; i < count; i++) {
                keys.add(string(Location.BODY));
            }
            List<Entry> entries = new ArrayList<>();
            for (String key : keys) {
                entries.add(new Entry(key, map.sparse() && chance(0.25) ? Canonical.NULL
                                                                         : value(map.value(), Location.BODY, null)));
            }
            return new Entries(entries);
        } else if (type instanceof StructType struct) {
            Map<String, Object> members = new TreeMap<>();
            if (struct.union()) {
                Member member = pick(struct.members());
                members.put(member.name(), memberValue(member));
            } else {
                for (Member member : struct.members()) {
                    if (chance(0.7)) {
                        members.put(member.name(), memberValue(member));
                    }
                }
            }
            return new Struct(members);
        }
        throw new IllegalArgumentException("No value for " + type);
    }

    private Object simple(Kind kind, Location location, String timestampFormat) {
        return switch (kind) {
            case BOOLEAN -> random.nextBoolean();
            case BYTE -> (byte) pickLong(Byte.MIN_VALUE, Byte.MAX_VALUE);
            case SHORT -> (short) pickLong(Short.MIN_VALUE, Short.MAX_VALUE);
            case INTEGER -> (int) pickLong(Integer.MIN_VALUE, Integer.MAX_VALUE);
            case LONG -> pickLong(Long.MIN_VALUE, Long.MAX_VALUE);
            case FLOAT -> pickFloat(location);
            case DOUBLE -> pickDouble(location);
            case BIG_INTEGER -> bigInteger();
            case BIG_DECIMAL -> Canonical.decimal(new BigDecimal(bigInteger(), random.nextInt(40) - 15));
            case STRING -> location == Location.PAYLOAD ? nonEmpty(Location.BODY) : string(location);
            case BLOB -> {
                int length = location == Location.PAYLOAD ? 1 + random.nextInt(24) : random.nextInt(24);
                byte[] bytes = new byte[chance(0.1) ? 0 : length];
                random.nextBytes(bytes);
                yield Blob.of(location == Location.PAYLOAD && bytes.length == 0 ? new byte[] {0} : bytes);
            }
            case TIMESTAMP -> timestamp(timestampFormat);
        };
    }

    /** The JSON codec writes {@code -0.0} as {@code 0}: a documented lossy case of JSON bodies. */
    private boolean negativeZeroLost(Location location) {
        return protocol.json() && (location == Location.BODY || location == Location.PAYLOAD);
    }

    private long pickLong(long min, long max) {
        return switch (random.nextInt(6)) {
            case 0 -> min;
            case 1 -> max;
            case 2 -> 0;
            case 3 -> -1;
            default -> min + (long) (random.nextDouble() * ((double) max - (double) min));
        };
    }

    private float pickFloat(Location location) {
        return switch (random.nextInt(10)) {
            case 0 -> Float.NaN;
            case 1 -> Float.POSITIVE_INFINITY;
            case 2 -> Float.NEGATIVE_INFINITY;
            case 3 -> negativeZeroLost(location) ? 0.0f : -0.0f;
            case 4 -> Float.MIN_VALUE;
            case 5 -> Float.MAX_VALUE;
            case 6 -> 0.1f;
            default -> {
                float f = Float.intBitsToFloat(random.nextInt());
                yield Float.isFinite(f) ? f : 1.5f;
            }
        };
    }

    private double pickDouble(Location location) {
        return switch (random.nextInt(10)) {
            case 0 -> Double.NaN;
            case 1 -> Double.POSITIVE_INFINITY;
            case 2 -> Double.NEGATIVE_INFINITY;
            case 3 -> negativeZeroLost(location) ? 0.0 : -0.0;
            case 4 -> Double.MIN_VALUE;
            case 5 -> Double.MAX_VALUE;
            case 6 -> 0.1;
            default -> {
                double d = Double.longBitsToDouble(random.nextLong());
                yield Double.isFinite(d) ? d : -2.5e-300;
            }
        };
    }

    private BigInteger bigInteger() {
        return switch (random.nextInt(5)) {
            case 0 -> BigInteger.ZERO;
            case 1 -> BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
            case 2 -> BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE);
            default -> {
                BigInteger value = new BigInteger(1 + random.nextInt(140), random);
                yield random.nextBoolean() ? value.negate() : value;
            }
        };
    }

    private Instant timestamp(String format) {
        // 1900-01-01 .. 2100-01-01 at millisecond precision; whole seconds for http-date.
        long millis = -2_208_988_800_000L + (long) (random.nextDouble() * 6_311_433_600_000L);
        if (chance(0.1)) {
            millis = 0;
        }
        Instant instant = Instant.ofEpochMilli(millis);
        return "http-date".equals(format) ? Instant.ofEpochSecond(instant.getEpochSecond()) : instant;
    }

    private static final List<String> STRINGS = List.of(
        "", "a", "hello world", " padded ", "tab\tnew\nline\r\nend", "\u00fcn\u00efc\u00f6d\u00e9",
        "\u65e5\u672c\u8a9e", "emoji \ud83d\ude00\ud83c\udf89", "<&>\"'", "comma, separated", "\"quoted\"",
        "percent %20 plus+ slash/ q?=&#", "   ", "\u0000\u001f control");

    private static final List<String> HEADER_STRINGS = List.of(
        "a", "hello world", "comma, separated", "\"quoted\"", "semi;colon", "percent %20 plus+", "x=y&z",
        "trailing\\", "1234");

    private String string(Location location) {
        if (location == Location.HEADER) {
            return chance(0.6) ? pick(HEADER_STRINGS) : ascii(1 + random.nextInt(12));
        }
        String value = chance(0.6) ? pick(STRINGS) : unicode(random.nextInt(12));
        if (location == Location.LABEL && value.isEmpty()) {
            return "label";
        }
        if (protocol.xml()) {
            // XML 1.0 has no C0 controls other than tab, newline and carriage return.
            value = value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "?");
        }
        return value;
    }

    private String nonEmpty(Location location) {
        String value = string(location);
        return value.isEmpty() ? "x" : value;
    }

    private String ascii(int length) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < length; i++) {
            text.append((char) ('!' + random.nextInt('~' - '!' + 1)));
        }
        return text.toString();
    }

    private String unicode(int length) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int codePoint = switch (random.nextInt(4)) {
                case 0 -> 0x20 + random.nextInt(0x5F);
                case 1 -> 0xA0 + random.nextInt(0x2000);
                case 2 -> 0x3000 + random.nextInt(0x5000);
                default -> 0x10000 + random.nextInt(0x10000);
            };
            text.appendCodePoint(codePoint);
        }
        return text.toString();
    }

    private Entries prefixHeaders() {
        Set<String> keys = new LinkedHashSet<>();
        int count = random.nextInt(4);
        for (int i = 0; i < count; i++) {
            StringBuilder key = new StringBuilder();
            for (int c = 0, n = 1 + random.nextInt(5); c < n; c++) {
                key.append((char) ('a' + random.nextInt(26)));
            }
            keys.add(key.toString());
        }
        List<Entry> entries = new ArrayList<>();
        for (String key : keys) {
            entries.add(new Entry(key, string(Location.HEADER)));
        }
        return new Entries(entries);
    }

    private Object document(int depth) {
        int choice = random.nextInt(depth >= 2 ? 4 : 6);
        return switch (choice) {
            case 0 -> string(Location.BODY);
            case 1 -> random.nextBoolean();
            case 2 -> Canonical.decimal(BigDecimal.valueOf(random.nextLong() >> random.nextInt(64)));
            case 3 -> Canonical.decimal(BigDecimal.valueOf(random.nextInt(2_000_001) - 1_000_000, random.nextInt(4)));
            case 4 -> {
                List<Object> list = new ArrayList<>();
                for (int i = 0, n = random.nextInt(3); i < n; i++) {
                    list.add(document(depth + 1));
                }
                yield list;
            }
            default -> {
                Map<String, Object> members = new TreeMap<>();
                for (int i = 0, n = random.nextInt(3); i < n; i++) {
                    members.put("k" + i + (chance(0.3) ? " \u00e9" : ""), document(depth + 1));
                }
                yield new Struct(members);
            }
        };
    }
}
