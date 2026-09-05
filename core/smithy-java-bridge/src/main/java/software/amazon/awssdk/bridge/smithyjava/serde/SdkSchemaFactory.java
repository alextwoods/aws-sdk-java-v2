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

package software.amazon.awssdk.bridge.smithyjava.serde;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.SchemaBuilder;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpHeaderTrait;
import software.amazon.smithy.model.traits.HttpLabelTrait;
import software.amazon.smithy.model.traits.HttpPayloadTrait;
import software.amazon.smithy.model.traits.HttpPrefixHeadersTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.JsonNameTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;

/**
 * Builds a smithy-java {@link Schema} for a generated AWS SDK v2 shape directly from its
 * {@code SDK_FIELDS} (the per-member metadata codegen already emits: member name +
 * {@link MarshallingType} + list/map element fields). Generated POJOs call this once into a
 * static {@code $SCHEMA}, so a v2 POJO can implement smithy-java's {@code SerializableStruct}
 * and serialize itself with no bridge wrapper.
 *
 * <p>Member <b>order</b> follows {@code SDK_FIELDS}, and each member's smithy
 * {@code memberIndex()} equals its index in that list — so generated {@code serializeMembers}
 * can resolve members by index, monomorphically, like a code-generated smithy shape.
 */
@SdkProtectedApi
public final class SdkSchemaFactory {

    private static final Trait[] NO_TRAITS = new Trait[0];

    private SdkSchemaFactory() {
    }

    /** Build a structure {@link Schema} for {@code shapeId} from the v2 shape's SDK fields. */
    public static Schema structure(String shapeId, List<SdkField<?>> sdkFields) {
        // inProgress: recursion guard keyed by synthetic ShapeId, so a self-referential shape
        // (e.g. DynamoDB AttributeValue, which contains map<String,AttributeValue>) reuses the
        // in-flight SchemaBuilder instead of recursing forever. smithy's putMember(name,
        // SchemaBuilder) accepts an unbuilt builder precisely for this.
        return structure(ShapeId.from(shapeId), sdkFields, new HashMap<>());
    }

    private static Schema structure(ShapeId id, List<SdkField<?>> sdkFields, Map<ShapeId, SchemaBuilder> inProgress) {
        SchemaBuilder builder = Schema.structureBuilder(id);
        inProgress.put(id, builder);
        for (SdkField<?> field : sdkFields) {
            putMember(builder, smithyMemberName(field), field, field.memberName(), inProgress,
                      bindingTraits(field, field.memberName()));
        }
        return builder.build();
    }

    // Adds one member to {@code parent}. Uses putMember(name, SchemaBuilder) for in-flight nested
    // structs (recursion) and putMember(name, Schema) for everything resolved.
    //
    // {@code traits} is supplied by the caller rather than derived here because the two kinds of member
    // get their traits from different places: a STRUCTURE member from its own SdkField's binding
    // metadata (see bindingTraits), a list/map element from the *container's* ListTrait/MapTrait, which
    // is where v2 records the element's XML element name. This is the "C2J as a runtime shim over
    // smithy": the v2 SDK_FIELDS trait vocabulary (LocationTrait/PayloadTrait/...) is mapped onto
    // smithy's binding traits so the smithy HttpBindingSerializer routes each member to
    // URI/header/query/payload/body correctly.
    private static void putMember(SchemaBuilder parent, String memberName, SdkField<?> field,
                                  String name, Map<ShapeId, SchemaBuilder> inProgress, Trait[] traits) {
        MarshallingType<?> t = field.marshallingType();
        if (t == MarshallingType.SDK_POJO) {
            SdkPojo nested = nestedPojo(field);
            if (nested != null) {
                ShapeId nestedId = nestedId(nested, name);
                SchemaBuilder existing = inProgress.get(nestedId);
                if (existing != null) {
                    parent.putMember(memberName, existing, traits);   // recursive back-edge
                } else {
                    SchemaBuilder nestedBuilder = Schema.structureBuilder(nestedId);
                    inProgress.put(nestedId, nestedBuilder);
                    for (SdkField<?> f : nested.sdkFields()) {
                        putMember(nestedBuilder, smithyMemberName(f), f, f.memberName(), inProgress,
                                  bindingTraits(f, f.memberName()));
                    }
                    wireBuilderSupplier(nestedBuilder, field);
                    parent.putMember(memberName, nestedBuilder, traits);
                }
                return;
            }
        }
        parent.putMember(memberName, memberSchema(field, name, inProgress), traits);
    }

