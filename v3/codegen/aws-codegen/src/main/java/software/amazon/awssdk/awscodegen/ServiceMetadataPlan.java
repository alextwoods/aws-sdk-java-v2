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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import software.amazon.awssdk.awscodegen.schema.SchemaSymbols;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.aws.traits.protocols.AwsProtocolTrait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryCompatibleTrait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryErrorTrait;
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryNameTrait;
import software.amazon.smithy.aws.traits.protocols.Ec2QueryTrait;
import software.amazon.smithy.aws.traits.protocols.RestXmlTrait;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.protocol.traits.Rpcv2CborTrait;

/**
 * Render plan of the public {@code <Service>ServiceMetadata} class.
 *
 * @param metadata symbol of the generated class
 * @param serviceInfo symbol of the package-private generation identity type, which reads the provenance
 * @param service absolute shape ID of the service
 * @param sdkId the {@code aws.api#service} SDK ID, or empty if the service has none
 * @param version the modeled service version, or empty if the service has none
 * @param protocols the service's protocol trait IDs, sorted
 * @param protocolDeclarations the service's protocol traits with their runtime options, sorted by trait ID
 * @param awsQueryCompatible whether the service has {@code aws.protocols#awsQueryCompatible}
 * @param awsQueryErrors the {@code aws.protocols#awsQueryError} codes of the service's errors, sorted by error ID
 * @param queryVersion the service version recorded for the query protocols ({@code Version=}); empty unless the
 *        service declares {@code awsQuery} or {@code ec2Query}, so other services' metadata is unchanged
 * @param ec2QueryNames the {@code aws.protocols#ec2QueryName} values of the members of the service closure, sorted by
 *        member ID; empty unless the service declares {@code ec2Query}
 * @param authSchemes the service's effective auth scheme IDs, in the modeled priority order
 * @param schemaHolder the service's schema holder
 * @param serviceSchemaField the service schema constant in the holder
 * @param operationTable the service's {@code <Service>Operations} table
 * @param typeRegistry the holder of the service's primary type registry
 */
