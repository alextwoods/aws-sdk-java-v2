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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.dynamicclient.DynamicAuthSchemeParams;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.scheme.BearerAuthScheme;
import software.amazon.awssdk.http.auth.scheme.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

class ModelAuthSchemeResolverTest {
    private static final Model MODEL = InternalTestModels.assemble("auth.smithy");
    private static final DynamicAuthSchemes AUTH_SCHEMES =
        DynamicAuthSchemes.forService(MODEL, InternalTestModels.onlyService(MODEL));
    private static final Set<String> BUILT_IN = DynamicAuthSchemes.builtInAuthSchemes(true).keySet();

    @Test
    void forService_derivesTheEffectiveSchemesOfEachOperation() {
        assertThat(schemeIds("DefaultAuth")).containsExactly("aws.auth#sigv4");
        assertThat(schemeIds("Sigv4aFirst")).containsExactly("aws.auth#sigv4a", "aws.auth#sigv4");
        assertThat(schemeIds("BearerOnly")).containsExactly("smithy.api#httpBearerAuth");
        assertThat(schemeIds("Anonymous")).containsExactly("smithy.api#noAuth");
        assertThat(schemeIds("OptionalAuth")).containsExactly("aws.auth#sigv4", "smithy.api#noAuth");
        assertThat(schemeIds("BasicOnly")).containsExactly("smithy.api#httpBasicAuth");
        assertThat(AUTH_SCHEMES.operation(software.amazon.awssdk.schema.ShapeId.from("test.auth#Unknown"))).isNull();
    }

    @Test
    void builtInAuthSchemes_includeSigv4aOnlyWhenItsSignerIsAvailable() {
        assertThat(DynamicAuthSchemes.builtInAuthSchemes(true))
            .containsOnlyKeys(AwsV4AuthScheme.SCHEME_ID, AwsV4aAuthScheme.SCHEME_ID, BearerAuthScheme.SCHEME_ID,
                              NoAuthAuthScheme.SCHEME_ID);
        assertThat(DynamicAuthSchemes.builtInAuthSchemes(false))
            .containsOnlyKeys(AwsV4AuthScheme.SCHEME_ID, BearerAuthScheme.SCHEME_ID, NoAuthAuthScheme.SCHEME_ID);
        // http-auth-aws-crt is an optional dependency of http-auth-aws and is not on this module's class path.
        assertThat(DynamicAuthSchemes.sigv4aSignerAvailable()).isFalse();
    }

