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

import java.util.Collections;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A modeled Smithy resource.
 *
 * <p>Port of smithy-java {@code software.amazon.smithy.java.core.schema.ApiResource}.
 */
@SdkPublicApi
public interface ApiResource {
    /**
     * Returns the schema of the resource.
     *
     * @return the resource schema, including its runtime traits
     */
    Schema schema();

    /**
     * Returns the schemas of the resource's identifiers, by identifier name.
     *
     * @return an unmodifiable map of identifier names to schemas; empty by default
     */
    default Map<String, Schema> identifiers() {
        return Collections.emptyMap();
    }

    /**
     * Returns the schemas of the resource's properties, by property name.
     *
     * @return an unmodifiable map of property names to schemas; empty by default
     */
    default Map<String, Schema> properties() {
        return Collections.emptyMap();
    }

    /**
     * Returns the service at the root of the resource tree.
     *
     * @return the service, or null if it is not known
     */
    default ApiService service() {
        return null;
    }

    /**
     * Returns the resource this resource is bound to. A resource is bound to at most one parent resource.
     *
     * @return the parent resource, or null if the resource is bound directly to the service
     */
    default ApiResource boundResource() {
        return null;
    }
}
