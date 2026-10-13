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

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A modeled Smithy service. Each generated service has one instance, which its {@link ApiOperation}s return from
 * {@link ApiOperation#service()}.
 *
 * <p>The interface has one abstract method, so generated code implements it with a lambda that returns the
 * generated service schema.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.ApiService}.
 */
@SdkPublicApi
public interface ApiService {
    /**
     * Returns the schema of the service.
     *
     * @return the service schema, including the shape ID and the service's runtime traits
     */
    Schema schema();

    /**
     * Returns the name of the service.
     *
     * @return the shape name of the service's shape ID, for example {@code AmazonSQS}
     */
    default String name() {
        return schema().id().getName();
    }
}
