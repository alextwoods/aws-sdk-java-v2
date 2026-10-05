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

package software.amazon.awssdk.awscodegen;

import java.util.Objects;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Generator configuration of one {@link AwsCodegen} invocation, shared by the directed generator and every
 * {@link AwsCodegenIntegration}.
 *
 * <p>Settings hold configuration only. Every semantic fact about the service is read from the Smithy {@code Model}
 * available on {@link AwsCodegenContext}; settings never restate model content.
 *
 * @param service absolute shape ID of the service being generated
 * @param packageName root Java package of the generated service sources
 */
public record AwsCodegenSettings(ShapeId service, String packageName) {

    public AwsCodegenSettings {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(packageName, "packageName");
        if (service.hasMember()) {
            throw new AwsCodegenException("Service '" + service + "' must be a shape ID, not a member ID.");
        }
        AwsCodegenRequest.requireJavaPackage(packageName);
    }

    /** Settings for {@code request}, whose shape ID and package were validated when it was constructed. */
    static AwsCodegenSettings from(AwsCodegenRequest request) {
        return new AwsCodegenSettings(ShapeId.from(request.serviceShapeId()), request.packageName());
    }
}
