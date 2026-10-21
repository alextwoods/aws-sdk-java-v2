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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.dynamicclient.schema.internal.ModelConversions;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.scheme.BearerAuthScheme;
import software.amazon.awssdk.http.auth.scheme.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Validate;
import software.amazon.smithy.aws.traits.auth.SigV4ATrait;
import software.amazon.smithy.aws.traits.auth.SigV4Trait;
import software.amazon.smithy.aws.traits.auth.UnsignedPayloadTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * The effective auth schemes of every operation of a dynamic client's service, derived from the model with the rules
 * codegen uses for {@code OperationModel.effectiveAuthSchemes} (Requirement 27.3):
 * {@code ServiceIndex.getEffectiveAuthSchemes(service, operation, NO_AUTH_AWARE)}, so the {@code auth},
 * {@code optionalAuth} and auth definition traits apply, and an operation with no modeled scheme gets
 * {@code smithy.api#noAuth} only.
 *
 * <p>Also holds the auth schemes the SDK implements: {@code aws.auth#sigv4}, {@code aws.auth#sigv4a} (only when the
 * optional {@code software.amazon.awssdk:http-auth-aws-crt} module is on the class path, because its signer needs it),
 * {@code smithy.api#httpBearerAuth} and {@code smithy.api#noAuth}.
 */
@SdkInternalApi
public final class DynamicAuthSchemes {
    /**
     * The marker class of {@code http-auth-aws-crt}, which {@code http-auth-aws} also checks before it creates the
     * sigv4a signer.
     */
    private static final String HTTP_AUTH_AWS_CRT_CLASS = "software.amazon.awssdk.http.auth.aws.crt.HttpAuthAwsCrt";

    private static final Lazy<Boolean> SIGV4A_SIGNER_AVAILABLE = new Lazy<>(DynamicAuthSchemes::loadSigv4aSigner);

    private final ShapeId serviceId;
    private final Map<software.amazon.awssdk.schema.ShapeId, OperationAuth> operations;
    private final Set<String> schemeIds;

    private DynamicAuthSchemes(ShapeId serviceId, Map<software.amazon.awssdk.schema.ShapeId, OperationAuth> operations,
                               Set<String> schemeIds) {
        this.serviceId = serviceId;
        this.operations = operations;
        this.schemeIds = schemeIds;
    }

    /**
     * Derives the effective auth schemes of every operation of a service.
     *
     * @param model the model
     * @param service the service shape of the model
     * @return the auth schemes of the service
     */
    public static DynamicAuthSchemes forService(Model model, ServiceShape service) {
        Validate.paramNotNull(model, "model");
        Validate.paramNotNull(service, "service");
        ServiceIndex serviceIndex = ServiceIndex.of(model);
        Map<software.amazon.awssdk.schema.ShapeId, OperationAuth> operations = new HashMap<>();
        Set<String> schemeIds = new TreeSet<>();
        for (OperationShape operation : TopDownIndex.of(model).getContainedOperations(service)) {
            Map<ShapeId, Trait> effective =
                serviceIndex.getEffectiveAuthSchemes(service, operation, ServiceIndex.AuthSchemeMode.NO_AUTH_AWARE);
            List<EffectiveAuthScheme> schemes = new ArrayList<>(effective.size());
            effective.forEach((id, trait) -> {
                schemes.add(new EffectiveAuthScheme(id.toString(), signingName(trait)));
                schemeIds.add(id.toString());
            });
            operations.put(ModelConversions.shapeId(operation.getId()),
                           new OperationAuth(operation.getId().getName(), Collections.unmodifiableList(schemes),
                                             operation.hasTrait(UnsignedPayloadTrait.class)));
        }
        return new DynamicAuthSchemes(service.getId(), Collections.unmodifiableMap(operations),
                                      Collections.unmodifiableSet(schemeIds));
    }

    /**
     * Returns the auth schemes the SDK implements, keyed by scheme ID, as a generated builder's built-in schemes.
     *
     * @param sigv4aSignerAvailable whether the sigv4a signer can be created ({@link #sigv4aSignerAvailable()})
     * @return a new mutable map
     */
    public static Map<String, AuthScheme<?>> builtInAuthSchemes(boolean sigv4aSignerAvailable) {
        Map<String, AuthScheme<?>> schemes = new HashMap<>();
        schemes.put(AwsV4AuthScheme.SCHEME_ID, AwsV4AuthScheme.create());
        if (sigv4aSignerAvailable) {
            schemes.put(AwsV4aAuthScheme.SCHEME_ID, AwsV4aAuthScheme.create());
        }
        schemes.put(BearerAuthScheme.SCHEME_ID, BearerAuthScheme.create());
        schemes.put(NoAuthAuthScheme.SCHEME_ID, NoAuthAuthScheme.create());
        return schemes;
    }

    /**
     * Returns whether the sigv4a signer can be created: whether the optional {@code http-auth-aws-crt} module is on
     * the class path. Checked once per class loader.
     *
     * @return true if the sigv4a signer is available
     */
    public static boolean sigv4aSignerAvailable() {
        return SIGV4A_SIGNER_AVAILABLE.getValue();
    }

    /**
     * Returns the ShapeId of the service.
     *
     * @return the service ShapeId
     */
    public ShapeId serviceId() {
        return serviceId;
    }

    /**
     * Returns the effective auth schemes of an operation of the service.
     *
     * @param operationId the operation ShapeId
     * @return the operation's auth, or null if the operation is not in the service
     */
    public OperationAuth operation(software.amazon.awssdk.schema.ShapeId operationId) {
        return operations.get(operationId);
    }

    /**
     * Returns the effective auth schemes that have no implementation, each with the operations that cannot be called
     * because none of their effective schemes has one.
     *
     * @param implemented the IDs of the schemes with an implementation
     * @return the missing scheme IDs, sorted, each with the sorted names of the operations it leaves without a usable
     *     scheme (possibly none)
     */
    public Map<String, Set<String>> missingSchemes(Collection<String> implemented) {
        Map<String, Set<String>> missing = new TreeMap<>();
        for (String schemeId : schemeIds) {
            if (!implemented.contains(schemeId)) {
                missing.put(schemeId, new TreeSet<>());
            }
        }
        if (missing.isEmpty()) {
            return missing;
        }
        for (OperationAuth operation : operations.values()) {
            if (!operation.hasImplementedScheme(implemented)) {
                for (EffectiveAuthScheme scheme : operation.schemes()) {
                    missing.get(scheme.schemeId()).add(operation.name());
                }
            }
        }
        return missing;
    }

    private static String signingName(Trait trait) {
        if (trait instanceof SigV4Trait) {
            return ((SigV4Trait) trait).getName();
        }
        if (trait instanceof SigV4ATrait) {
            return ((SigV4ATrait) trait).getName();
        }
        return null;
    }

    private static boolean loadSigv4aSigner() {
        ClassLoader classLoader = DynamicAuthSchemes.class.getClassLoader();
        try {
            Class.forName(HTTP_AUTH_AWS_CRT_CLASS, false, classLoader);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /**
     * The effective auth schemes of one operation.
     */
    public static final class OperationAuth {
        private final String name;
        private final List<EffectiveAuthScheme> schemes;
        private final boolean unsignedPayload;

        private OperationAuth(String name, List<EffectiveAuthScheme> schemes, boolean unsignedPayload) {
            this.name = name;
            this.schemes = schemes;
            this.unsignedPayload = unsignedPayload;
        }

        /**
         * Returns the operation's shape name.
         *
         * @return the shape name
         */
        public String name() {
            return name;
        }

        /**
         * Returns the effective auth schemes in priority order.
         *
         * @return the schemes; never empty
         */
        public List<EffectiveAuthScheme> schemes() {
            return schemes;
        }

        /**
         * Returns whether the operation has the {@code aws.auth#unsignedPayload} trait.
         *
         * @return true if the payload is not signed
         */
        public boolean unsignedPayload() {
            return unsignedPayload;
        }

        boolean hasImplementedScheme(Collection<String> implemented) {
            for (EffectiveAuthScheme scheme : schemes) {
                if (implemented.contains(scheme.schemeId())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * One effective auth scheme of an operation.
     */
    public static final class EffectiveAuthScheme {
        private final String schemeId;
        private final String signingName;

        private EffectiveAuthScheme(String schemeId, String signingName) {
            this.schemeId = schemeId;
            this.signingName = signingName;
        }

        /**
         * Returns the scheme ID, the ShapeId of its auth definition trait.
         *
         * @return the scheme ID
         */
        public String schemeId() {
            return schemeId;
        }

        /**
         * Returns the signing name of a {@code aws.auth#sigv4} or {@code aws.auth#sigv4a} scheme.
         *
         * @return the signing name, or null for other schemes
         */
        public String signingName() {
            return signingName;
        }
    }
}
