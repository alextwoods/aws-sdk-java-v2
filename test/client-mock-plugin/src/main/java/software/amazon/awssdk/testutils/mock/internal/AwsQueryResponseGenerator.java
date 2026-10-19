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

package software.amazon.awssdk.testutils.mock.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.schema.traits.AwsQueryTrait;

/**
 * Generates {@code aws.protocols#awsQuery} responses.
 */
@SdkInternalApi
@ThreadSafe
public final class AwsQueryResponseGenerator extends QueryResponseGenerator {
    /**
     * Creates the generator; called by {@link java.util.ServiceLoader}.
     */
    public AwsQueryResponseGenerator() {
        super(AwsQueryTrait.ID, false);
    }
}
