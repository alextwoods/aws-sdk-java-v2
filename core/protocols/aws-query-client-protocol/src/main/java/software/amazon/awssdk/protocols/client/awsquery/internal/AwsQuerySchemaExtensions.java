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

package software.amazon.awssdk.protocols.client.awsquery.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codecs.commons.TimestampFormatter;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;
import software.amazon.awssdk.schema.ShapeType;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.Ec2QueryNameTrait;
import software.amazon.awssdk.schema.traits.TimestampFormatTrait;
import software.amazon.awssdk.schema.traits.XmlNameTrait;
import software.amazon.awssdk.utils.StringUtils;

// CHECKSTYLE:OFF - Keeps the smithy-java (and design Porting_Map) name.
/**
 * Computes, once per member schema, the form keys the AWS Query serializer needs: the percent-encoded member name for
 * {@code awsQuery} and for {@code ec2Query}, the list and map key segments, and the timestamp format.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.aws.client.awsquery.AwsQuerySchemaExtensions}. Registered
 * as a {@link SchemaExtensionProvider} through {@code META-INF/services}. The names come from traits applied directly
 * to the member ({@code xmlName}, {@code ec2QueryName}, {@code xmlFlattened}), as the legacy SDK's {@code locationName}
 * did; the timestamp format includes the target's {@code timestampFormat}.
 */
@SdkInternalApi
public final class AwsQuerySchemaExtensions
    implements SchemaExtensionProvider<AwsQuerySchemaExtensions.QueryMemberBinding> {
    // CHECKSTYLE:ON

    /**
     * Extension key for the query binding of a member schema.
     */
    public static final SchemaExtensionKey<QueryMemberBinding> KEY = new SchemaExtensionKey<>();

    private static final byte[] MEMBER = {'m', 'e', 'm', 'b', 'e', 'r'};
    private static final byte[] ENTRY = {'e', 'n', 't', 'r', 'y'};
    private static final byte[] KEY_NAME = {'k', 'e', 'y'};
    private static final byte[] VALUE_NAME = {'v', 'a', 'l', 'u', 'e'};

    @Override
    public SchemaExtensionKey<QueryMemberBinding> key() {
        return KEY;
    }

    @Override
    public QueryMemberBinding provide(Schema schema) {
        if (!schema.isMember()) {
            return null;
        }
        return compute(schema);
    }

    /**
     * Returns the binding of {@code member}: the registered extension, or one computed now if the provider is not
     * registered (for example on a class path without this module's service file).
     */
    static QueryMemberBinding binding(Schema member) {
        QueryMemberBinding binding = member.getExtension(KEY);
        return binding != null ? binding : compute(member);
    }

    private static QueryMemberBinding compute(Schema member) {
        byte[] awsQueryName = QueryFormSerializer.encodeName(awsQueryName(member));
        byte[] ec2QueryName = QueryFormSerializer.encodeName(ec2QueryName(member));
        boolean flattened = member.getDirectTrait(TraitKey.XML_FLATTENED_TRAIT) != null;

        byte[] listMemberName = MEMBER;
        byte[] mapEntryName = flattened ? null : ENTRY;
        byte[] mapKeyName = KEY_NAME;
        byte[] mapValueName = VALUE_NAME;
        Schema target = member.memberTarget();
        if (target != null && target.type() == ShapeType.LIST) {
            listMemberName = nameOrDefault(target.listMember(), MEMBER);
        } else if (target != null && target.type() == ShapeType.MAP) {
            mapKeyName = nameOrDefault(target.mapKeyMember(), KEY_NAME);
            mapValueName = nameOrDefault(target.mapValueMember(), VALUE_NAME);
        }

        // An unknown format resolves to null here and fails when a value is written, not when the schema is read.
        TimestampFormatTrait timestampFormat = member.getTrait(TraitKey.TIMESTAMP_FORMAT_TRAIT);
        TimestampFormatter timestampFormatter = timestampFormat == null
                                                ? TimestampFormatter.Prelude.DATE_TIME
                                                : TimestampFormatter.match(timestampFormat.getFormat());
        return new QueryMemberBinding(awsQueryName, ec2QueryName, flattened, listMemberName, mapEntryName, mapKeyName,
                                      mapValueName, timestampFormatter);
    }

    private static String awsQueryName(Schema member) {
        XmlNameTrait xmlName = member.getDirectTrait(TraitKey.XML_NAME_TRAIT);
        return xmlName != null ? xmlName.getValue() : member.memberName();
    }

    private static String ec2QueryName(Schema member) {
        Ec2QueryNameTrait ec2Name = member.getDirectTrait(TraitKey.EC2_QUERY_NAME_TRAIT);
        if (ec2Name != null) {
            return ec2Name.getValue();
        }
        XmlNameTrait xmlName = member.getDirectTrait(TraitKey.XML_NAME_TRAIT);
        return StringUtils.capitalize(xmlName != null ? xmlName.getValue() : member.memberName());
    }

    private static byte[] nameOrDefault(Schema collectionMember, byte[] defaultName) {
        XmlNameTrait xmlName = collectionMember == null ? null : collectionMember.getDirectTrait(TraitKey.XML_NAME_TRAIT);
        return xmlName == null ? defaultName : QueryFormSerializer.encodeName(xmlName.getValue());
    }

    /**
     * The pre-computed query keys of a member schema. Immutable; the arrays are never modified after construction and
     * are only read by this package.
     */
    public static final class QueryMemberBinding {
        private final byte[] awsQueryName;
        private final byte[] ec2QueryName;
        private final boolean flattened;
        private final byte[] listMemberName;
        private final byte[] mapEntryName;
        private final byte[] mapKeyName;
        private final byte[] mapValueName;
        private final TimestampFormatter timestampFormatter;

        QueryMemberBinding(byte[] awsQueryName, byte[] ec2QueryName, boolean flattened, byte[] listMemberName,
                           byte[] mapEntryName, byte[] mapKeyName, byte[] mapValueName,
                           TimestampFormatter timestampFormatter) {
            this.awsQueryName = awsQueryName;
            this.ec2QueryName = ec2QueryName;
            this.flattened = flattened;
            this.listMemberName = listMemberName;
            this.mapEntryName = mapEntryName;
            this.mapKeyName = mapKeyName;
            this.mapValueName = mapValueName;
            this.timestampFormatter = timestampFormatter;
        }

        /**
         * The percent-encoded key of this member for {@code awsQuery} or {@code ec2Query}.
         */
        byte[] name(boolean ec2) {
            return ec2 ? ec2QueryName : awsQueryName;
        }

        /**
         * Whether the member has {@code xmlFlattened}, so a list writes {@code Name.N} and a map {@code Name.N.key}.
         */
        boolean flattened() {
            return flattened;
        }

        /**
         * The element segment of a non-flattened list: the list member's {@code xmlName}, or {@code member}.
         */
        byte[] listMemberName() {
            return listMemberName;
        }

        /**
         * The entry segment of a map, {@code entry}; null for a flattened map.
         */
        byte[] mapEntryName() {
            return mapEntryName;
        }

        /**
         * The key segment of a map entry: the key member's {@code xmlName}, or {@code key}.
         */
        byte[] mapKeyName() {
            return mapKeyName;
        }

        /**
         * The value segment of a map entry: the value member's {@code xmlName}, or {@code value}.
         */
        byte[] mapValueName() {
            return mapValueName;
        }

        /**
         * The formatter for this member's {@code timestampFormat} (member or target), else {@code date-time}; null for
         * a format this SDK does not know.
         */
        TimestampFormatter timestampFormatter() {
            return timestampFormatter;
        }
    }
}
