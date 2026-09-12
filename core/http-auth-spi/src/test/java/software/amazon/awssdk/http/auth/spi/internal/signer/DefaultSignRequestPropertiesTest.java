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

package software.amazon.awssdk.http.auth.spi.internal.signer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.Identity;

/**
 * The sign request can read signer properties through an {@link AuthSchemeOption} instead of holding a copy. These pin
 * the behaviours that read-through must reproduce: lookup, precedence, the merged view on {@code toBuilder}, and
 * that a builder reused after {@code build()} does not mutate the request it already produced.
 */
class DefaultSignRequestPropertiesTest {

    private static final SignerProperty<String> REGION = SignerProperty.create(DefaultSignRequestPropertiesTest.class, "Region");
    private static final SignerProperty<String> NAME = SignerProperty.create(DefaultSignRequestPropertiesTest.class, "Name");
    private static final SignerProperty<String> CLOCK = SignerProperty.create(DefaultSignRequestPropertiesTest.class, "Clock");
    private static final Identity IDENTITY = new Identity() {
    };
    private static final SdkHttpRequest REQUEST = SdkHttpRequest.builder()
                                                                .protocol("https").host("h").method(SdkHttpMethod.GET)
                                                                .build();

    private static AuthSchemeOption option() {
        return AuthSchemeOption.builder().schemeId("test")
                               .putSignerProperty(REGION, "us-east-1")
                               .putSignerProperty(NAME, "svc")
                               .build();
    }

    private static DefaultSignRequest.BuilderImpl<Identity> builder() {
        return (DefaultSignRequest.BuilderImpl<Identity>) SignRequest.builder(IDENTITY).request(REQUEST);
    }

    @Test
    void schemePropertiesAreReadThrough() {
        SignRequest<Identity> request = builder().signerProperties(option()).putProperty(CLOCK, "clock").build();

        assertThat(request.property(REGION)).isEqualTo("us-east-1");
        assertThat(request.property(NAME)).isEqualTo("svc");
        assertThat(request.property(CLOCK)).isEqualTo("clock");
        assertThat(request.hasProperty(REGION)).isTrue();
        assertThat(request.requireProperty(NAME)).isEqualTo("svc");
    }

    @Test
    void schemePropertyWinsOverRequestLevelProperty_matchingTheOldCopyOrder() {
        // The old code copied scheme properties AFTER request-level puts, so the scheme value overrode.
        DefaultSignRequest.BuilderImpl<Identity> b = builder();
        b.putProperty(REGION, "request-level");
        SignRequest<Identity> request = b.signerProperties(option()).build();

        assertThat(request.property(REGION)).isEqualTo("us-east-1");
    }

    @Test
    void toBuilder_materializesTheMergedView() {
        SignRequest<Identity> request = builder().signerProperties(option()).putProperty(CLOCK, "clock").build();

        SignRequest<Identity> copy = request.toBuilder().build();

        assertThat(copy.property(REGION)).isEqualTo("us-east-1");
        assertThat(copy.property(NAME)).isEqualTo("svc");
        assertThat(copy.property(CLOCK)).isEqualTo("clock");
    }

    @Test
    void toBuilder_thenOverride_overridesEvenSchemeProperties() {
        SignRequest<Identity> request = builder().signerProperties(option()).build();

        SignRequest<Identity> copy = request.toBuilder().putProperty(REGION, "eu-west-1").build();

        assertThat(copy.property(REGION)).isEqualTo("eu-west-1");
        assertThat(request.property(REGION)).isEqualTo("us-east-1");
    }

    @Test
    void builderReusedAfterBuild_doesNotMutateTheBuiltRequest() {
        DefaultSignRequest.BuilderImpl<Identity> b = builder();
        b.putProperty(CLOCK, "first");
        SignRequest<Identity> first = b.build();

        b.putProperty(CLOCK, "second");
        SignRequest<Identity> second = b.build();

        assertThat(first.property(CLOCK)).isEqualTo("first");
        assertThat(second.property(CLOCK)).isEqualTo("second");
    }

    @Test
    void withoutScheme_behavesAsBefore() {
        SignRequest<Identity> request = builder().putProperty(CLOCK, "clock").build();

        assertThat(request.property(CLOCK)).isEqualTo("clock");
        assertThat(request.property(REGION)).isNull();
        assertThat(request.toString()).contains("clock");
    }

    @Test
    void toString_includesSchemeProperties() {
        SignRequest<Identity> request = builder().signerProperties(option()).build();

        assertThat(request.toString()).contains("us-east-1").contains("svc");
    }
}
