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

package software.amazon.awssdk.protocols.ec2query;

import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.awssdk.protocols.query.QueryFormRules;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.schema.TraitKey;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.XmlNameTrait;

/**
 * The {@code ec2Query} form-key rules: a member's key is its {@code aws.protocols#ec2QueryName}, else its own
 * {@code xmlName} with the first letter upper-cased, else its member name with the first letter upper-cased; every
 * list and map is flattened ({@code Key.N}, and the list member's {@code xmlName} is ignored); empty lists are
 * omitted.
 *
 * <p>{@code ec2QueryName} is a build-time-only AWS trait, so its values come from the service's
 * {@link ServiceProtocols#ec2QueryNames()}, keyed by member shape ID. Immutable and thread safe.
 */
public final class Ec2QueryFormRules implements QueryFormRules {
    private final Map<ShapeId, String> ec2QueryNames;

    private Ec2QueryFormRules(Map<ShapeId, String> ec2QueryNames) {
        this.ec2QueryNames = ec2QueryNames;
    }

    /** The rules of {@code service}, with its recorded {@code ec2QueryName} values. */
    public static Ec2QueryFormRules of(ServiceProtocols service) {
        return new Ec2QueryFormRules(Objects.requireNonNull(service, "service").ec2QueryNames());
    }

    @Override
    public String memberKey(Schema member) {
        String name = ec2QueryNames.get(member.id());
        if (name != null) {
            return name;
        }
        XmlNameTrait xmlName = member.getDirectTrait(TraitKey.XML_NAME_TRAIT);
        return capitalize(xmlName != null ? xmlName.getValue() : member.memberName());
    }

    @Override
    public boolean flattened(Schema member) {
        return true;
    }

    @Override
    public boolean writesEmptyLists() {
        return false;
    }

    static String capitalize(String name) {
        if (name.isEmpty() || Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    public String toString() {
        return "QueryFormRules[ec2Query]";
    }
}
