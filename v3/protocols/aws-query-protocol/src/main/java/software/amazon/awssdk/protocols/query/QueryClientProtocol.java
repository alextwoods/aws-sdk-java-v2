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

package software.amazon.awssdk.protocols.query;

import java.util.Objects;
import software.amazon.awssdk.protocols.core.ClientProtocol;
import software.amazon.awssdk.protocols.core.ModeledProtocol;
import software.amazon.awssdk.protocols.core.OperationProtocol;
import software.amazon.awssdk.protocols.core.ProtocolContext;
import software.amazon.awssdk.protocols.core.ProtocolSelectionException;
import software.amazon.awssdk.protocols.core.ServiceProtocols;
import software.amazon.awssdk.protocols.core.error.ModeledErrors;
import software.amazon.awssdk.smithy.runtime.schema.OperationSchemas;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * The per-client runtime of a query protocol ({@code awsQuery} or {@code ec2Query}). Wire rules:
 * <ul>
 *   <li>every request is {@code POST /} with {@code Content-Type: application/x-www-form-urlencoded} and the form
 *       body of {@link QueryFormSerializer} ({@code Action=<operation shape name>&Version=<service version>&...});
 *       HTTP binding traits are ignored;</li>
 *   <li>a successful response is XML read by {@link software.amazon.awssdk.protocols.core.xml.XmlShapeDeserializer}
 *       from {@code <OpResponse><OpResult>...} ({@link ResultWrapping#OPERATION_RESULT}, {@code awsQuery}) or
 *       {@code <OpResponse>...} ({@link ResultWrapping#NONE}, {@code ec2Query}); an empty body or a missing
 *       result element is an empty output;</li>
 *   <li>an error response is an {@link software.amazon.awssdk.protocols.core.xml.XmlErrorEnvelope}; its
 *       {@code Code} resolves the modeled error through the {@code awsQueryError} codes, then shape names, and its
 *       {@code <Error>} element holds the error's members; the envelope's request ID wins over the headers'.</li>
 * </ul>
 *
 * <p>Immutable and thread safe.
 */
public final class QueryClientProtocol implements ClientProtocol {
    /** Where a successful response holds the output members. */
    public enum ResultWrapping {
        /** {@code <OpResponse><OpResult>members</OpResult></OpResponse>} ({@code awsQuery}). */
        OPERATION_RESULT,
        /** {@code <OpResponse>members</OpResponse>} ({@code ec2Query}). */
        NONE
    }

    private final ProtocolContext context;
    private final QueryFormRules rules;
    private final ResultWrapping wrapping;
    private final ModeledErrors errors;

    private QueryClientProtocol(ProtocolContext context, QueryFormRules rules, ResultWrapping wrapping) {
        this.context = Objects.requireNonNull(context, "context");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.wrapping = Objects.requireNonNull(wrapping, "wrapping");
        this.errors = ModeledErrors.of(context);
    }

    /**
     * The runtime of the client described by {@code context}.
     *
     * @param rules the protocol's form-key rules
     * @param wrapping the protocol's success-response wrapping
     */
    public static QueryClientProtocol create(ProtocolContext context, QueryFormRules rules, ResultWrapping wrapping) {
        return new QueryClientProtocol(context, rules, wrapping);
    }

    /**
     * Rejects, at client construction, a service without a modeled version, which every query request must send.
     *
     * @throws ProtocolSelectionException if {@code service} has no version
     */
    public static void requireServiceVersion(ServiceProtocols service, ModeledProtocol declaration) {
        if (service.serviceVersion().isEmpty()) {
            throw ProtocolSelectionException.builder()
                                            .reason(ProtocolSelectionException.Reason.UNSUPPORTED_BY_RUNTIME)
                                            .serviceId(service.serviceId())
                                            .requested(declaration.id())
                                            .declared(service.protocolIds())
                                            .message("The " + declaration.id() + " runtime requires the service "
                                                     + "version of " + service.serviceId() + ", which its protocol "
                                                     + "data does not record")
                                            .build();
        }
    }

    @Override
    public ShapeId protocolId() {
        return context.selected().protocolId();
    }

    @Override
    public OperationProtocol operation(OperationSchemas operation) {
        return new QueryOperationProtocol(this, operation);
    }

    ProtocolContext context() {
        return context;
    }

    QueryFormRules rules() {
        return rules;
    }

    ResultWrapping wrapping() {
        return wrapping;
    }

    ModeledErrors errors() {
        return errors;
    }

    @Override
    public String toString() {
        return "QueryClientProtocol[" + protocolId() + ", " + context.service().serviceId() + "]";
    }
}
