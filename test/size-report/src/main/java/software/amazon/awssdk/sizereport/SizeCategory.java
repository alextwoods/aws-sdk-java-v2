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

package software.amazon.awssdk.sizereport;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * The seven categories of Requirement 36.2 that a service's change in uncompressed class-file bytes is broken into.
 * {@link ClassAttributor} documents which bytes go to which category.
 */
@SdkInternalApi
enum SizeCategory {
    SCHEMAS("Schemas"),
    TYPE_REGISTRY("Type_Registries"),
    GENERATED_MODEL_CLASS("Generated_Model_Class classes"),
    CLIENT("Client classes"),
    MARSHALLER("Removed marshallers and unmarshallers"),
    EXCEPTION_MAPPING("Removed exception-mapping code"),
    OTHER("Other");

    private final String label;

    SizeCategory(String label) {
        this.label = label;
    }

    /**
     * The category name used in the report.
     */
    public String label() {
        return label;
    }
}
