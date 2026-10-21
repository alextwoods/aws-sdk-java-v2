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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.dynamicclient.DynamicAuthSchemeParams;
import software.amazon.awssdk.dynamicclient.DynamicAuthSchemeResolver;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.schema.ShapeId;
import software.amazon.awssdk.utils.Validate;

/**
 * The default auth scheme resolver of a dynamic client: the effective auth schemes of the called operation from the
 * model ({@link DynamicAuthSchemes}), in model order, without the schemes that have no implementation (Requirements
 * 27.3 and 27.4).
 *
 * <p>The options carry the signer properties a generated {@code <Service>AuthSchemeProvider} sets:
 * <ul>
 *     <li>{@code aws.auth#sigv4}: the trait's signing name, the region of the call, and payload signing off for an
 *     operation with {@code aws.auth#unsignedPayload};</li>
 *     <li>{@code aws.auth#sigv4a}: the trait's signing name, the sigv4a region set of the call (or its region), and
 *     payload signing off for {@code aws.auth#unsignedPayload};</li>
 *     <li>any other scheme: the scheme ID only.</li>
 * </ul>
 * When the profile file sets {@code auth_scheme_preference}, the preferred schemes come first, as with the generated
 * {@code Preferred<Service>AuthSchemeProvider}.
 *
 * <p>An operation none of whose effective schemes has an implementation fails with an {@link SdkClientException} that
 * names the missing schemes. Auth scheme resolution runs before a request is signed and sent, so no request is sent.
 */
@SdkInternalApi
@ThreadSafe
public final class ModelAuthSchemeResolver implements DynamicAuthSchemeResolver {
    private final DynamicAuthSchemes authSchemes;
    private final Set<String> implementedSchemes;
    private final List<String> preferences;

    /**
     * Creates a resolver.
     *
     * @param authSchemes the effective auth schemes of the service
     * @param implementedSchemes the IDs of the client's auth schemes: the key set of its
     *     {@code SdkClientOption.AUTH_SCHEMES}. The resolver keeps the set, not a copy, so the schemes a plugin adds to
     *     that map while the client is built are seen
     * @param preferences the {@code auth_scheme_preference} scheme names, such as {@code sigv4a}; may be empty
     */
    public ModelAuthSchemeResolver(DynamicAuthSchemes authSchemes, Set<String> implementedSchemes,
                                   List<String> preferences) {
        this.authSchemes = Validate.paramNotNull(authSchemes, "authSchemes");
        this.implementedSchemes = Collections.unmodifiableSet(Validate.paramNotNull(implementedSchemes,
                                                                                    "implementedSchemes"));
        this.preferences = Collections.unmodifiableList(new ArrayList<>(
            Validate.paramNotNull(preferences, "preferences")));
    }

    /**
     * Returns the IDs of the auth schemes this resolver can return options for.
     *
     * @return the scheme IDs
     */
    public Set<String> implementedSchemes() {
        return implementedSchemes;
    }

    /**
     * Returns the effective auth schemes of the service this resolver resolves from.
     *
     * @return the auth schemes
     */
    public DynamicAuthSchemes authSchemes() {
        return authSchemes;
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(DynamicAuthSchemeParams params) {
        ShapeId operationId = params.operation().schema().id();
        DynamicAuthSchemes.OperationAuth operation = authSchemes.operation(operationId);
        if (operation == null) {
            throw new IllegalArgumentException("Operation " + operationId + " is not an operation of service "
                                               + authSchemes.serviceId());
        }
        List<AuthSchemeOption> options = new ArrayList<>(operation.schemes().size());
        List<String> missing = new ArrayList<>();
        for (DynamicAuthSchemes.EffectiveAuthScheme scheme : operation.schemes()) {
            if (implementedSchemes.contains(scheme.schemeId())) {
                options.add(option(scheme, operation, params.executionAttributes()));
            } else {
                missing.add(scheme.schemeId());
            }
        }
        if (options.isEmpty()) {
            throw SdkClientException.create(
                "Cannot call operation " + operationId + ": none of its auth schemes has an implementation. Missing "
                + "auth schemes: " + missing + ". Register an implementation with DynamicClient.Builder.putAuthScheme, "
                + "or configure an authSchemeResolver.");
        }
        return Collections.unmodifiableList(applyPreferences(options));
    }

    private static AuthSchemeOption option(DynamicAuthSchemes.EffectiveAuthScheme scheme,
                                           DynamicAuthSchemes.OperationAuth operation,
                                           ExecutionAttributes attributes) {
        AuthSchemeOption.Builder option = AuthSchemeOption.builder().schemeId(scheme.schemeId());
        Region region = attributes.getAttribute(AwsExecutionAttribute.AWS_REGION);
        if (AwsV4AuthScheme.SCHEME_ID.equals(scheme.schemeId())) {
            option.putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, scheme.signingName());
            if (region != null) {
                option.putSignerProperty(AwsV4HttpSigner.REGION_NAME, region.id());
            }
            unsignedPayload(option, operation);
        } else if (AwsV4aAuthScheme.SCHEME_ID.equals(scheme.schemeId())) {
            option.putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, scheme.signingName());
            RegionSet regionSet = regionSet(attributes, region);
            if (regionSet != null) {
                option.putSignerProperty(AwsV4aHttpSigner.REGION_SET, regionSet);
            }
            unsignedPayload(option, operation);
        }
        return option.build();
    }

    private static void unsignedPayload(AuthSchemeOption.Builder option, DynamicAuthSchemes.OperationAuth operation) {
        if (operation.unsignedPayload()) {
            option.putSignerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false);
        }
    }

    private static RegionSet regionSet(ExecutionAttributes attributes, Region region) {
        Set<String> configured = attributes.getAttribute(AwsExecutionAttribute.AWS_SIGV4A_SIGNING_REGION_SET);
        if (configured != null && !configured.isEmpty()) {
            return RegionSet.create(configured);
        }
        return region == null ? null : RegionSet.create(region.id());
    }

    /**
     * Moves the options whose scheme name (the part after {@code #}) is in the preferences to the front, in preference
     * order, as the generated {@code Preferred<Service>AuthSchemeProvider} does.
     */
    private List<AuthSchemeOption> applyPreferences(List<AuthSchemeOption> options) {
        if (preferences.isEmpty()) {
            return options;
        }
        List<AuthSchemeOption> ordered = new ArrayList<>(options.size());
        for (String preferred : preferences) {
            for (AuthSchemeOption option : options) {
                if (schemeName(option.schemeId()).equals(preferred) && !ordered.contains(option)) {
                    ordered.add(option);
                    break;
                }
            }
        }
        for (AuthSchemeOption option : options) {
            if (!ordered.contains(option)) {
                ordered.add(option);
            }
        }
        return ordered;
    }

    private static String schemeName(String schemeId) {
        int hash = schemeId.indexOf('#');
        return hash < 0 ? schemeId : schemeId.substring(hash + 1);
    }
}
