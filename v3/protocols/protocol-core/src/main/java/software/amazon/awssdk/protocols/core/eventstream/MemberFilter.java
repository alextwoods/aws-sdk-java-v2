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

package software.amazon.awssdk.protocols.core.eventstream;

import java.util.function.Predicate;
import software.amazon.smithy.java.core.schema.Schema;
import software.amazon.smithy.java.core.serde.InterceptingSerializer;
import software.amazon.smithy.java.core.serde.ShapeSerializer;

/** Forwards the members {@code include} accepts to the delegate and drops the rest. */
final class MemberFilter extends InterceptingSerializer {
    private final ShapeSerializer delegate;
    private final Predicate<Schema> include;

    MemberFilter(ShapeSerializer delegate, Predicate<Schema> include) {
        this.delegate = delegate;
        this.include = include;
    }

    @Override
    protected ShapeSerializer before(Schema schema) {
        return include.test(schema) ? delegate : NoOpSerializer.INSTANCE;
    }
}
