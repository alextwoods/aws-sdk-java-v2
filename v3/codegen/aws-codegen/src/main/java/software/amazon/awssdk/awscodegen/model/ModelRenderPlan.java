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

/**
 * Render plan of one generated model-type file (the model package's counterpart of the generator's
 * {@code RenderPlan}): computed from the model for exactly one file, holding only symbols, shape IDs, names,
 * primitives and pre-rendered Java code, and discarded after rendering. {@code RenderPlanRulesTest} enforces this for
 * every permitted plan.
 */
sealed interface ModelRenderPlan permits ModelTypePlan, EnumTypePlan, ServiceExceptionPlan {
}
