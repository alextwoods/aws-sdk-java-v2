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

package software.amazon.awssdk.codegen.c2j2smithy;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.aws.traits.auth.SigV4ATrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.model.traits.AuthTrait;
import software.amazon.smithy.model.traits.HttpBearerAuthTrait;
import software.amazon.smithy.model.traits.OptionalAuthTrait;
import software.amazon.smithy.model.traits.Trait;

/**
 * Converts C2J signing and auth properties into Smithy auth traits.
 *
 * <p>The service's default schemes come from {@code metadata.auth} (a list of auth scheme ShapeIds), or, when it is
 * absent, from {@code metadata.signatureVersion}. Per operation, the C2J {@code auth} list takes precedence over
 * {@code authtype}. {@code smithy.api#noAuth} alone becomes {@code @auth([])}; mixed with other schemes it becomes
 * {@code @optionalAuth} plus {@code @auth} with the other schemes. {@code v4-unsigned-body} and
 * {@code unsignedPayload: true} add {@code aws.auth#unsignedPayload}.
 *
 * <p>Every auth definition trait any operation needs is applied to the service. When the effective service default
 * differs from what Smithy derives from the applied auth traits (all of them, sorted by ShapeId), the service also gets
 * an {@code @auth} trait with its C2J default list.
 */
final class AuthConversion {
    static final ShapeId NO_AUTH = ShapeId.from("smithy.api#noAuth");

    private final String signingName;
    private final List<ShapeId> serviceDefault;
    private final boolean serviceAuthConverted;
    private final Map<String, OperationAuth> operations = new HashMap<>();
    private final Set<ShapeId> applied = new TreeSet<>();

    private AuthConversion(JsonNode metadata, JsonNode c2jOperations, ConversionGapReport.Builder gaps) {
        String endpointPrefix = metadata.path("endpointPrefix").asText(null);
        String name = metadata.path("signingName").asText(endpointPrefix);
        this.signingName = name != null ? name : metadata.path("serviceId").asText("service").toLowerCase(Locale.US);

        List<ShapeId> defaults = null;
        if (metadata.path("auth").isArray()) {
            defaults = new ArrayList<>();
            for (JsonNode scheme : metadata.get("auth")) {
                ShapeId id = schemeId(scheme.asText(), gaps);
                if (NO_AUTH.equals(id)) {
                    gaps.record("metadata.auth=" + NO_AUTH, "Service-level optional auth has no standard trait (only "
                                                            + "operations take @optionalAuth); it is ignored.");
                } else if (id != null) {
                    defaults.add(id);
                }
            }
        } else if (metadata.path("signatureVersion").isTextual()) {
            defaults = authtypeSchemes(metadata.get("signatureVersion").asText());
        }
        this.serviceAuthConverted = defaults != null;
        this.serviceDefault = defaults != null ? defaults : Collections.emptyList();
        applied.addAll(serviceDefault);

        for (Map.Entry<String, JsonNode> e : c2jOperations.properties()) {
            OperationAuth auth = operationAuth(e.getValue(), gaps);
            operations.put(e.getKey(), auth);
            if (auth.schemes != null) {
                applied.addAll(auth.schemes);
            }
        }
        if (applied.contains(SigV4ATrait.ID)) {
            // The sigv4a trait can only be applied to a service that also has sigv4.
            applied.add(SigV4Trait.ID);
        }
    }

    static AuthConversion of(JsonNode metadata, JsonNode c2jOperations, ConversionGapReport.Builder gaps) {
        return new AuthConversion(metadata, c2jOperations, gaps);
    }

    /**
     * Whether {@code metadata.signatureVersion} (or {@code metadata.auth}) was converted to auth traits.
     */
    boolean serviceAuthConverted() {
        return serviceAuthConverted;
    }

