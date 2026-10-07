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

package software.amazon.awssdk.protocols.smithyjava;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.java.internal.shaded.tools.jackson.core.StreamReadConstraints;

/**
 * Brings smithy-java's JSON parsing limits in line with v2's.
 *
 * <p>smithy-java's default JSON provider is a shaded Jackson 3 whose factory is built once, in a static
 * initializer, with Jackson's default {@link StreamReadConstraints}. Those cap an object key at 50,000
 * characters. DynamoDB attribute names can be 65,535 bytes, and v2's own factory
 * ({@code AwsStructuredPlainJsonFactory}) raises the cap to exactly that; {@code LongAttributeNameTest}
 * holds it there. smithy-java offers no per-codec setting, so the only lever is Jackson's process-wide
 * default, which has to be in place before the provider's factory exists. It affects nothing but
 * smithy-java's shaded copy of Jackson, and only raises a limit.
 */
@SdkInternalApi
final class SmithyJsonDefaults {

    private static final int MAX_NAME_LENGTH = 65_535;

    private SmithyJsonDefaults() {
    }

    static void apply() {
        StreamReadConstraints current = StreamReadConstraints.defaults();
        if (current.getMaxNameLength() < MAX_NAME_LENGTH) {
            StreamReadConstraints.overrideDefaultStreamReadConstraints(
                current.rebuild().maxNameLength(MAX_NAME_LENGTH).build());
        }
    }
}