    /**
     * The name to register a structure member under in the smithy schema.
     *
     * <p>Normally the v2 member name. The exception is URI labels: {@code @httpLabel} carries no name of
     * its own, so smithy matches a label to a member by <em>member name</em>, and the member name has to
     * be the name in the URI pattern. v2's member name is the Java-facing one and codegen is free to
     * change it — S3's {@code customization.config} renames CopyObject's {@code Bucket}/{@code Key} to
     * {@code DestinationBucket}/{@code DestinationKey} — which left the {@code /{Key+}} label unbound
     * and every CopyObject failing with "URI label `Key` not set". The {@code locationName} is the
     * modeled name and does not move, so labels bind to that.
     *
     * <p>Safe to differ only because a label member is never also a body member: body element names come
     * from the member name, but a PATH-located member is not in the body. Header and query members keep
     * the v2 member name, since their wire name travels in {@code @httpHeader}/{@code @httpQuery}
     * instead. Member <em>order</em> — and therefore {@code memberIndex()}, which generated
     * {@code serializeMembers} and {@code SmithyMemberConsumer} switch on — is untouched.
     */
    private static String smithyMemberName(SdkField<?> field) {
        LocationTrait loc = field.getTrait(LocationTrait.class);
        if (loc == null || loc.locationName() == null) {
            return field.memberName();
        }
        switch (loc.location()) {
            case PATH:
            case GREEDY_PATH:
                return loc.locationName();
            default:
                return field.memberName();
        }
    }

    // Translate a v2 SdkField's binding/wire metadata into smithy member traits. FAIL LOUD on any
    // MarshallLocation we don't yet map, so we never silently emit wrong bytes.
    private static Trait[] bindingTraits(SdkField<?> field, String memberName) {
        List<Trait> traits = new ArrayList<>(2);

        // @httpPayload means "this single member IS the whole body" in smithy. In v2 that is ONLY
        // the explicit PayloadTrait — NOT MarshallLocation.PAYLOAD, which v2 uses for every ordinary
        // body member ("serialized into the body document"). Mapping PAYLOAD-location -> @httpPayload
        // was wrong: it marked all body members as the whole payload, so the deserializer dumped the
        // entire body into the last one. Only PayloadTrait -> @httpPayload.
        boolean isPayload = field.getTrait(PayloadTrait.class) != null;

        // Track whether this is an ordinary body member (PAYLOAD location, no PayloadTrait) — those
        // get jsonName/xmlName for divergent wire names but no HTTP location trait.
        boolean isBodyMember = false;

        LocationTrait loc = field.getTrait(LocationTrait.class);
        if (loc != null) {
            MarshallLocation location = loc.location();
            String wireName = loc.locationName() != null ? loc.locationName() : memberName;
            switch (location) {
                case PATH:
                case GREEDY_PATH:
                    traits.add(new HttpLabelTrait());
                    break;
                case HEADER:
                    // v2 spells a header *map* as a HEADER-located map whose locationName is the shared
                    // prefix ("x-amz-meta-"); smithy has a separate trait for that. Without the split,
                    // the map got a plain @httpHeader and smithy silently wrote nothing at all — every
                    // CopyObject/PutObject user metadata entry vanished with no error anywhere.
                    if (field.marshallingType() == MarshallingType.MAP) {
                        traits.add(new HttpPrefixHeadersTrait(wireName));
                    } else {
                        traits.add(new HttpHeaderTrait(wireName));
                    }
                    break;
                case QUERY_PARAM:
                    traits.add(new HttpQueryTrait(wireName));
                    break;
                case PAYLOAD:
                    // Ordinary body member (unless an explicit PayloadTrait also present, handled
                    // above). No HTTP location trait — it just lives in the body document.
                    isBodyMember = true;
                    break;
                case STATUS_CODE:
                    // Response-only; not part of request serialization. No request-side trait.
                    break;
                default:
                    throw new IllegalStateException(
                        "SdkSchemaFactory: unmapped MarshallLocation " + location + " for member '"
                        + memberName + "'. Add a smithy-trait mapping before enabling this protocol.");
            }
        }

        if (isPayload) {
            traits.add(new HttpPayloadTrait());
        }

        // Wire-name traits for body members whose serialized name differs from the member name.
        // restJson uses jsonName (codec useJsonName=true); restXml/query use xmlName. We can't tell
        // the protocol here, so emit BOTH when a body member's locationName diverges — each codec
        // reads only the trait it cares about, so the unused one is inert.
        if (isBodyMember && loc.locationName() != null && !loc.locationName().equals(memberName)) {
            traits.add(new JsonNameTrait(loc.locationName()));
            traits.add(new XmlNameTrait(loc.locationName()));
        }

        // @xmlFlattened: v2 spells this on the container trait, smithy on the member. A flattened list
        // repeats its item element directly under the parent; a non-flattened one nests them inside a
        // wrapper named after the member. Getting this wrong is a silent wrong-bytes bug, not an error:
        // S3's CompleteMultipartUpload wants <Part>..</Part><Part>..</Part> and without the trait
        // smithy emits <Part><member>..</member><member>..</member></Part>, which S3 rejects. It is
        // equally wrong on the read side, where it surfaces as "Expected list item 'member'".
        ListTrait listTrait = field.getTrait(ListTrait.class);
        MapTrait mapTrait = field.getTrait(MapTrait.class);
        if ((listTrait != null && listTrait.isFlattened()) || (mapTrait != null && mapTrait.isFlattened())) {
            traits.add(new XmlFlattenedTrait());
        }

        // Timestamp format (epoch/iso8601/rfc822) — affects both header and body timestamps.
        TimestampFormatTrait tsFormat = field.getTrait(TimestampFormatTrait.class);
        if (tsFormat != null) {
            traits.add(new software.amazon.smithy.model.traits.TimestampFormatTrait(
                    smithyTimestampFormat(tsFormat.format())));
        }

        return traits.isEmpty() ? NO_TRAITS : traits.toArray(new Trait[0]);
    }

