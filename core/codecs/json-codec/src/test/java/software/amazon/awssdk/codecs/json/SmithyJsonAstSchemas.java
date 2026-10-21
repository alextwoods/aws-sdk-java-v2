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

package software.amazon.awssdk.codecs.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.schema.PreludeSchemas;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaBuilder;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.schema.traits.JsonNameTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.Trait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;

/**
 * Builds {@link Schema}s from a <a href="https://smithy.io/2.0/spec/json-ast.html">Smithy JSON AST</a> read as a
 * {@link Document}, for the SEP {@code serdeTests} suite ({@link DocumentSerdeTestSuite}).
 *
 * <p>This is a test stand-in for the Schema_Converter (design C.1, task 13.1), which is not implemented yet. It
 * supports only what a codec test model needs: simple shapes, {@code enum}, {@code intEnum}, {@code structure},
 * {@code union}, {@code list} and {@code map} shapes, member targets in the model or in the prelude, recursion, and the
 * {@code jsonName}, {@code timestampFormat}, {@code xmlName} and {@code enumValue} traits. Anything else fails with an
 * {@link IllegalArgumentException} naming the shape, so a model that needs more cannot be converted silently wrong.
 */
final class SmithyJsonAstSchemas {
    private static final Map<ShapeId, Schema> PRELUDE = new HashMap<>();

    static {
        for (Schema schema : new Schema[] {PreludeSchemas.BLOB, PreludeSchemas.BOOLEAN, PreludeSchemas.STRING,
                                           PreludeSchemas.TIMESTAMP, PreludeSchemas.BYTE, PreludeSchemas.SHORT,
                                           PreludeSchemas.INTEGER, PreludeSchemas.LONG, PreludeSchemas.FLOAT,
                                           PreludeSchemas.DOUBLE, PreludeSchemas.BIG_INTEGER,
                                           PreludeSchemas.BIG_DECIMAL, PreludeSchemas.DOCUMENT,
                                           PreludeSchemas.UNIT}) {
            PRELUDE.put(schema.id(), schema);
        }
    }

    private SmithyJsonAstSchemas() {
    }

    /**
     * Converts every shape of a model.
     *
     * @param model the JSON AST of the model
     * @return the schema of each shape in the model, by ShapeId
     * @throws IllegalArgumentException if the model uses a shape type, trait or target this class does not support
     */
    static Map<ShapeId, Schema> convert(Document model) {
        Map<String, Document> shapes = model.getMember("shapes").asMap();
        Map<ShapeId, Schema> simple = new LinkedHashMap<>();
        Map<ShapeId, SchemaBuilder> aggregates = new LinkedHashMap<>();

        for (Map.Entry<String, Document> entry : shapes.entrySet()) {
            ShapeId id = ShapeId.from(entry.getKey());
            Document shape = entry.getValue();
            String type = shape.getMember("type").asString();
            Trait[] traits = traits(id.toString(), shape);
            SchemaBuilder builder = aggregateBuilder(type, id, traits);
            if (builder != null) {
                aggregates.put(id, builder);
            } else {
                simple.put(id, simpleSchema(type, id, shape, traits));
            }
        }

        for (Map.Entry<ShapeId, SchemaBuilder> entry : aggregates.entrySet()) {
            Document shape = shapes.get(entry.getKey().toString());
            for (Map.Entry<String, Document> member : members(shape).entrySet()) {
                String memberId = entry.getKey() + "$" + member.getKey();
                Document target = member.getValue().getMember("target");
                if (target == null) {
                    throw new IllegalArgumentException("Member " + memberId + " has no target");
                }
                ShapeId targetId = ShapeId.from(target.asString());
                Trait[] traits = traits(memberId, member.getValue());
                if (aggregates.containsKey(targetId)) {
                    entry.getValue().putMember(member.getKey(), aggregates.get(targetId), traits);
                } else {
                    entry.getValue().putMember(member.getKey(), target(memberId, targetId, simple), traits);
                }
            }
        }

        Map<ShapeId, Schema> result = new LinkedHashMap<>(simple);
        List<Schema> built = new ArrayList<>();
        for (Map.Entry<ShapeId, SchemaBuilder> entry : aggregates.entrySet()) {
            Schema schema = entry.getValue().build();
            built.add(schema);
            result.put(entry.getKey(), schema);
        }
        for (Schema schema : built) {
            result.put(schema.id(), schema.resolve());
        }
        return result;
    }

