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

package software.amazon.awssdk.awscodegen.model;

import java.util.List;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Render plan of the service's V2-compatible response metadata view {@code <Service>ResponseMetadata}.
 *
 * @param type symbol of the metadata view
 * @param service absolute ID of the generated service
 * @param accessors V2's custom metadata accessors of the service ({@code customResponseMetadata}), in V2's order
 */
record ServiceResponseMetadataPlan(Symbol type, ShapeId service, List<Accessor> accessors)
    implements ModelRenderPlan {
    ServiceResponseMetadataPlan {
        accessors = List.copyOf(accessors);
    }

    /**
     * One custom metadata accessor.
     *
     * @param constant name of the constant holding the metadata key, for example {@code EXTENDED_REQUEST_ID}
     * @param header the metadata key: the response header the accessor reads
     * @param method the accessor name, for example {@code extendedRequestId}
     */
    record Accessor(String constant, String header, String method) {
    }
}