    /**
     * The {@code @xmlName} for a list item or map key/value, or no traits if it is already the default.
     *
     * <p>v2 records this on the container ({@code ListTrait.memberLocationName}, {@code MapTrait}'s
     * key/value location names) rather than on the element itself, and leaves it null when the element
     * uses smithy's default name. S3's {@code Tagging} needs it:
     * {@code <TagSet><Tag>..</Tag></TagSet>}, not {@code <TagSet><member>..</member></TagSet>}.
     *
     * <p>Only {@code @xmlName} is emitted, not the {@code @jsonName} that {@link #bindingTraits} pairs
     * it with. A JSON list item has no name at all, so a {@code jsonName} on one would either be inert
     * or actively wrong; there is nothing to be defensive about.
     */
    private static Trait[] elementNameTrait(String locationName, String defaultName) {
        if (locationName == null || locationName.equals(defaultName)) {
            return NO_TRAITS;
        }
        return new Trait[] {new XmlNameTrait(locationName)};
    }

    private static String smithyTimestampFormat(TimestampFormatTrait.Format format) {
        switch (format) {
            case ISO_8601:
                return "date-time";
            case RFC_822:
                return "http-date";
            case UNIX_TIMESTAMP:
            case UNIX_TIMESTAMP_MILLIS:
                return "epoch-seconds";
            default:
                throw new IllegalStateException("SdkSchemaFactory: unmapped timestamp format " + format);
        }
    }