    @Test
    void resolveAuthScheme_sigv4_hasTheSigningNameAndTheRegionOfTheCall() {
        AuthSchemeOption option = resolve(BUILT_IN, "DefaultAuth", attributes()).get(0);

        assertThat(option.schemeId()).isEqualTo("aws.auth#sigv4");
        assertThat(option.signerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME)).isEqualTo("auth-signing");
        assertThat(option.signerProperty(AwsV4HttpSigner.REGION_NAME)).isEqualTo("us-west-2");
        assertThat(option.signerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED)).isNull();
    }

    @Test
    void resolveAuthScheme_sigv4a_hasTheSigningNameAndTheRegionSet() {
        List<AuthSchemeOption> options = resolve(BUILT_IN, "Sigv4aFirst", attributes());
        ExecutionAttributes withRegionSet = attributes();
        withRegionSet.putAttribute(AwsExecutionAttribute.AWS_SIGV4A_SIGNING_REGION_SET,
                                   new HashSet<>(Arrays.asList("us-east-1", "us-west-2")));
        AuthSchemeOption configured = resolve(BUILT_IN, "Sigv4aFirst", withRegionSet).get(0);

        assertThat(options).extracting(AuthSchemeOption::schemeId).containsExactly("aws.auth#sigv4a", "aws.auth#sigv4");
        assertThat(options.get(0).signerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME)).isEqualTo("auth-signing");
        assertThat(options.get(0).signerProperty(AwsV4aHttpSigner.REGION_SET)).isEqualTo(RegionSet.create("us-west-2"));
        assertThat(configured.signerProperty(AwsV4aHttpSigner.REGION_SET))
            .isEqualTo(RegionSet.create(Arrays.asList("us-east-1", "us-west-2")));
    }

    @Test
    void resolveAuthScheme_bearerAndNoAuth_haveTheSchemeIdOnly() {
        AuthSchemeOption bearer = resolve(BUILT_IN, "BearerOnly", attributes()).get(0);
        AuthSchemeOption noAuth = resolve(BUILT_IN, "Anonymous", attributes()).get(0);
        List<AuthSchemeOption> optional = resolve(BUILT_IN, "OptionalAuth", attributes());

        assertThat(bearer.schemeId()).isEqualTo("smithy.api#httpBearerAuth");
        assertThat(signerPropertyCount(bearer)).isZero();
        assertThat(noAuth.schemeId()).isEqualTo("smithy.api#noAuth");
        assertThat(signerPropertyCount(noAuth)).isZero();
        assertThat(optional).extracting(AuthSchemeOption::schemeId).containsExactly("aws.auth#sigv4", "smithy.api#noAuth");
    }

    @Test
    void resolveAuthScheme_unsignedPayload_turnsPayloadSigningOff() {
        AuthSchemeOption option = resolve(BUILT_IN, "UnsignedPayload", attributes()).get(0);

        assertThat(option.signerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED)).isFalse();
    }

    @Test
    void resolveAuthScheme_withoutAnImplementation_skipsTheScheme() {
        Set<String> withoutSigv4a = DynamicAuthSchemes.builtInAuthSchemes(false).keySet();

        assertThat(resolve(withoutSigv4a, "Sigv4aFirst", attributes())).extracting(AuthSchemeOption::schemeId)
                                                                     .containsExactly("aws.auth#sigv4");
        assertThat(resolve(BUILT_IN, "BasicThenSigv4", attributes())).extracting(AuthSchemeOption::schemeId)
                                                                     .containsExactly("aws.auth#sigv4");
    }

    @Test
    void resolveAuthScheme_noUsableScheme_failsNamingTheMissingSchemes() {
        assertThatThrownBy(() -> resolve(BUILT_IN, "BasicOnly", attributes()))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Cannot call operation test.auth#BasicOnly: none of its auth schemes has an implementation. "
                        + "Missing auth schemes: [smithy.api#httpBasicAuth]. Register an implementation with "
                        + "DynamicClient.Builder.putAuthScheme, or configure an authSchemeResolver.");
    }

    @Test
    void resolveAuthScheme_withARegisteredImplementation_usesTheScheme() {
        Set<String> withBasic = new HashSet<>(BUILT_IN);
        withBasic.add("smithy.api#httpBasicAuth");

        assertThat(resolve(withBasic, "BasicOnly", attributes())).extracting(AuthSchemeOption::schemeId)
                                                                 .containsExactly("smithy.api#httpBasicAuth");
    }

    @Test
    void resolveAuthScheme_withPreferences_movesThePreferredSchemesFirst() {
        ModelAuthSchemeResolver resolver = new ModelAuthSchemeResolver(AUTH_SCHEMES, BUILT_IN,
                                                                       Arrays.asList("noAuth", "unknown"));

        List<AuthSchemeOption> options = resolver.resolveAuthScheme(params("OptionalAuth", attributes()));

        assertThat(options).extracting(AuthSchemeOption::schemeId).containsExactly("smithy.api#noAuth", "aws.auth#sigv4");
    }

    @Test
    void resolveAuthScheme_operationOfAnotherService_fails() {
        Model other = InternalTestModels.assembleIdl("$version: \"2.0\"\nnamespace test.other\n"
                                                     + "service Other { operations: [DefaultAuth] }\n"
                                                     + "operation DefaultAuth {}\n");
        ModelAuthSchemeResolver resolver = new ModelAuthSchemeResolver(AUTH_SCHEMES, BUILT_IN, Collections.emptyList());
        DynamicAuthSchemeParams params = DynamicAuthSchemeParams.builder()
                                                                .protocolId(software.amazon.awssdk.schema.ShapeId.from(
                                                                    "aws.protocols#awsJson1_0"))
                                                                .operation(InternalTestModels.operation(
                                                                    other, ShapeId.from("test.other#DefaultAuth")))
                                                                .build();

        assertThatThrownBy(() -> resolver.resolveAuthScheme(params))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Operation test.other#DefaultAuth is not an operation of service test.auth#AuthService");
    }

    @Test
    void missingSchemes_listsEachMissingSchemeWithTheOperationsItLeavesWithoutAuth() {
        Map<String, Set<String>> missing =
            AUTH_SCHEMES.missingSchemes(DynamicAuthSchemes.builtInAuthSchemes(false).keySet());

        assertThat(missing).containsOnlyKeys("aws.auth#sigv4a", "smithy.api#httpBasicAuth");
        assertThat(missing.get("aws.auth#sigv4a")).isEmpty();
        assertThat(missing.get("smithy.api#httpBasicAuth")).containsExactly("BasicOnly");
        assertThat(AUTH_SCHEMES.missingSchemes(new TreeSet<>(Arrays.asList(
            "aws.auth#sigv4", "aws.auth#sigv4a", "smithy.api#httpBearerAuth", "smithy.api#noAuth",
            "smithy.api#httpBasicAuth")))).isEmpty();
    }

    private static int signerPropertyCount(AuthSchemeOption option) {
        int[] count = {0};
        option.forEachSignerProperty(new AuthSchemeOption.SignerPropertyConsumer() {
            @Override
            public <T> void accept(SignerProperty<T> propertyKey, T propertyValue) {
                count[0]++;
            }
        });
        return count[0];
    }

    private static List<String> schemeIds(String operation) {
        return AUTH_SCHEMES.operation(software.amazon.awssdk.schema.ShapeId.from("test.auth#" + operation))
                           .schemes()
                           .stream()
                           .map(DynamicAuthSchemes.EffectiveAuthScheme::schemeId)
                           .collect(Collectors.toList());
    }

    private static List<AuthSchemeOption> resolve(Set<String> implemented, String operation,
                                                  ExecutionAttributes attributes) {
        return new ModelAuthSchemeResolver(AUTH_SCHEMES, implemented, Collections.emptyList())
            .resolveAuthScheme(params(operation, attributes));
    }

    private static DynamicAuthSchemeParams params(String operation, ExecutionAttributes attributes) {
        return DynamicAuthSchemeParams.builder()
                                      .protocolId(software.amazon.awssdk.schema.ShapeId.from("aws.protocols#awsJson1_0"))
                                      .operation(InternalTestModels.operation(MODEL, ShapeId.from("test.auth#" + operation)))
                                      .executionAttributes(attributes)
                                      .build();
    }

    private static ExecutionAttributes attributes() {
        ExecutionAttributes attributes = new ExecutionAttributes();
        attributes.putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_WEST_2);
        return attributes;
    }
}
