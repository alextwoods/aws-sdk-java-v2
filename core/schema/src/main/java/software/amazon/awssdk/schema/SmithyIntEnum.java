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
 * An int enum value.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.SmithyIntEnum}, with {@link #sdkSchema()} added
 * so that the enum exposes its schema (Requirement 6.9).
 */
@SdkPublicApi
public interface SmithyIntEnum {
    /**
     * Returns the wire value of the enum.
     *
     * @return the wire value
     */
    int getValue();

    /**
     * Returns the schema of the int enum shape.
     *
     * @return the schema of the int enum shape
     */
    Schema sdkSchema();
}
