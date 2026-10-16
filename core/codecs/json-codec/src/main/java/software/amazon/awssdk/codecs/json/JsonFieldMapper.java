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

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.codecs.json.internal.JsonSchemaExtensions;
import software.amazon.awssdk.schema.MemberLookup;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.JsonNameTrait;

/**
 * Maps members to JSON field names and back. The two implementations are {@link UseMemberName} and
 * {@link UseJsonNameTrait}; {@link JsonSettings#fieldMapper()} returns the one the settings select.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.json.JsonFieldMapper}. The source is a {@code sealed}
 * interface; here the implementations have private constructors and are reached through their {@code INSTANCE}
 * constants.
 */
@SdkPublicApi
public interface JsonFieldMapper {

    /**
     * Returns a lookup that resolves serialized field names into the members of {@code container}.
     *
     * @param container the structure or union (or a member targeting one)
     * @return the member lookup
     */
    MemberLookup fieldToMember(Schema container);

    /**
     * Returns the JSON field name of a member.
     *
     * @param member the member schema
     * @return the field name
     */
    String memberToField(Schema member);

    /**
     * Uses the member name and ignores the {@code jsonName} trait.
     */
    final class UseMemberName implements JsonFieldMapper {
        /**
         * The single instance.
         */
        public static final UseMemberName INSTANCE = new UseMemberName();

        private UseMemberName() {
        }

        @Override
        public MemberLookup fieldToMember(Schema container) {
            return container;
        }

        @Override
        public String memberToField(Schema member) {
            return member.memberName();
        }

        @Override
        public String toString() {
            return "FieldMapper{useJsonName=false}";
        }
    }

    /**
     * Uses the {@code jsonName} trait if present, otherwise the member name.
     */
    final class UseJsonNameTrait implements JsonFieldMapper {
        /**
         * The single instance.
         */
        public static final UseJsonNameTrait INSTANCE = new UseJsonNameTrait();

        private UseJsonNameTrait() {
        }

        @Override
        public MemberLookup fieldToMember(Schema container) {
            // The container may be a member schema; resolve it to its target.
            Schema schema = container.isMember() ? container.memberTarget() : container;
            return schema.getExtension(JsonSchemaExtensions.KEY).jsonMemberLookup();
        }

        @Override
        public String memberToField(Schema member) {
            JsonNameTrait jsonName = member.getTrait(TraitKey.JSON_NAME_TRAIT);
            return jsonName == null ? member.memberName() : jsonName.getValue();
        }

        @Override
        public String toString() {
            return "FieldMapper{useJsonName=true}";
        }
    }
}
