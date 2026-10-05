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

package software.amazon.awssdk.protocols.query;

import software.amazon.awssdk.protocols.core.xml.XmlShapeDeserializer;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;

/**
 * The key-naming rules in which the query protocols' form bodies differ ({@link QueryFormSerializer}).
 * {@link #AWS_QUERY} implements {@code awsQuery}; {@code ec2-query-protocol} implements {@code ec2Query}.
 *
 * <p>Implementations are immutable and thread safe.
 */
public interface QueryFormRules {
    /**
     * {@code awsQuery}: a member's key is its own {@code xmlName} else its member name; lists and maps are flattened
     * only with {@code xmlFlattened}; an empty list is sent as {@code Key=}.
     */
    QueryFormRules AWS_QUERY = new QueryFormRules() {
        @Override
        public String memberKey(Schema member) {
            return XmlShapeDeserializer.memberName(member);
        }

        @Override
        public boolean flattened(Schema member) {
            return member.hasTrait(TraitKey.XML_FLATTENED_TRAIT);
        }

        @Override
        public boolean writesEmptyLists() {
            return true;
        }

        @Override
        public String toString() {
            return "QueryFormRules[awsQuery]";
        }
    };

    /** The key segment of a structure or union member. */
    String memberKey(Schema member);

    /**
     * True if the list or map value of {@code member} (a structure, list or map-value member) is flattened: its
     * items are keyed {@code Key.N} rather than {@code Key.member.N}, and its entries {@code Key.N.key} rather than
     * {@code Key.entry.N.key}.
     */
    boolean flattened(Schema member);

    /** True if an empty list is sent as {@code Key=}; false if it is omitted. Empty maps are always omitted. */
    boolean writesEmptyLists();
}
