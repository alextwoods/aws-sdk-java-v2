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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.BaseSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public abstract class DefaultBaseSignRequest<PayloadT, IdentityT extends Identity>
    implements BaseSignRequest<PayloadT, IdentityT> {

    protected final SdkHttpRequest request;
    protected final PayloadT payload;
    protected final IdentityT identity;
    /**
     * Properties set directly on this request. Unmodifiable. When {@link #signerProperties} is present, this map holds
     * only the request-level properties (clock, checksum store, ...) and the auth-scheme's properties are read through
     * to the option rather than copied here.
     */
    protected final Map<SignerProperty<?>, Object> properties;

    /**
     * The resolved auth scheme's signer properties, when the request was built from one, read through on lookup.
     *
     * <p>The pipeline used to copy every property of the selected {@link AuthSchemeOption} into the request's own map on
     * every call: an iteration through a consumer callback, a {@code HashMap} put per property, a resize, and then a
     * second copy of the whole map at build. All of it to move values that already sit in an immutable map one lookup
     * away. Holding the option and consulting it on {@link #property} does the same job with no per-call copy.
     *
     * <p>Precedence is preserved: the copy used to run <i>after</i> the request-level puts, so an option property
     * overrode a request-level one of the same key. Here the option is consulted first for the same reason.
     */
    protected final AuthSchemeOption signerProperties;

    protected DefaultBaseSignRequest(BuilderImpl<?, PayloadT, IdentityT> builder) {
        this.request = Validate.paramNotNull(builder.request, "request");
        this.payload = builder.payload;
        this.identity = Validate.paramNotNull(builder.identity, "identity");
        this.signerProperties = builder.signerProperties;
        // The builder's map is owned by the request from here on; the builder copies before any later mutation.
        this.properties = Collections.unmodifiableMap(builder.properties);
        builder.propertiesShared = true;
    }

    @Override
    public SdkHttpRequest request() {
        return request;
    }

    @Override
    public Optional<PayloadT> payload() {
        return Optional.ofNullable(payload);
    }

    @Override
    public IdentityT identity() {
        return identity;
    }

    @Override
    public <T> T property(SignerProperty<T> property) {
        if (signerProperties != null) {
            T fromScheme = signerProperties.signerProperty(property);
            if (fromScheme != null) {
                return fromScheme;
            }
        }
        return (T) properties.get(property);
    }

    /**
     * Every property visible through {@link #property}, materialized. For {@code toBuilder} and {@code toString}, which
     * need the merged view rather than the two halves.
     */
    protected Map<SignerProperty<?>, Object> allProperties() {
        if (signerProperties == null) {
            return properties;
        }
        Map<SignerProperty<?>, Object> merged = new HashMap<>(properties);
        signerProperties.forEachSignerProperty(new AuthSchemeOption.SignerPropertyConsumer() {
            @Override
            public <T> void accept(SignerProperty<T> key, T value) {
                merged.put(key, value);
            }
        });
        return Collections.unmodifiableMap(merged);
    }

    @SdkInternalApi
    public abstract static class BuilderImpl<B extends Builder<B, PayloadT, IdentityT>, PayloadT,
        IdentityT extends Identity> implements Builder<B, PayloadT, IdentityT> {
        private Map<SignerProperty<?>, Object> properties = new HashMap<>();
        private boolean propertiesShared;
        private AuthSchemeOption signerProperties;
        private SdkHttpRequest request;
        private PayloadT payload;
        private IdentityT identity;

        protected BuilderImpl() {
        }

        protected BuilderImpl(IdentityT identity) {
            this.identity = identity;
        }

        @Override
        public B request(SdkHttpRequest request) {
            this.request = request;
            return thisBuilder();
        }

        @Override
        public B payload(PayloadT payload) {
            this.payload = payload;
            return thisBuilder();
        }

        @Override
        public B identity(IdentityT identity) {
            this.identity = identity;
            return thisBuilder();
        }

        @Override
        public <T> B putProperty(SignerProperty<T> key, T value) {
            ownedProperties().put(key, value);
            return thisBuilder();
        }

        /**
         * Read the given auth scheme's signer properties through, instead of copying them into this request. Internal:
         * the pipeline's signing stages use it; the public builder surface is unchanged.
         */
        public B signerProperties(AuthSchemeOption authSchemeOption) {
            this.signerProperties = authSchemeOption;
            return thisBuilder();
        }

        protected B properties(Map<SignerProperty<?>, Object> properties) {
            this.properties = new HashMap<>(properties);
            this.propertiesShared = false;
            this.signerProperties = null;
            return thisBuilder();
        }

        /**
         * The map to mutate: a private copy if a built request still holds the current one.
         */
        private Map<SignerProperty<?>, Object> ownedProperties() {
            if (propertiesShared) {
                properties = new HashMap<>(properties);
                propertiesShared = false;
            }
            return properties;
        }

        private B thisBuilder() {
            return (B) this;
        }
    }
}