    /**
     * The auth definition traits for the service, plus {@code @auth} when the C2J default order is not the one Smithy
     * derives.
     */
    List<Trait> serviceTraits() {
        List<Trait> traits = new ArrayList<>();
        for (ShapeId id : applied) {
            if (SigV4Trait.ID.equals(id)) {
                traits.add(SigV4Trait.builder().name(signingName).build());
            } else if (SigV4ATrait.ID.equals(id)) {
                traits.add(SigV4ATrait.builder().name(signingName).build());
            } else if (HttpBearerAuthTrait.ID.equals(id)) {
                traits.add(new HttpBearerAuthTrait());
            }
        }
        if (!serviceDefault.equals(new ArrayList<>(applied))) {
            traits.add(new AuthTrait(serviceDefault));
        }
        return traits;
    }

    /**
     * The auth traits for one operation, keyed by its C2J operations-map key.
     */
    List<Trait> operationTraits(String operationKey) {
        OperationAuth auth = operations.get(operationKey);
        List<Trait> traits = new ArrayList<>(3);
        if (auth == null) {
            return traits;
        }
        if (auth.schemes != null && !auth.schemes.equals(serviceDefault)) {
            traits.add(new AuthTrait(auth.schemes));
        }
        if (auth.optional) {
            traits.add(new OptionalAuthTrait());
        }
        if (auth.unsignedPayload) {
            traits.add(new UnsignedPayloadTrait());
        }
        return traits;
    }

    /**
     * The C2J authtype of an operation that has no standard trait, or null.
     */
    String unmappedAuthtype(String operationKey) {
        OperationAuth auth = operations.get(operationKey);
        return auth == null ? null : auth.unmappedAuthtype;
    }

    private static OperationAuth operationAuth(JsonNode op, ConversionGapReport.Builder gaps) {
        OperationAuth auth = new OperationAuth();
        String authtype = op.path("authtype").asText(null);
        auth.unsignedPayload = "v4-unsigned-body".equals(authtype) || op.path("unsignedPayload").asBoolean(false);
        if (op.path("auth").isArray()) {
            List<ShapeId> schemes = new ArrayList<>();
            boolean noAuth = false;
            for (JsonNode scheme : op.get("auth")) {
                ShapeId id = schemeId(scheme.asText(), gaps);
                if (NO_AUTH.equals(id)) {
                    noAuth = true;
                } else if (id != null) {
                    schemes.add(id);
                }
            }
            auth.optional = noAuth && !schemes.isEmpty();
            auth.schemes = schemes;
        } else if (authtype != null) {
            auth.schemes = authtypeSchemes(authtype);
            if (auth.schemes == null) {
                auth.unmappedAuthtype = authtype;
            }
        }
        return auth;
    }

    // A C2J auth scheme ShapeId with a runtime trait mapping, or null (recorded as a gap).
    private static ShapeId schemeId(String scheme, ConversionGapReport.Builder gaps) {
        ShapeId id;
        try {
            id = ShapeId.from(scheme);
        } catch (ShapeIdSyntaxException e) {
            id = null;
        }
        if (NO_AUTH.equals(id) || SigV4Trait.ID.equals(id) || SigV4ATrait.ID.equals(id)
            || HttpBearerAuthTrait.ID.equals(id)) {
            return id;
        }
        gaps.record("auth=" + scheme, "No auth trait is emitted for this auth scheme, so it is not an effective auth "
                                      + "scheme of the service or operation.");
        return null;
    }

    // The schemes a C2J authtype or signatureVersion denotes, or null when it has no standard trait.
    private static List<ShapeId> authtypeSchemes(String authtype) {
        switch (authtype) {
            case "none":
                return Collections.emptyList();
            case "v4":
            case "s3":
            case "s3v4":
            case "iam":
            case "v4-unsigned-body":
                return Collections.singletonList(SigV4Trait.ID);
            case "v4a":
                return Collections.singletonList(SigV4ATrait.ID);
            case "bearer":
                return Collections.singletonList(HttpBearerAuthTrait.ID);
            default:
                return null;
        }
    }

    private static final class OperationAuth {
        private List<ShapeId> schemes;
        private boolean optional;
        private boolean unsignedPayload;
        private String unmappedAuthtype;
    }
}