    private static SchemaBuilder aggregateBuilder(String type, ShapeId id, Trait[] traits) {
        switch (type) {
            case "structure":
                return Schema.structureBuilder(id, traits);
            case "union":
                return Schema.unionBuilder(id, traits);
            case "list":
                return Schema.listBuilder(id, traits);
            case "map":
                return Schema.mapBuilder(id, traits);
            default:
                return null;
        }
    }

    private static Schema simpleSchema(String type, ShapeId id, Document shape, Trait[] traits) {
        switch (type) {
            case "blob":
                return Schema.createBlob(id, traits);
            case "boolean":
                return Schema.createBoolean(id, traits);
            case "string":
                return Schema.createString(id, traits);
            case "byte":
                return Schema.createByte(id, traits);
            case "short":
                return Schema.createShort(id, traits);
            case "integer":
                return Schema.createInteger(id, traits);
            case "long":
                return Schema.createLong(id, traits);
            case "float":
                return Schema.createFloat(id, traits);
            case "double":
                return Schema.createDouble(id, traits);
            case "bigInteger":
                return Schema.createBigInteger(id, traits);
            case "bigDecimal":
                return Schema.createBigDecimal(id, traits);
            case "timestamp":
                return Schema.createTimestamp(id, traits);
            case "document":
                return Schema.createDocument(id, traits);
            case "enum":
                Set<String> values = new LinkedHashSet<>();
                for (Map.Entry<String, Document> member : members(shape).entrySet()) {
                    Document value = enumValue(member.getValue());
                    values.add(value == null ? member.getKey() : value.asString());
                }
                return Schema.createEnum(id, values, traits);
            case "intEnum":
                Set<Integer> intValues = new LinkedHashSet<>();
                for (Map.Entry<String, Document> member : members(shape).entrySet()) {
                    Document value = enumValue(member.getValue());
                    if (value == null) {
                        throw new IllegalArgumentException("intEnum member " + id + "$" + member.getKey()
                                                           + " has no enumValue");
                    }
                    intValues.add(value.asInteger());
                }
                return Schema.createIntEnum(id, intValues, traits);
            default:
                throw new IllegalArgumentException("Unsupported shape type " + type + " of " + id);
        }
    }

    private static Map<String, Document> members(Document shape) {
        Map<String, Document> members = new LinkedHashMap<>();
        for (String name : new String[] {"member", "key", "value"}) {
            Document member = shape.getMember(name);
            if (member != null) {
                members.put(name, member);
            }
        }
        Document named = shape.getMember("members");
        if (named != null) {
            members.putAll(named.asMap());
        }
        return members;
    }

    private static Document enumValue(Document member) {
        Document traits = member.getMember("traits");
        return traits == null ? null : traits.getMember("smithy.api#enumValue");
    }

    private static Schema target(String memberId, ShapeId targetId, Map<ShapeId, Schema> simple) {
        Schema target = simple.containsKey(targetId) ? simple.get(targetId) : PRELUDE.get(targetId);
        if (target == null) {
            throw new IllegalArgumentException("Member " + memberId + " targets unsupported shape " + targetId);
        }
        return target;
    }

    private static Trait[] traits(String owner, Document shapeOrMember) {
        Document traits = shapeOrMember.getMember("traits");
        if (traits == null) {
            return new Trait[0];
        }
        List<Trait> result = new ArrayList<>();
        for (Map.Entry<String, Document> trait : traits.asMap().entrySet()) {
            switch (trait.getKey()) {
                case "smithy.api#jsonName":
                    result.add(new JsonNameTrait(trait.getValue().asString()));
                    break;
                case "smithy.api#xmlName":
                    result.add(new XmlNameTrait(trait.getValue().asString()));
                    break;
                case "smithy.api#timestampFormat":
                    result.add(new TimestampFormatTrait(trait.getValue().asString()));
                    break;
                case "smithy.api#enumValue":
                    // Read by the enum and intEnum shapes; enum members have no schemas.
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported trait " + trait.getKey() + " on " + owner);
            }
        }
        return result.toArray(new Trait[0]);
    }
}
