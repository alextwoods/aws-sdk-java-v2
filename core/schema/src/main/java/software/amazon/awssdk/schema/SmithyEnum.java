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
 * A string enum value.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.SmithyEnum}, with {@link #sdkSchema()} added so
 * that the enum exposes its schema (Requirement 6.9).
 */
@SdkPublicApi
public interface SmithyEnum {
    /**
     * Returns the wire value of the enum.
     *
     * @return the wire value, or null for a value that is unknown to this version of the SDK and has no wire value
     */
    String getValue();

    /**
     * Returns the schema of the enum shape.
     *
     * @return the schema of the enum shape
     */
    Schema sdkSchema();
}
