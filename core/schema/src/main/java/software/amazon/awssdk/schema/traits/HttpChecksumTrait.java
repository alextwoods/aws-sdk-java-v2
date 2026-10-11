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

package software.amazon.awssdk.schema.traits;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.schema.serde.document.Document;
import software.amazon.awssdk.utils.ToString;

/**
 * The {@code aws.protocols#httpChecksum} trait: the request and response checksum behavior of an operation.
 *
 * <p>SDK runtime form of {@code software.amazon.smithy.aws.traits.HttpChecksumTrait}.
 */
@SdkPublicApi
public final class HttpChecksumTrait implements Trait {

    /**
     * The trait's shape ID, {@code aws.protocols#httpChecksum}.
     */
    public static final ShapeId ID = ShapeId.fromParts("aws.protocols", "httpChecksum");

    private static final String REQUEST_CHECKSUM_REQUIRED = "requestChecksumRequired";
    private static final String REQUEST_ALGORITHM_MEMBER = "requestAlgorithmMember";
    private static final String REQUEST_VALIDATION_MODE_MEMBER = "requestValidationModeMember";
    private static final String RESPONSE_ALGORITHMS = "responseAlgorithms";

    private final boolean requestChecksumRequired;
    private final String requestAlgorithmMember;
    private final String requestValidationModeMember;
    private final List<String> responseAlgorithms;

    /**
     * Creates the trait.
     *
     * @param requestChecksumRequired whether a request checksum is required
     * @param requestAlgorithmMember the input member that selects the request checksum algorithm, or null
     * @param requestValidationModeMember the input member that opts in to response validation, or null
     * @param responseAlgorithms the algorithms whose response checksums are validated; may be empty
     * @throws NullPointerException if {@code responseAlgorithms} or one of its elements is null
     */
    public HttpChecksumTrait(boolean requestChecksumRequired, String requestAlgorithmMember,
                             String requestValidationModeMember, List<String> responseAlgorithms) {
        this.requestChecksumRequired = requestChecksumRequired;
        this.requestAlgorithmMember = requestAlgorithmMember;
        this.requestValidationModeMember = requestValidationModeMember;
        this.responseAlgorithms = TraitValues.copyOf(responseAlgorithms, "responseAlgorithms");
    }

    @Override
    public ShapeId traitId() {
        return ID;
    }

    /**
     * Returns whether a request checksum is required.
     *
     * @return true if requests must carry a checksum
     */
    public boolean isRequestChecksumRequired() {
        return requestChecksumRequired;
    }

    /**
     * Returns the input member that selects the request checksum algorithm.
     *
     * @return the member name, if set
     */
    public Optional<String> getRequestAlgorithmMember() {
        return Optional.ofNullable(requestAlgorithmMember);
    }

    /**
     * Returns the input member that opts in to response checksum validation.
     *
     * @return the member name, if set
     */
    public Optional<String> getRequestValidationModeMember() {
        return Optional.ofNullable(requestValidationModeMember);
    }

    /**
     * Returns the algorithms whose response checksums are validated.
     *
     * @return an unmodifiable, possibly empty list of algorithm names
     */
    public List<String> getResponseAlgorithms() {
        return responseAlgorithms;
    }

    @Override
    public Document toDocument() {
        Map<String, Document> members = new LinkedHashMap<>();
        if (requestAlgorithmMember != null) {
            members.put(REQUEST_ALGORITHM_MEMBER, Document.of(requestAlgorithmMember));
        }
        if (requestValidationModeMember != null) {
            members.put(REQUEST_VALIDATION_MODE_MEMBER, Document.of(requestValidationModeMember));
        }
        if (requestChecksumRequired) {
            members.put(REQUEST_CHECKSUM_REQUIRED, Document.of(true));
        }
        if (!responseAlgorithms.isEmpty()) {
            members.put(RESPONSE_ALGORITHMS, TraitValues.stringList(responseAlgorithms));
        }
        return Document.of(members);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HttpChecksumTrait)) {
            return false;
        }
        HttpChecksumTrait other = (HttpChecksumTrait) o;
        return requestChecksumRequired == other.requestChecksumRequired
               && Objects.equals(requestAlgorithmMember, other.requestAlgorithmMember)
               && Objects.equals(requestValidationModeMember, other.requestValidationModeMember)
               && responseAlgorithms.equals(other.responseAlgorithms);
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(requestChecksumRequired);
        result = 31 * result + Objects.hashCode(requestAlgorithmMember);
        result = 31 * result + Objects.hashCode(requestValidationModeMember);
        return 31 * result + responseAlgorithms.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("HttpChecksumTrait")
                       .add("requestChecksumRequired", requestChecksumRequired)
                       .add("requestAlgorithmMember", requestAlgorithmMember)
                       .add("requestValidationModeMember", requestValidationModeMember)
                       .add("responseAlgorithms", responseAlgorithms)
                       .build();
    }

    static HttpChecksumTrait fromDocument(Document value) {
        Map<String, Document> object = TraitValues.expectObject(ID, value);
        return new HttpChecksumTrait(TraitValues.optionalBoolean(ID, object, REQUEST_CHECKSUM_REQUIRED),
                                     TraitValues.optionalString(ID, object, REQUEST_ALGORITHM_MEMBER),
                                     TraitValues.optionalString(ID, object, REQUEST_VALIDATION_MODE_MEMBER),
                                     TraitValues.optionalStringList(ID, object, RESPONSE_ALGORITHMS));
    }
}
