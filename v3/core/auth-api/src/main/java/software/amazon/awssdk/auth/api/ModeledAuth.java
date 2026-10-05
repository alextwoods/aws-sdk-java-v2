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

package software.amazon.awssdk.auth.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import software.amazon.awssdk.utils.ToString;

/**
 * The auth traits of one operation of one service in the Canonical Smithy Model, as generated metadata (or a dynamic
 * model) records them, and the effective auth schemes they select (Requirement 13.1).
 *
 * <p>{@link #effectiveSchemeIds()} follows Smithy's {@code ServiceIndex.getEffectiveAuthSchemes(service, operation,
 * NO_AUTH_AWARE)}:
 *
 * <ol>
 *   <li>The service schemes are the auth-definition traits applied to the service, in the order of the service's
 *       {@code @auth} trait when it has one, otherwise sorted by absolute shape ID the way Smithy's {@code ShapeId} orders them
 *       (case-insensitively, then case-sensitively as a tiebreak).</li>
 *   <li>An operation {@code @auth} trait selects, in its own order, those of its schemes the service applies; otherwise
 *       the operation uses the service schemes.</li>
 *   <li>{@code smithy.api#noAuth} is appended when the operation has {@code @optionalAuth} or no scheme remains.</li>
 * </ol>
 *
 * <p>The per-scheme trait values the signers need (for example the {@code aws.auth#sigv4} {@code name}) are kept as
 * {@linkplain #schemeTraitValues(String) string maps} keyed by trait member name. Immutable.
 */
public final class ModeledAuth {
    /** The {@code aws.auth#unsignedPayload} trait: the operation's payload is not signed. */
    public static final String UNSIGNED_PAYLOAD_TRAIT = "aws.auth#unsignedPayload";

    /**
     * Smithy's {@code ShapeId.compareTo} over absolute shape IDs: case-insensitive on the full ID, then case-sensitive as
     * a tiebreak.
     */
    private static final Comparator<String> SHAPE_ID_ORDER =
        String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

    private final String operationId;
    private final Map<String, Map<String, String>> serviceSchemes;
    private final List<String> serviceAuthTrait;
    private final List<String> operationAuthTrait;
    private final boolean optionalAuth;
    private final boolean unsignedPayload;
    private final List<String> effective;

    private ModeledAuth(Builder builder) {
        this.operationId = Objects.requireNonNull(builder.operationId, "operationId");
        this.serviceSchemes = Map.copyOf(builder.serviceSchemes);
        this.serviceAuthTrait = builder.serviceAuthTrait == null ? null : List.copyOf(builder.serviceAuthTrait);
        this.operationAuthTrait = builder.operationAuthTrait == null ? null : List.copyOf(builder.operationAuthTrait);
        this.optionalAuth = builder.optionalAuth;
        this.unsignedPayload = builder.unsignedPayload;
        this.effective = List.copyOf(computeEffective());
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The absolute shape ID of the operation, for diagnostics. */
    public String operationId() {
        return operationId;
    }

    /** The effective auth scheme IDs of the operation, in priority order; never empty. */
    public List<String> effectiveSchemeIds() {
        return effective;
    }

    /** The trait member values of the service's {@code schemeId} auth trait, if the service applies it. */
    public Optional<Map<String, String>> schemeTraitValues(String schemeId) {
        return Optional.ofNullable(serviceSchemes.get(schemeId));
    }

    /** Whether the operation has {@code @optionalAuth}. */
    public boolean optionalAuth() {
        return optionalAuth;
    }

    /** Whether the operation has {@code aws.auth#unsignedPayload}. */
    public boolean unsignedPayload() {
        return unsignedPayload;
    }

    private List<String> computeEffective() {
        List<String> service = new ArrayList<>();
        if (serviceAuthTrait != null) {
            serviceAuthTrait.stream().filter(serviceSchemes::containsKey).forEach(service::add);
        } else {
            TreeSet<String> sorted = new TreeSet<>(SHAPE_ID_ORDER);
            sorted.addAll(serviceSchemes.keySet());
            service.addAll(sorted);
        }
        List<String> result = new ArrayList<>();
        if (operationAuthTrait != null) {
            operationAuthTrait.stream().filter(serviceSchemes::containsKey).distinct().forEach(result::add);
        } else {
            result.addAll(service);
        }
        result.remove(AuthSchemeIds.NO_AUTH);
        if (result.isEmpty() || optionalAuth) {
            result.add(AuthSchemeIds.NO_AUTH);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ModeledAuth that && operationId.equals(that.operationId)
               && serviceSchemes.equals(that.serviceSchemes) && Objects.equals(serviceAuthTrait, that.serviceAuthTrait)
               && Objects.equals(operationAuthTrait, that.operationAuthTrait) && optionalAuth == that.optionalAuth
               && unsignedPayload == that.unsignedPayload;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operationId, serviceSchemes, serviceAuthTrait, operationAuthTrait, optionalAuth,
                            unsignedPayload);
    }

    @Override
    public String toString() {
        return ToString.builder("ModeledAuth").add("operation", operationId).add("effective", effective).build();
    }

    /** Builder of {@link ModeledAuth}. */
    public static final class Builder {
        private String operationId;
        private final Map<String, Map<String, String>> serviceSchemes = new LinkedHashMap<>();
        private List<String> serviceAuthTrait;
        private List<String> operationAuthTrait;
        private boolean optionalAuth;
        private boolean unsignedPayload;

        private Builder() {
        }

        /** The absolute shape ID of the operation. */
        public Builder operationId(String operationId) {
            this.operationId = operationId;
            return this;
        }

        /** An auth-definition trait the service applies, with no member values. */
        public Builder addServiceScheme(String schemeId) {
            return addServiceScheme(schemeId, Map.of());
        }

        /** An auth-definition trait the service applies, with its member values (for example {@code name}). */
        public Builder addServiceScheme(String schemeId, Map<String, String> traitValues) {
            serviceSchemes.put(Objects.requireNonNull(schemeId, "schemeId"), Map.copyOf(traitValues));
            return this;
        }

        /** The service's {@code @auth} trait value; {@code null} when the service has none. */
        public Builder serviceAuthTrait(List<String> schemeIds) {
            this.serviceAuthTrait = schemeIds;
            return this;
        }

        /** The operation's {@code @auth} trait value; {@code null} when the operation has none. */
        public Builder operationAuthTrait(List<String> schemeIds) {
            this.operationAuthTrait = schemeIds;
            return this;
        }

        public Builder optionalAuth(boolean optionalAuth) {
            this.optionalAuth = optionalAuth;
            return this;
        }

        public Builder unsignedPayload(boolean unsignedPayload) {
            this.unsignedPayload = unsignedPayload;
            return this;
        }

        public ModeledAuth build() {
            return new ModeledAuth(this);
        }
    }
}
