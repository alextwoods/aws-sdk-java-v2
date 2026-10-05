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

package software.amazon.awssdk.awscodegen.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import software.amazon.smithy.aws.traits.auth.SigV4ATrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.HttpBearerAuthTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;

/**
 * Every operation must resolve its auth behavior from the canonical model: either at least one auth scheme Modern
 * Auth supports, or explicitly modeled anonymous access ({@code @auth([])} on the operation or service, or
 * {@code @optionalAuth} on the operation). A service that applies no auth scheme and never declares anonymous access
 * has lost its auth semantics (for example a missing {@code @aws.auth#sigv4}); it is reported per operation.
 */
final class AuthCheck implements SemanticCheck {

    /** Auth schemes V3 Modern Auth can sign with. */
    static final SortedSet<ShapeId> SUPPORTED = new TreeSet<>(Set.of(
        SigV4Trait.ID, SigV4ATrait.ID, HttpBearerAuthTrait.ID));

    static final String ELEMENT = "auth scheme";

    @Override
    public String id() {
        return "aws.auth";
    }

    @Override
    public List<Diagnostic> check(Model model, ServiceShape service) {
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        String serviceId = service.getId().toString();
        List<Diagnostic> result = new ArrayList<>();
        for (OperationShape operation : new TreeSet<>(TopDownIndex.of(model).getContainedOperations(service))) {
            SortedSet<ShapeId> schemes =
                new TreeSet<>(serviceIndex.getEffectiveAuthSchemes(service, operation).keySet());
            boolean optional = operation.hasTrait(OptionalAuthTrait.class);
            String operationId = operation.getId().toString();
            if (schemes.isEmpty()) {
                boolean anonymous = operation.hasTrait(AuthTrait.class) || service.hasTrait(AuthTrait.class) || optional;
                if (!anonymous) {
                    result.add(Diagnostic.forOperation(
                        DiagnosticCode.MISSING_SEMANTIC_ELEMENT, serviceId, operationId, ELEMENT,
                        "The operation resolves no auth scheme: neither it nor the service applies an auth scheme "
                        + "trait (for example @aws.auth#sigv4), and neither declares anonymous access with "
                        + "@smithy.api#auth([]) or @smithy.api#optionalAuth."));
                }
            } else if (!optional && schemes.stream().noneMatch(SUPPORTED::contains)) {
                result.add(Diagnostic.forOperation(
                    DiagnosticCode.UNSUPPORTED_SEMANTIC_ELEMENT, serviceId, operationId, ELEMENT,
                    "The operation resolves only auth schemes " + schemes + ", none of which V3 Modern Auth "
                    + "supports; supported auth schemes are " + SUPPORTED + "."));
            }
        }
        return result;
    }
}
