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

package software.amazon.awssdk.protocols.core.binding;

import java.util.Objects;
import software.amazon.smithy.java.core.schema.Schema;

/**
 * The HTTP location of one structure member.
 *
 * @param member the member schema
 * @param location where the member is bound
 * @param name the wire name: the label name for {@link HttpBindingLocation#LABEL}, the parameter name for
 *        {@link HttpBindingLocation#QUERY}, the header name for {@link HttpBindingLocation#HEADER} and the header
 *        prefix for {@link HttpBindingLocation#PREFIX_HEADERS}; empty for the other locations, whose wire name (if
 *        any) is chosen by the protocol's codec
 */
public record HttpMemberBinding(Schema member, HttpBindingLocation location, String name) {

    public HttpMemberBinding {
        Objects.requireNonNull(member, "member");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(name, "name");
        if (!member.isMember()) {
            throw new IllegalArgumentException("Not a member schema: " + member.id());
        }
    }

    /** The member name. */
    public String memberName() {
        return member.memberName();
    }
}
