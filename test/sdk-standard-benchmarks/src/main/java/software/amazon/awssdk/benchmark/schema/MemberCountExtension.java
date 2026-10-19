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

package software.amazon.awssdk.benchmark.schema;

import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SchemaExtensionKey;
import software.amazon.awssdk.schema.SchemaExtensionProvider;

/**
 * A schema extension used by {@link SchemaExtensionBenchmark}: the number of members of a schema. Registered in
 * {@code META-INF/services/software.amazon.awssdk.schema.SchemaExtensionProvider}.
 */
public final class MemberCountExtension implements SchemaExtensionProvider<MemberCountExtension.MemberCount> {
    public static final SchemaExtensionKey<MemberCount> KEY = new SchemaExtensionKey<>();

    @Override
    public SchemaExtensionKey<MemberCount> key() {
        return KEY;
    }

    @Override
    public MemberCount provide(Schema schema) {
        return new MemberCount(schema.members().size());
    }

    /**
     * The extension value.
     */
    public static final class MemberCount {
        private final int count;

        MemberCount(int count) {
            this.count = count;
        }

        public int count() {
            return count;
        }
    }
}
