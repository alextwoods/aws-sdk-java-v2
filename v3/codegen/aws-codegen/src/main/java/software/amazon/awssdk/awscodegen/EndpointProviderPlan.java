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

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Render plan of the public {@code <root>.endpoints.<Service>EndpointProvider} interface, the V2-compatible entry
 * point for resolving the service's endpoint from typed parameters.
 *
 * @param provider symbol of the generated interface
 * @param params symbol of the service's endpoint parameters class
 * @param rules symbol of the service's endpoint rules class, which holds the default implementation
 * @param service absolute shape ID of the service
 */
record EndpointProviderPlan(Symbol provider, Symbol params, Symbol rules, ShapeId service) implements RenderPlan {

    static EndpointProviderPlan of(AwsCodegenContext context) {
        return new EndpointProviderPlan(EndpointRulesGenerator.providerSymbol(context),
                                        EndpointRulesGenerator.paramsSymbol(context),
                                        EndpointRulesGenerator.rulesSymbol(context), context.service().getId());
    }
}
