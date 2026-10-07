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

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Render plan of the service's base response {@code <Service>Response}, which every operation output extends.
 *
 * @param type symbol of the base response
 * @param metadata symbol of the service's metadata view, {@code <Service>ResponseMetadata}
 * @param service absolute ID of the generated service
 */
record ServiceResponsePlan(Symbol type, Symbol metadata, ShapeId service) implements ModelRenderPlan {
}
