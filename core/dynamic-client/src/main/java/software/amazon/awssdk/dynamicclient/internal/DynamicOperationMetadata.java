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

package software.amazon.awssdk.dynamicclient.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.client.handler.ClientExecutionParams;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksum;
import software.amazon.awssdk.core.interceptor.trait.HttpChecksumRequired;
import software.amazon.awssdk.core.internal.interceptor.trait.RequestCompression;
import software.amazon.awssdk.schema.ApiOperation;
import software.amazon.awssdk.schema.Schema;
import software.amazon.awssdk.schema.SerializableStruct;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.TraitKey;
import software.amazon.awssdk.schema.traits.EndpointTrait;
import software.amazon.awssdk.schema.traits.HttpChecksumTrait;
import software.amazon.awssdk.schema.traits.HttpHeaderTrait;
import software.amazon.awssdk.schema.traits.PaginatedTrait;
import software.amazon.awssdk.schema.traits.RequestCompressionTrait;
import software.amazon.awssdk.utils.HostnameValidator;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * The per-operation settings of a dynamic call that a generated client hard-codes in each operation method, derived
 * once per operation from the operation's schemas and runtime traits only (Requirement 30.4): the
 * {@code smithy.api#endpoint} host prefix, {@code smithy.api#requestCompression}, {@code aws.protocols#httpChecksum},
 * {@code smithy.api#httpChecksumRequired}, the bearer credential type and the {@code smithy.api#paginated} trait merged
 * with the service's.
 *
 * <p>{@link #apply} puts them on a call's {@link ClientExecutionParams} as a generated client's operation method does
 * (codegen's {@code SchemaOperationsProtocolSpec}): {@code hostPrefixExpression}, {@code credentialType} and the
 * {@code HTTP_CHECKSUM_REQUIRED}, {@code HTTP_CHECKSUM} and {@code REQUEST_COMPRESSION} execution attributes, so the
 * shared pipeline's compression and checksum stages do the rest. The members that hold per-call values (host labels,
 * the request checksum algorithm and the response validation mode) are read from the call's input.
 *
 * <p>Instances are immutable and safe to use from multiple threads.
 */
@SdkInternalApi
@ThreadSafe
public final class DynamicOperationMetadata {
    /**
     * The metadata of the call's operation, which the endpoint resolver reads to add the host prefix.
     */
    public static final ExecutionAttribute<DynamicOperationMetadata> METADATA =
        new ExecutionAttribute<>("DynamicOperationMetadata");

    private static final Logger log = Logger.loggerFor(DynamicOperationMetadata.class);
    private static final Pattern HOST_LABEL = Pattern.compile("\\{([^}]+)}");
    private static final ShapeId BEARER_AUTH = ShapeId.fromParts("smithy.api", "httpBearerAuth");

    /**
     * The response checksum algorithms in the order codegen's {@code HttpChecksumTrait} sorts them, fastest first.
     */
    private static final List<ChecksumAlgorithm> CHECKSUM_ALGORITHM_PRIORITY = Collections.unmodifiableList(Arrays.asList(
        DefaultChecksumAlgorithm.XXHASH3, DefaultChecksumAlgorithm.XXHASH128, DefaultChecksumAlgorithm.CRC64NVME,
        DefaultChecksumAlgorithm.CRC32C, DefaultChecksumAlgorithm.CRC32, DefaultChecksumAlgorithm.XXHASH64,
        DefaultChecksumAlgorithm.SHA512, DefaultChecksumAlgorithm.SHA256, DefaultChecksumAlgorithm.SHA1,
        DefaultChecksumAlgorithm.MD5));

    private final String operationName;
    private final String inputName;
    private final String hostPrefix;
    private final String hostPrefixFormat;
    private final List<Schema> hostLabelMembers;
    private final RequestCompression requestCompression;
    private final boolean httpChecksumRequired;
    private final HttpChecksumTrait httpChecksumTrait;
    private final boolean inputStreaming;
    private final Schema requestAlgorithmMember;
    private final String requestAlgorithmHeader;
    private final Schema requestValidationModeMember;
    private final List<ChecksumAlgorithm> responseAlgorithms;
    private final boolean bearerAuthPreferred;
    private final PaginatedTrait paginated;

    private DynamicOperationMetadata(ApiOperation<?, ?> operation) {
        Schema schema = operation.schema();
        Schema input = operation.inputSchema();
        this.operationName = operation.name();
        this.inputName = StringUtils.uncapitalize(input.id().getName());
        this.inputStreaming = operation.inputStreamMember() != null;

        EndpointTrait endpoint = schema.getTrait(TraitKey.ENDPOINT_TRAIT);
        this.hostPrefix = endpoint == null || StringUtils.isEmpty(endpoint.getHostPrefix()) ? null
                                                                                           : endpoint.getHostPrefix();
        List<Schema> labels = new ArrayList<>();
        this.hostPrefixFormat = hostPrefix == null ? null : hostPrefixFormat(hostPrefix, input, labels);
        this.hostLabelMembers = Collections.unmodifiableList(labels);

        RequestCompressionTrait compression = schema.getTrait(TraitKey.REQUEST_COMPRESSION_TRAIT);
        this.requestCompression = compression == null
                                  ? null
                                  : RequestCompression.builder()
                                                      .encodings(compression.getEncodings())
                                                      .isStreaming(inputStreaming)
                                                      .build();
        this.httpChecksumRequired = schema.hasTrait(TraitKey.HTTP_CHECKSUM_REQUIRED_TRAIT);

        this.httpChecksumTrait = schema.getTrait(TraitKey.HTTP_CHECKSUM_TRAIT);
        this.requestAlgorithmMember = httpChecksumTrait == null
                                      ? null
                                      : httpChecksumTrait.getRequestAlgorithmMember().map(input::member).orElse(null);
        HttpHeaderTrait algorithmHeader = requestAlgorithmMember == null
                                          ? null
                                          : requestAlgorithmMember.getTrait(TraitKey.HTTP_HEADER_TRAIT);
        this.requestAlgorithmHeader = algorithmHeader == null ? null : algorithmHeader.getValue();
        this.requestValidationModeMember =
            httpChecksumTrait == null
            ? null
            : httpChecksumTrait.getRequestValidationModeMember().map(input::member).orElse(null);
        this.responseAlgorithms = httpChecksumTrait == null ? Collections.emptyList()
                                                            : responseAlgorithms(httpChecksumTrait, operationName);

        List<ShapeId> auth = operation.effectiveAuthSchemes();
        this.bearerAuthPreferred = !auth.isEmpty() && BEARER_AUTH.equals(auth.get(0));
        this.paginated = paginated(schema.getTrait(TraitKey.PAGINATED_TRAIT),
                                   operation.service().schema().getTrait(TraitKey.PAGINATED_TRAIT));
    }

    /**
     * Derives the call metadata of an operation from its schemas and traits.
     *
     * @param operation the operation
     * @return the metadata
     */
    public static DynamicOperationMetadata of(ApiOperation<?, ?> operation) {
        return new DynamicOperationMetadata(operation);
    }

    /**
     * Puts the metadata on a call's parameters, as a generated client's operation method does.
     *
     * @param params the call's parameters
     * @param input the call's input, which holds the host labels and the checksum members
     * @throws IllegalArgumentException if a host label member of the input is missing or not a valid host name part
     */
    public void apply(ClientExecutionParams<?, ?> params, SerializableStruct input) {
        params.putExecutionAttribute(METADATA, this);
        String resolvedHostPrefix = resolvedHostPrefix(input);
        if (resolvedHostPrefix != null) {
            params.hostPrefixExpression(resolvedHostPrefix);
        }
        if (bearerAuthPreferred) {
            params.credentialType(CredentialType.TOKEN);
        }
        if (httpChecksumRequired) {
            params.putExecutionAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM_REQUIRED,
                                         HttpChecksumRequired.create());
        }
        HttpChecksum httpChecksum = httpChecksum(input);
        if (httpChecksum != null) {
            params.putExecutionAttribute(SdkInternalExecutionAttribute.HTTP_CHECKSUM, httpChecksum);
        }
        if (requestCompression != null) {
            params.putExecutionAttribute(SdkInternalExecutionAttribute.REQUEST_COMPRESSION, requestCompression);
        }
    }

    /**
     * The host prefix of the operation's {@code endpoint} trait with the input's host labels filled in, as a generated
     * client's {@code resolvedHostExpression} and {@code <Service>EndpointResolverUtils.hostPrefix}.
     *
     * @param input the call's input
     * @return the host prefix, or null when the operation has none
     * @throws IllegalArgumentException if a host label is missing, empty, longer than 63 characters or not made of
     *     letters, digits and hyphens
     */
    public String resolvedHostPrefix(SerializableStruct input) {
        if (hostPrefixFormat == null) {
            return null;
        }
        if (hostLabelMembers.isEmpty()) {
            return hostPrefixFormat;
        }
        Object[] values = new Object[hostLabelMembers.size()];
        for (int i = 0; i < values.length; i++) {
            Schema member = hostLabelMembers.get(i);
            Object value = input.getMemberValue(member);
            String label = value == null ? null : value.toString();
            HostnameValidator.validateHostnameCompliant(label, member.memberName(), inputName);
            values[i] = label;
        }
        return String.format(hostPrefixFormat, values);
    }

    /**
     * The {@code HTTP_CHECKSUM} attribute of a call, as a generated client builds it: the static trait values, plus
     * the input's request algorithm and response validation mode members.
     *
     * @param input the call's input
     * @return the attribute, or null when the operation has no {@code httpChecksum} trait
     */
    public HttpChecksum httpChecksum(SerializableStruct input) {
        if (httpChecksumTrait == null) {
            return null;
        }
        HttpChecksum.Builder builder = HttpChecksum.builder()
                                                   .requestChecksumRequired(httpChecksumTrait.isRequestChecksumRequired())
                                                   .isRequestStreaming(inputStreaming);
        if (requestAlgorithmMember != null) {
            builder.requestAlgorithm(stringValue(input, requestAlgorithmMember))
                   .requestAlgorithmHeader(requestAlgorithmHeader);
        }
        if (requestValidationModeMember != null) {
            builder.requestValidationMode(stringValue(input, requestValidationModeMember));
            if (!responseAlgorithms.isEmpty()) {
                builder.responseAlgorithmsV2(responseAlgorithms.toArray(new ChecksumAlgorithm[0]));
            }
        }
        return builder.build();
    }

    /**
     * The host prefix of the operation's {@code endpoint} trait, with its labels unresolved.
     *
     * @return the host prefix, or null
     */
    public String hostPrefix() {
        return hostPrefix;
    }

    /**
     * The {@code REQUEST_COMPRESSION} attribute of every call, from the {@code requestCompression} trait.
     *
     * @return the attribute, or null when the operation has no {@code requestCompression} trait
     */
    public RequestCompression requestCompression() {
        return requestCompression;
    }

    /**
     * Whether the operation has the {@code httpChecksumRequired} trait.
     *
     * @return true if it has
     */
    public boolean httpChecksumRequired() {
        return httpChecksumRequired;
    }

    /**
     * Whether the operation's first effective auth scheme is {@code smithy.api#httpBearerAuth}, so the call's
     * credential type is {@code TOKEN}.
     *
     * @return true if it is
     */
    public boolean bearerAuthPreferred() {
        return bearerAuthPreferred;
    }

    /**
     * The operation's {@code paginated} trait merged with the service's, as {@code PaginatedIndex} merges them: an
     * operation without the trait is not paginated, and a value the operation does not set comes from the service.
     *
     * @return the merged trait, or null when the operation has no {@code paginated} trait
     */
    public PaginatedTrait paginated() {
        return paginated;
    }

    private static String stringValue(SerializableStruct input, Schema member) {
        Object value = input.getMemberValue(member);
        return value == null ? null : value.toString();
    }

    /**
     * Replaces each {@code {label}} with {@code %s}, as codegen's {@code HostPrefixProcessor}, and collects the input
     * members the labels name.
     */
    private static String hostPrefixFormat(String hostPrefix, Schema input, List<Schema> labels) {
        Matcher matcher = HOST_LABEL.matcher(hostPrefix);
        StringBuffer format = new StringBuffer();
        while (matcher.find()) {
            String label = matcher.group(1);
            Schema member = input.member(label);
            if (member == null) {
                throw new IllegalStateException("The host prefix " + hostPrefix + " of operation names the host label "
                                                + label + ", which is not a member of " + input.id());
            }
            labels.add(member);
            matcher.appendReplacement(format, "%s");
        }
        matcher.appendTail(format);
        // A literal '%' in a host prefix would be a format specifier; host names cannot contain one anyway.
        return format.toString();
    }

    /**
     * The trait's response algorithms that the SDK implements, fastest first. An algorithm the SDK does not know is
     * skipped: codegen rejects it at generation time, but a model loaded at runtime may name a newer one.
     */
    private static List<ChecksumAlgorithm> responseAlgorithms(HttpChecksumTrait trait, String operationName) {
        List<ChecksumAlgorithm> algorithms = new ArrayList<>();
        for (String name : trait.getResponseAlgorithms()) {
            ChecksumAlgorithm algorithm = DefaultChecksumAlgorithm.fromValue(name.toUpperCase(Locale.US));
            if (algorithm == null || !CHECKSUM_ALGORITHM_PRIORITY.contains(algorithm)) {
                log.debug(() -> "Skipping response checksum algorithm " + name + " of operation " + operationName
                                + ": the SDK does not implement it.");
                continue;
            }
            algorithms.add(algorithm);
        }
        algorithms.sort(Comparator.comparingInt(CHECKSUM_ALGORITHM_PRIORITY::indexOf));
        return Collections.unmodifiableList(algorithms);
    }

    private static PaginatedTrait paginated(PaginatedTrait operation, PaginatedTrait service) {
        if (operation == null || service == null) {
            return operation;
        }
        return new PaginatedTrait(operation.getInputToken().orElse(service.getInputToken().orElse(null)),
                                  operation.getOutputToken().orElse(service.getOutputToken().orElse(null)),
                                  operation.getItems().orElse(service.getItems().orElse(null)),
                                  operation.getPageSize().orElse(service.getPageSize().orElse(null)));
    }
}
