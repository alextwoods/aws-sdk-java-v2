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

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait;
import software.amazon.smithy.aws.traits.protocols.AwsJson1_1Trait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryTrait;
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait;
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.protocol.traits.Rpcv2CborTrait;

/**
 * Every generated client selects exactly one protocol declared by the canonical model and supported by the protocol
 * runtime (Requirement 5.6), so the service must apply at least one of the seven launch protocols (Requirement 5.5).
 * Protocols are the traits Smithy's {@link ServiceIndex} recognizes through {@code @protocolDefinition}.
 */
final class ProtocolCheck implements SemanticCheck {

    /** The launch protocols of the V3 protocol runtime. */
    static final SortedSet<ShapeId> SUPPORTED = new TreeSet<>(Set.of(
        AwsJson1_0Trait.ID, AwsJson1_1Trait.ID, RestJson1Trait.ID, RestXmlTrait.ID, AwsQueryTrait.ID,
        Ec2QueryTrait.ID, Rpcv2CborTrait.ID));

    static final String ELEMENT = "protocol trait";

    @Override
    public String id() {
        return "aws.protocol";
    }

    @Override
    public List<Diagnostic> check(Model model, ServiceShape service) {
        SortedSet<ShapeId> declared = new TreeSet<>(ServiceIndex.of(model).getProtocols(service).keySet());
        String id = service.getId().toString();
        if (declared.isEmpty()) {
            return List.of(Diagnostic.forService(
                DiagnosticCode.MISSING_SEMANTIC_ELEMENT, id, ELEMENT,
                "The service applies no protocol trait, so a generated client has no modeled protocol to select. "
                + "Apply one of " + SUPPORTED + " to " + id + "."));
        }
        if (declared.stream().noneMatch(SUPPORTED::contains)) {
            return List.of(Diagnostic.forService(
                DiagnosticCode.UNSUPPORTED_SEMANTIC_ELEMENT, id, ELEMENT,
                "The service applies only protocol traits " + declared + ", none of which the V3 protocol runtime "
                + "supports; supported protocols are " + SUPPORTED + "."));
        }
        return List.of();
    }
}