    // {@code name} is a synthetic, unique-within-schema hint used to mint the target type's
    // ShapeId. List/map element SdkFields carry a null memberName(), so we derive their name from
    // the enclosing member's name (e.g. "Item" -> list element "ItemMember", map value "ItemValue").
    // The id only names the target TYPE; the JSON key comes from putMember(memberName, ...), so
    // synthetic ids never affect wire output.
    private static Schema memberSchema(SdkField<?> field, String name, Map<ShapeId, SchemaBuilder> inProgress) {
        MarshallingType<?> t = field.marshallingType();
        if (t == MarshallingType.STRING) {
            return Schema.createString(syntheticId(name));
        } else if (t == MarshallingType.INTEGER) {
            return Schema.createInteger(syntheticId(name));
        } else if (t == MarshallingType.LONG) {
            return Schema.createLong(syntheticId(name));
        } else if (t == MarshallingType.SHORT) {
            return Schema.createShort(syntheticId(name));
        } else if (t == MarshallingType.FLOAT) {
            return Schema.createFloat(syntheticId(name));
        } else if (t == MarshallingType.DOUBLE) {
            return Schema.createDouble(syntheticId(name));
        } else if (t == MarshallingType.BIG_DECIMAL) {
            return Schema.createBigDecimal(syntheticId(name));
        } else if (t == MarshallingType.BOOLEAN) {
            return Schema.createBoolean(syntheticId(name));
        } else if (t == MarshallingType.INSTANT) {
            return Schema.createTimestamp(syntheticId(name));
        } else if (t == MarshallingType.SDK_BYTES) {
            return Schema.createBlob(syntheticId(name));
        } else if (t == MarshallingType.SDK_POJO) {
            // Reached only when nestedPojo() was null (no constructor) — keep prior placeholder.
            return Schema.createString(syntheticId(name));
        } else if (t == MarshallingType.LIST) {
            ListTrait lt = field.getTrait(ListTrait.class);
            SchemaBuilder list = Schema.listBuilder(syntheticId(name));
            if (lt != null) {
                putMember(list, "member", lt.memberFieldInfo(), name + "Member", inProgress,
                          elementNameTrait(lt.memberLocationName(), "member"));
            } else {
                list.putMember("member", Schema.createString(syntheticId(name + "Member")));
            }
            return list.build();
        } else if (t == MarshallingType.MAP) {
            MapTrait mt = field.getTrait(MapTrait.class);
            SchemaBuilder map = Schema.mapBuilder(syntheticId(name));
            if (mt != null) {
                map.putMember("key", Schema.createString(syntheticId(name + "Key")),
                              elementNameTrait(mt.keyLocationName(), "key"));
                putMember(map, "value", mt.valueFieldInfo(), name + "Value", inProgress,
                          elementNameTrait(mt.valueLocationName(), "value"));
            } else {
                map.putMember("key", Schema.createString(syntheticId(name + "Key")));
                map.putMember("value", Schema.createString(syntheticId(name + "Value")));
            }
            return map.build();
        }
        // DOCUMENT / STREAM / NULL etc.: placeholder string.
        return Schema.createString(syntheticId(name));
    }

    // Wire the smithy ShapeBuilder supplier for a nested struct member, so the deserialize path can
    // instantiate the right builder. The element SdkField's constructor() yields a fresh v2 builder,
    // which (when the shape is generated with the flag) is itself a smithy ShapeBuilder.
    private static void wireBuilderSupplier(SchemaBuilder nestedBuilder, SdkField<?> field) {
        Supplier<SdkPojo> ctor = field.constructor();
        if (ctor != null) {
            nestedBuilder.builderSupplier(() -> {
                SdkPojo b = ctor.get();
                return b instanceof software.amazon.smithy.java.core.schema.ShapeBuilder
                        ? (software.amazon.smithy.java.core.schema.ShapeBuilder<?>) b
                        : null;
            });
        }
    }

    /**
     * The {@link ShapeId} to give a nested structure: its real modeled id when the shape can tell us,
     * otherwise a synthetic one derived from the member name.
     *
     * <p>The synthetic id is not as inert as it looks. For JSON it genuinely never reaches the wire —
     * element names come from {@code putMember(memberName, ...)}. XML is different: the root element of
     * an {@code @httpPayload} structure is named from the *target shape's* id, so a payload whose
     * synthetic id was {@code TaggingStruct} went out as {@code <TaggingStruct>} where S3 requires
     * {@code <Tagging>}. Using the real id also removes a latent collision: the synthetic id keys only
     * on the member name, so two distinct shapes reached through same-named members in different
     * sub-trees would share one entry in {@code inProgress} and the second would silently get the
     * first's schema.
     *
     * <p>Returns the synthetic id when {@code schema()} is null, which is what a self-referential shape
     * reports while its own static {@code $SCHEMA} initializer is still on the stack. That is the
     * DynamoDB {@code AttributeValue} case, and it must keep working: falling back preserves the
     * pre-existing recursion guard exactly.
     */
    private static ShapeId nestedId(SdkPojo nested, String memberName) {
        if (nested instanceof software.amazon.smithy.java.core.schema.ShapeBuilder) {
            Schema own = ((software.amazon.smithy.java.core.schema.ShapeBuilder<?>) nested).schema();
            if (own != null) {
                return own.id();
            }
        }
        return syntheticId(memberName + "Struct");
    }

    private static SdkPojo nestedPojo(SdkField<?> field) {
        Supplier<SdkPojo> ctor = field.constructor();
        return ctor != null ? ctor.get() : null;
    }

    private static ShapeId syntheticId(String name) {
        return ShapeId.fromParts("software.amazon.awssdk.bridge.synthetic", capitalize(name));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "Anon";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
