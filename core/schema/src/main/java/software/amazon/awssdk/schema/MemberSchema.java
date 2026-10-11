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

package software.amazon.awssdk.schema;

import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A member schema whose target schema is built. Member queries delegate to the target.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.MemberSchema}.
 */
@SdkInternalApi
final class MemberSchema extends Schema {
    private final Schema target;

    MemberSchema(MemberSchemaBuilder builder) {
        super(builder);
        this.target = builder.target;
    }

    @Override
    public Schema memberTarget() {
        return target;
    }

    @Override
    SchemaBuilder.Members memberData() {
        return target.memberData();
    }

    @Override
    public Set<String> stringEnumValues() {
        return target.stringEnumValues();
    }

    @Override
    public Set<Integer> intEnumValues() {
        return target.intEnumValues();
    }
}