record ServiceMetadataPlan(Symbol metadata, Symbol serviceInfo, ShapeId service, String sdkId, String version,
                           List<ShapeId> protocols, List<ProtocolDeclaration> protocolDeclarations,
                           boolean awsQueryCompatible, List<AwsQueryError> awsQueryErrors,
                           String queryVersion, List<Ec2QueryName> ec2QueryNames,
                           List<ShapeId> authSchemes, Symbol schemaHolder,
                           String serviceSchemaField, Symbol operationTable, Symbol typeRegistry)
    implements RenderPlan {

    ServiceMetadataPlan {
        protocols = List.copyOf(protocols);
        protocolDeclarations = List.copyOf(protocolDeclarations);
        awsQueryErrors = List.copyOf(awsQueryErrors);
        ec2QueryNames = List.copyOf(ec2QueryNames);
        authSchemes = List.copyOf(authSchemes);
    }

    /**
     * One protocol trait of the service as the runtime {@code ModeledProtocol} records it (AWS protocol trait classes
     * are build-time only, so the options are rendered as values).
     *
     * @param id the protocol trait ID
     * @param http the trait's {@code http} list, empty if absent or the trait has none
     * @param eventStreamHttp the trait's {@code eventStreamHttp} list, empty if absent or the trait has none
     * @param noErrorWrapping the {@code restXml} {@code noErrorWrapping} flag
     */
    record ProtocolDeclaration(ShapeId id, List<String> http, List<String> eventStreamHttp, boolean noErrorWrapping) {
        ProtocolDeclaration {
            http = List.copyOf(http);
            eventStreamHttp = List.copyOf(eventStreamHttp);
        }
    }

    /**
     * The {@code aws.protocols#awsQueryError} trait of one error of the service closure.
     *
     * @param error the error structure ID
     * @param code the wire error code
     * @param httpResponseCode the HTTP status code
     */
    record AwsQueryError(ShapeId error, String code, int httpResponseCode) {
    }

    /**
     * The {@code aws.protocols#ec2QueryName} trait of one member of the service closure.
     *
     * @param member the member shape ID
     * @param name the query key of the member
     */
    record Ec2QueryName(ShapeId member, String name) {
    }

    static ServiceMetadataPlan of(AwsCodegenContext context) {
        ServiceShape service = context.service();
        AwsCodegenSettings settings = context.settings();
        ServiceIndex index = ServiceIndex.of(context.model());
        Symbol schemaHolder = AwsSymbolProvider.schemaHolder(service, settings);
        return new ServiceMetadataPlan(
            AwsSymbolProvider.serviceMetadata(service, settings),
            context.symbolProvider().toSymbol(service),
            service.getId(),
            service.getTrait(ServiceTrait.class).map(ServiceTrait::getSdkId).orElse(""),
            service.getVersion(),
            CodegenOrdering.shapeIds(index.getProtocols(service).values()),
            protocolDeclarations(index.getProtocols(service)),
            service.hasTrait(AwsQueryCompatibleTrait.class),
            awsQueryErrors(context.model(), service),
            service.hasTrait(AwsQueryTrait.class) || service.hasTrait(Ec2QueryTrait.class) ? service.getVersion() : "",
            service.hasTrait(Ec2QueryTrait.class) ? ec2QueryNames(context.model(), service) : List.of(),
            List.copyOf(index.getEffectiveAuthSchemes(service).keySet()),
            schemaHolder,
            SchemaSymbols.exported(service.getId(), service, schemaHolder).field(),
            AwsSymbolProvider.operationTable(service, settings),
            AwsSymbolProvider.typeRegistry(service, settings));
    }

    private static List<ProtocolDeclaration> protocolDeclarations(Map<ShapeId, Trait> protocols) {
        List<ProtocolDeclaration> out = new ArrayList<>();
        for (Map.Entry<ShapeId, Trait> entry : CodegenOrdering.sorted(protocols).entrySet()) {
            Trait trait = entry.getValue();
            List<String> http = List.of();
            List<String> eventStreamHttp = List.of();
            if (trait instanceof AwsProtocolTrait aws) {
                http = aws.getHttp();
                eventStreamHttp = aws.getEventStreamHttp();
            } else if (trait instanceof Rpcv2CborTrait cbor) {
                http = strings(cbor.getHttp());
                eventStreamHttp = strings(cbor.getEventStreamHttp());
            }
            boolean noErrorWrapping = trait instanceof RestXmlTrait restXml && restXml.isNoErrorWrapping();
            out.add(new ProtocolDeclaration(entry.getKey(), http, eventStreamHttp, noErrorWrapping));
        }
        return out;
    }

    /** The awsQueryError codes of every error an operation of the service can return (including service errors). */
    private static List<AwsQueryError> awsQueryErrors(Model model, ServiceShape service) {
        OperationIndex operations = OperationIndex.of(model);
        SortedMap<ShapeId, AwsQueryError> errors = new TreeMap<>();
        for (StructureShape error : operations.getErrors(service)) {
            addAwsQueryError(errors, error);
        }
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            for (StructureShape error : operations.getErrors(service, operation)) {
                addAwsQueryError(errors, error);
            }
        }
        return List.copyOf(errors.values());
    }

    private static void addAwsQueryError(SortedMap<ShapeId, AwsQueryError> errors, StructureShape error) {
        error.getTrait(AwsQueryErrorTrait.class).ifPresent(trait -> errors.putIfAbsent(
            error.getId(), new AwsQueryError(error.getId(), trait.getCode(), trait.getHttpResponseCode())));
    }

    /** The ec2QueryName traits of every member in the service closure, sorted by member ID. */
    private static List<Ec2QueryName> ec2QueryNames(Model model, ServiceShape service) {
        SortedMap<ShapeId, Ec2QueryName> names = new TreeMap<>();
        for (MemberShape member : new Walker(model).walkShapes(service).stream()
                                                  .flatMap(shape -> shape.asMemberShape().stream()).toList()) {
            member.getTrait(Ec2QueryNameTrait.class).ifPresent(
                trait -> names.put(member.getId(), new Ec2QueryName(member.getId(), trait.getValue())));
        }
        return List.copyOf(names.values());
    }

    /** {@code Rpcv2CborTrait} exposes its ALPN lists as raw {@code List}s of strings. */
    private static List<String> strings(List<?> values) {
        return values.stream().map(String.class::cast).toList();
    }
}
