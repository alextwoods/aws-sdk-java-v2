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

package software.amazon.awssdk.core.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.schema.internal.SdkFieldsExtension;
import software.amazon.awssdk.core.traits.DefaultValueTrait;
import software.amazon.awssdk.core.traits.JsonValueTrait;
import software.amazon.awssdk.core.traits.ListTrait;
import software.amazon.awssdk.core.traits.LocationTrait;
import software.amazon.awssdk.core.traits.MapTrait;
import software.amazon.awssdk.core.traits.PayloadTrait;
import software.amazon.awssdk.core.traits.RequiredTrait;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.core.traits.Trait;
import software.amazon.awssdk.core.traits.XmlAttributeTrait;
import software.amazon.awssdk.core.traits.XmlAttributesTrait;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeBuilder;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.Ec2QueryNameTrait;
import software.amazon.awssdk.schema.traits.MediaTypeTrait;
import software.amazon.awssdk.schema.traits.StringTrait;
import software.amazon.awssdk.schema.traits.XmlNamespaceTrait;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Derives the {@link SdkPojo#sdkFields()} of a generated model class from its Schema (design A.6, OD-14), so that
 * generated classes keep their public {@code sdkFields()} and {@code sdkFieldNameToField()} without per-member
 * {@code SdkField} constants.
 *
 * <p>The derived fields behave like the ones the SDK's previous code generator emitted for the same shape and protocol,
 * so the existing marshallers and unmarshallers keep working on them:
 * <ul>
 *     <li>getter: {@code SerializableStruct.getMemberValue(member)}; setter: {@code ShapeBuilder.setMemberValue(member,
 *     value)}; constructor: the member target's {@link Schema#shapeBuilder()}.</li>
 *     <li>{@code memberName}: the Smithy member name; {@link MarshallingType} from the target's {@link ShapeType}.</li>
 *     <li>{@link LocationTrait}: the HTTP binding of the member when the settings honor bindings, else {@code PAYLOAD};
 *     the location name is the protocol's member name trait ({@link WireNaming}), else the binding's name, else the
 *     member name.</li>
 *     <li>{@link ListTrait}, {@link MapTrait}, {@link PayloadTrait}, {@link JsonValueTrait} (a header with
 *     {@code mediaType: application/json}), {@link DefaultValueTrait#idempotencyToken()}, {@link TimestampFormatTrait},
 *     {@link XmlAttributesTrait} (a target with {@code xmlNamespace}), {@link XmlAttributeTrait} and, when the settings
 *     enable it, {@link RequiredTrait}, all from runtime traits.</li>
 *     <li>Members that target a streaming blob or event stream, and the {@code message} member of an error, are left
 *     out, as they were not fields of the generated classes.</li>
 * </ul>
 *
 * <p>Fields are derived once per schema and cached on the schema ({@link SdkFieldsExtension}). The cache holds the
 * fields of the first settings used for a schema; a call with other settings derives them again without caching. A
 * generated class uses one settings instance, from its service's {@code <Svc>SharedSerde}.
 *
 * <p>Nothing on the schema serde path calls this class: it is the bridge between schemas and the {@code SdkField}
 * world only.
 */
@SdkProtectedApi
@ThreadSafe
public final class SdkFieldsFromSchema {
    private static final String APPLICATION_JSON = "application/json";

    private final WireNaming wireNames;
    private final HttpBindingMode httpBindings;
    private final boolean requiredTraits;

    private SdkFieldsFromSchema(Builder builder) {
        this.wireNames = builder.wireNames;
        this.httpBindings = builder.httpBindings;
        this.requiredTraits = builder.requiredTraits;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the fields of a structure or union schema, in member order.
     *
     * @param schema the root schema of a generated model class
     * @return an unmodifiable list of fields
     */
    public List<SdkField<?>> fields(Schema schema) {
        return derived(schema).fields;
    }

    /**
     * Returns the fields of a structure or union schema by their marshalling location name, as
     * {@link SdkPojo#sdkFieldNameToField()} defines.
     *
     * @param schema the root schema of a generated model class
     * @return an unmodifiable map of fields
     */
    public Map<String, SdkField<?>> fieldsByName(Schema schema) {
        return derived(schema).byName;
    }

    private Derived derived(Schema schema) {
        SdkFieldsExtension.Holder holder = schema.getExtension(SdkFieldsExtension.KEY);
        if (holder == null) {
            // The provider is not registered, for example in a class loader without the services file.
            return derive(schema);
        }
        Object cached = holder.get();
        if (cached instanceof Derived && ((Derived) cached).settings.equals(this)) {
            return (Derived) cached;
        }
        Derived derived = derive(schema);
        if (cached == null) {
            holder.set(derived);
        }
        return derived;
    }

    private Derived derive(Schema schema) {
        if (schema.type() != ShapeType.STRUCTURE && schema.type() != ShapeType.UNION) {
            throw new IllegalArgumentException("SdkFields can only be derived from a structure or union schema, found "
                                               + schema);
        }
        boolean isError = schema.hasTrait(TraitKey.ERROR_TRAIT);
        List<SdkField<?>> fields = new ArrayList<>(schema.members().size());
        Map<String, SdkField<?>> byName = new HashMap<>();
        for (Schema member : schema.members()) {
            if (member.hasTrait(TraitKey.STREAMING_TRAIT) || isError && "message".equalsIgnoreCase(member.memberName())) {
                continue;
            }
            SdkField<?> field = memberField(member);
            fields.add(field);
            byName.put(field.locationName(), field);
        }
        return new Derived(this, Collections.unmodifiableList(fields), Collections.unmodifiableMap(byName));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SdkField<?> memberField(Schema member) {
        SdkField.Builder<Object> field = SdkField.builder((MarshallingType) marshallingType(member));
        field.memberName(member.memberName())
             .getter(new Getter(member))
             .setter(new Setter(member));
        constructor(member, field);
        field.traits(traits(member, false));
        return field.build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SdkField<?> elementField(Schema element) {
        SdkField.Builder<Object> field = SdkField.builder((MarshallingType) marshallingType(element));
        constructor(element, field);
        field.traits(traits(element, true));
        return field.build();
    }

    private static void constructor(Schema member, SdkField.Builder<Object> field) {
        Schema target = member.memberTarget();
        if (target.type() == ShapeType.STRUCTURE || target.type() == ShapeType.UNION) {
            field.constructor(new Constructor(target));
        }
    }

    private Trait[] traits(Schema member, boolean element) {
        List<Trait> traits = new ArrayList<>();
        Schema target = member.memberTarget();
        Location location = location(member, element);
        traits.add(location.trait);
        if (target.type() == ShapeType.LIST) {
            Schema listMember = target.listMember();
            traits.add(ListTrait.builder()
                                .memberLocationName(directValue(listMember, TraitKey.XML_NAME_TRAIT))
                                .memberFieldInfo(elementField(listMember))
                                .isFlattened(member.hasTrait(TraitKey.XML_FLATTENED_TRAIT))
                                .build());
        } else if (target.type() == ShapeType.MAP) {
            Schema key = target.mapKeyMember();
            Schema value = target.mapValueMember();
            traits.add(MapTrait.builder()
                               .keyLocationName(orDefault(directValue(key, TraitKey.XML_NAME_TRAIT), "key"))
                               .valueLocationName(orDefault(directValue(value, TraitKey.XML_NAME_TRAIT), "value"))
                               .valueFieldInfo(elementField(value))
                               .isFlattened(member.hasTrait(TraitKey.XML_FLATTENED_TRAIT))
                               .build());
        }
        if (location.payload || !element && member.hasTrait(TraitKey.EVENT_PAYLOAD_TRAIT)) {
            traits.add(PayloadTrait.create());
        }
        if (location.trait.location() == MarshallLocation.HEADER && isJsonMediaType(member)) {
            traits.add(JsonValueTrait.create());
        }
        if (!element && member.getDirectTrait(TraitKey.IDEMPOTENCY_TOKEN_TRAIT) != null) {
            traits.add(DefaultValueTrait.idempotencyToken());
        }
        software.amazon.awssdk.schema.traits.TimestampFormatTrait timestampFormat =
            member.getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT);
        if (timestampFormat != null) {
            traits.add(TimestampFormatTrait.create(timestampFormat(timestampFormat)));
        }
        XmlNamespaceTrait namespace = target.getTrait(TraitKey.XML_NAMESPACE_TRAIT);
        if (namespace != null) {
            traits.add(xmlAttributes(target, namespace));
        }
        if (!element && member.getDirectTrait(TraitKey.XML_ATTRIBUTE_TRAIT) != null) {
            traits.add(XmlAttributeTrait.create());
        }
        if (requiredTraits && !element && member.getDirectTrait(TraitKey.REQUIRED_TRAIT) != null) {
            traits.add(RequiredTrait.create());
        }
        return traits.toArray(new Trait[0]);
    }

    private Location location(Schema member, boolean element) {
        MarshallLocation location = MarshallLocation.PAYLOAD;
        String bindingName = null;
        boolean payload = false;
        if (!element) {
            if (member.hasTrait(TraitKey.EVENT_HEADER_TRAIT)) {
                location = MarshallLocation.HEADER;
            } else if (httpBindings != HttpBindingMode.NONE) {
                StringTrait header = member.getDirectTrait(TraitKey.HTTP_HEADER_TRAIT);
                StringTrait prefixHeaders = member.getDirectTrait(TraitKey.HTTP_PREFIX_HEADERS_TRAIT);
                if (header != null) {
                    location = MarshallLocation.HEADER;
                    bindingName = header.getValue();
                } else if (prefixHeaders != null) {
                    location = MarshallLocation.HEADER;
                    bindingName = prefixHeaders.getValue();
                } else if (member.getDirectTrait(TraitKey.HTTP_PAYLOAD_TRAIT) != null) {
                    payload = true;
                } else if (httpBindings == HttpBindingMode.REQUEST) {
                    StringTrait query = member.getDirectTrait(TraitKey.HTTP_QUERY_TRAIT);
                    if (query != null) {
                        location = MarshallLocation.QUERY_PARAM;
                        bindingName = query.getValue();
                    } else if (member.getDirectTrait(TraitKey.HTTP_QUERY_PARAMS_TRAIT) != null) {
                        location = MarshallLocation.QUERY_PARAM;
                    } else if (member.getDirectTrait(TraitKey.HTTP_LABEL_TRAIT) != null) {
                        location = MarshallLocation.PATH;
                    }
                } else if (member.getDirectTrait(TraitKey.HTTP_RESPONSE_CODE_TRAIT) != null) {
                    location = MarshallLocation.STATUS_CODE;
                }
            }
        }
        String unmarshallName = traitOrBindingName(member, bindingName);
        String marshallName = wireNames == WireNaming.EC2 ? ec2MarshallName(member, element) : unmarshallName;
        LocationTrait trait = LocationTrait.builder()
                                           .location(location)
                                           .locationName(marshallName)
                                           .unmarshallLocationName(unmarshallName)
                                           .build();
        return new Location(trait, payload);
    }

    private String traitOrBindingName(Schema member, String bindingName) {
        String traitName = null;
        switch (wireNames) {
            case JSON:
                traitName = directValue(member, TraitKey.JSON_NAME_TRAIT);
                break;
            case XML:
            case EC2:
                traitName = directValue(member, TraitKey.XML_NAME_TRAIT);
                break;
            default:
                break;
        }
        if (StringUtils.isNotBlank(traitName)) {
            return traitName;
        }
        if (StringUtils.isNotBlank(bindingName)) {
            return bindingName;
        }
        return member.memberName();
    }

    /**
     * The EC2 query marshalling name: {@code ec2QueryName}, else {@code xmlName} or the member name with its first
     * character upper-cased; a list element, map key or map value keeps its literal name.
     */
    private static String ec2MarshallName(Schema member, boolean element) {
        String ec2QueryName = directValue(member, TraitKey.get(Ec2QueryNameTrait.class));
        if (StringUtils.isNotBlank(ec2QueryName)) {
            return ec2QueryName;
        }
        String xmlName = directValue(member, TraitKey.XML_NAME_TRAIT);
        if (StringUtils.isNotBlank(xmlName)) {
            return StringUtils.upperCase(xmlName.substring(0, 1)) + xmlName.substring(1);
        }
        String name = member.memberName();
        return element ? name : StringUtils.upperCase(name.substring(0, 1)) + name.substring(1);
    }

    private static boolean isJsonMediaType(Schema member) {
        MediaTypeTrait mediaType = member.getTrait(TraitKey.MEDIA_TYPE_TRAIT);
        return mediaType != null && APPLICATION_JSON.equals(mediaType.getValue());
    }

    private static TimestampFormatTrait.Format timestampFormat(
        software.amazon.awssdk.schema.traits.TimestampFormatTrait trait) {
        switch (trait.getValue()) {
            case software.amazon.awssdk.schema.traits.TimestampFormatTrait.DATE_TIME:
                return TimestampFormatTrait.Format.ISO_8601;
            case software.amazon.awssdk.schema.traits.TimestampFormatTrait.EPOCH_SECONDS:
                return TimestampFormatTrait.Format.UNIX_TIMESTAMP;
            case software.amazon.awssdk.schema.traits.TimestampFormatTrait.HTTP_DATE:
                return TimestampFormatTrait.Format.RFC_822;
            default:
                throw new IllegalStateException("Unknown timestamp format " + trait.getValue());
        }
    }

    /**
     * The {@code xmlns:<prefix>} attribute of a target with {@code xmlNamespace}, plus its {@code xmlAttribute}
     * member if it has one, as the previous code generator emitted them.
     */
    @SuppressWarnings("unchecked")
    private XmlAttributesTrait xmlAttributes(Schema target, XmlNamespaceTrait namespace) {
        String uri = namespace.getUri();
        Pair<String, XmlAttributesTrait.AttributeAccessors> xmlns =
            Pair.of("xmlns:" + namespace.getPrefix().orElse(null),
                    XmlAttributesTrait.AttributeAccessors.builder().attributeGetter(ignore -> uri).build());
        for (Schema member : target.members()) {
            if (member.getDirectTrait(TraitKey.XML_ATTRIBUTE_TRAIT) != null) {
                Pair<String, XmlAttributesTrait.AttributeAccessors> attribute =
                    Pair.of(location(member, false).trait.locationName(),
                            XmlAttributesTrait.AttributeAccessors.builder()
                                                                 .attributeGetter(new AttributeGetter(member))
                                                                 .build());
                return XmlAttributesTrait.create(xmlns, attribute);
            }
        }
        return XmlAttributesTrait.create(xmlns);
    }

    private static MarshallingType<?> marshallingType(Schema member) {
        switch (member.type()) {
            case BOOLEAN:
                return MarshallingType.BOOLEAN;
            case BYTE:
                return MarshallingType.BYTE;
            case SHORT:
                return MarshallingType.SHORT;
            case INTEGER:
            case INT_ENUM:
                return MarshallingType.INTEGER;
            case LONG:
                return MarshallingType.LONG;
            case FLOAT:
                return MarshallingType.FLOAT;
            case DOUBLE:
                return MarshallingType.DOUBLE;
            case BIG_DECIMAL:
                return MarshallingType.BIG_DECIMAL;
            case STRING:
            case ENUM:
                return MarshallingType.STRING;
            case BLOB:
                return MarshallingType.SDK_BYTES;
            case TIMESTAMP:
                return MarshallingType.INSTANT;
            case DOCUMENT:
                return MarshallingType.DOCUMENT;
            case STRUCTURE:
            case UNION:
                return MarshallingType.SDK_POJO;
            case LIST:
                return MarshallingType.LIST;
            case MAP:
                return MarshallingType.MAP;
            default:
                throw new IllegalStateException("Member " + member.id() + " of type " + member.type()
                                                + " has no SdkField marshalling type");
        }
    }

    private static <T extends StringTrait> String directValue(Schema member, TraitKey<T> key) {
        T trait = member.getDirectTrait(key);
        return trait == null ? null : trait.getValue();
    }

    private static String orDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SdkFieldsFromSchema)) {
            return false;
        }
        SdkFieldsFromSchema that = (SdkFieldsFromSchema) o;
        return wireNames == that.wireNames && httpBindings == that.httpBindings && requiredTraits == that.requiredTraits;
    }

    @Override
    public int hashCode() {
        int result = wireNames.hashCode();
        result = 31 * result + httpBindings.hashCode();
        return 31 * result + Boolean.hashCode(requiredTraits);
    }

    /**
     * Which member name trait names a member on the wire, as the protocol of the service defines it.
     */
    public enum WireNaming {
        /**
         * Member names only (for example {@code smithy-rpc-v2-cbor}).
         */
        MEMBER,

        /**
         * {@code jsonName}, then the member name ({@code json}, {@code rest-json}).
         */
        JSON,

        /**
         * {@code xmlName}, then the member name ({@code rest-xml}, {@code query}).
         */
        XML,

        /**
         * {@code xmlName} for reading; for writing {@code ec2QueryName}, then {@code xmlName} or the member name with
         * its first character upper-cased ({@code ec2}).
         */
        EC2
    }

    /**
     * Which HTTP binding traits the fields honor: none for RPC protocols and for nested shapes, the request bindings
     * for an operation input of a REST protocol, and the response bindings for an operation output or error of a REST
     * protocol.
     */
    public enum HttpBindingMode {
        NONE,
        REQUEST,
        RESPONSE
    }

    public static final class Builder {
        private WireNaming wireNames = WireNaming.MEMBER;
        private HttpBindingMode httpBindings = HttpBindingMode.NONE;
        private boolean requiredTraits;

        private Builder() {
        }

        public Builder wireNames(WireNaming wireNames) {
            this.wireNames = Objects.requireNonNull(wireNames, "wireNames");
            return this;
        }

        public Builder httpBindings(HttpBindingMode httpBindings) {
            this.httpBindings = Objects.requireNonNull(httpBindings, "httpBindings");
            return this;
        }

        /**
         * @param requiredTraits whether required members get a {@link RequiredTrait}, which the client validates
         *     before marshalling (the {@code requiredTraitValidationEnabled} customization)
         */
        public Builder requiredTraits(boolean requiredTraits) {
            this.requiredTraits = requiredTraits;
            return this;
        }

        public SdkFieldsFromSchema build() {
            return new SdkFieldsFromSchema(this);
        }
    }

    private static final class Derived {
        private final SdkFieldsFromSchema settings;
        private final List<SdkField<?>> fields;
        private final Map<String, SdkField<?>> byName;

        private Derived(SdkFieldsFromSchema settings, List<SdkField<?>> fields, Map<String, SdkField<?>> byName) {
            this.settings = settings;
            this.fields = fields;
            this.byName = byName;
        }
    }

    private static final class Location {
        private final LocationTrait trait;
        private final boolean payload;

        private Location(LocationTrait trait, boolean payload) {
            this.trait = trait;
            this.payload = payload;
        }
    }

    private static final class Getter implements Function<Object, Object> {
        private final Schema member;

        private Getter(Schema member) {
            this.member = member;
        }

        @Override
        public Object apply(Object pojo) {
            return ((SerializableStruct) pojo).getMemberValue(member);
        }
    }

    private static final class Setter implements BiConsumer<Object, Object> {
        private final Schema member;

        private Setter(Schema member) {
            this.member = member;
        }

        @Override
        public void accept(Object builder, Object value) {
            ((ShapeBuilder<?>) builder).setMemberValue(member, value);
        }
    }

    private static final class Constructor implements Supplier<SdkPojo> {
        private final Schema target;

        private Constructor(Schema target) {
            this.target = target;
        }

        @Override
        public SdkPojo get() {
            ShapeBuilder<?> builder = target.shapeBuilder();
            if (builder == null) {
                throw new IllegalStateException("Schema " + target.id() + " has no builder");
            }
            return (SdkPojo) builder;
        }
    }

    private static final class AttributeGetter implements Function<Object, String> {
        private final Schema member;

        private AttributeGetter(Schema member) {
            this.member = member;
        }

        @Override
        public String apply(Object pojo) {
            return ((SerializableStruct) pojo).getMemberValue(member);
        }
    }
}
