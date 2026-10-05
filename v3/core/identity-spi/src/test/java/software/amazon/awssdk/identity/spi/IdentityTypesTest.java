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

package software.amazon.awssdk.identity.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class IdentityTypesTest {
    private static final IdentityProperty<String> ROLE = IdentityProperty.create(IdentityTypesTest.class, "Role");

    @Test
    void credentialsExposeValuesAndCompareWithoutProviderName() {
        AwsCredentialsIdentity basic = AwsCredentialsIdentity.create("AKID", "SECRET");
        assertEquals("AKID", basic.accessKeyId());
        assertEquals("SECRET", basic.secretAccessKey());
        assertEquals(Optional.empty(), basic.accountId());
        assertEquals(Optional.empty(), basic.providerName());
        assertEquals(Optional.empty(), basic.expirationTime());

        AwsCredentialsIdentity named = AwsCredentialsIdentity.builder().accessKeyId("AKID").secretAccessKey("SECRET")
                                                             .providerName("Static").build();
        assertEquals(Optional.of("Static"), named.providerName());
        assertEquals(basic, named);
        assertEquals(basic.hashCode(), named.hashCode());
        AwsCredentialsIdentity withAccount = AwsCredentialsIdentity.builder().accessKeyId("AKID")
                                                                   .secretAccessKey("SECRET").accountId("123").build();
        assertEquals(Optional.of("123"), withAccount.accountId());
        assertNotEquals(basic, withAccount);
        assertNotEquals(basic, AwsCredentialsIdentity.create("AKID", "OTHER"));
    }

    @Test
    void sessionCredentialsAreDistinctFromBasicCredentials() {
        AwsSessionCredentialsIdentity session = AwsSessionCredentialsIdentity.create("AKID", "SECRET", "TOKEN");
        assertEquals("TOKEN", session.sessionToken());
        assertEquals(session, AwsSessionCredentialsIdentity.create("AKID", "SECRET", "TOKEN"));
        assertEquals(session.hashCode(), AwsSessionCredentialsIdentity.create("AKID", "SECRET", "TOKEN").hashCode());
        assertNotEquals(session, AwsSessionCredentialsIdentity.create("AKID", "SECRET", "OTHER"));
        assertNotEquals(AwsCredentialsIdentity.create("AKID", "SECRET"), session);
        assertTrue(session instanceof AwsCredentialsIdentity);
    }

    @Test
    void secretsNeverAppearInToString() {
        String basic = AwsCredentialsIdentity.create("AKID", "SECRET").toString();
        String session = AwsSessionCredentialsIdentity.create("AKID", "SECRET", "TOKEN").toString();
        assertTrue(basic.contains("AKID"), basic);
        assertFalse(basic.contains("SECRET"), basic);
        assertFalse(session.contains("SECRET"), session);
        assertFalse(session.contains("TOKEN"), session);
    }

    @Test
    void missingRequiredValuesNameTheValue() {
        NullPointerException e = assertThrows(NullPointerException.class,
                                              () -> AwsCredentialsIdentity.create("AKID", null));
        assertEquals("secretAccessKey", e.getMessage());
        e = assertThrows(NullPointerException.class,
                         () -> AwsSessionCredentialsIdentity.builder().accessKeyId("A").secretAccessKey("S").build());
        assertEquals("sessionToken", e.getMessage());
    }

    @Test
    void resolveIdentityRequestsAreImmutableTypedPropertyMaps() {
        ResolveIdentityRequest request = ResolveIdentityRequest.builder().putProperty(ROLE, "admin").build();
        assertEquals("admin", request.property(ROLE));
        ResolveIdentityRequest.Builder builder = request.toBuilder();
        builder.putProperty(ROLE, null);
        assertEquals("admin", request.property(ROLE), "a builder never changes the request it came from");
        ResolveIdentityRequest empty = builder.build();
        assertNull(empty.property(ROLE));
        assertEquals(ResolveIdentityRequest.builder().build(), empty);
        assertEquals(request, ResolveIdentityRequest.builder().putProperty(ROLE, "admin").build());
        assertEquals(request.hashCode(), request.copy(b -> { }).hashCode());
        assertFalse(request.toString().contains("admin"), request.toString());
    }

    @Test
    void identityPropertiesAreUniquePerNamespaceAndName() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                                                  () -> IdentityProperty.create(IdentityTypesTest.class, "Role"));
        assertTrue(e.getMessage().contains("Role") && e.getMessage().contains(IdentityTypesTest.class.getName()),
                   e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> IdentityProperty.create(IdentityTypesTest.class, " "));
        assertEquals(IdentityTypesTest.class.getName(), ROLE.namespace());
        assertEquals("Role", ROLE.name());
        assertEquals(ROLE, ROLE);
    }

    @Test
    void providerDefaultMethodsDelegateWithTheBuiltRequest() {
        AwsCredentialsIdentity credentials = AwsCredentialsIdentity.create("AKID", "SECRET");
        AtomicReference<ResolveIdentityRequest> seen = new AtomicReference<>();
        IdentityProvider<AwsCredentialsIdentity> provider = new IdentityProvider<>() {
            @Override
            public Class<AwsCredentialsIdentity> identityType() {
                return AwsCredentialsIdentity.class;
            }

            @Override
            public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
                seen.set(request);
                return CompletableFuture.completedFuture(credentials);
            }
        };
        assertSame(credentials, provider.resolveIdentity().join());
        assertEquals(ResolveIdentityRequest.builder().build(), seen.get());
        assertSame(credentials, provider.resolveIdentity(r -> r.putProperty(ROLE, "reader")).join());
        assertEquals("reader", seen.get().property(ROLE));
        assertEquals(AwsCredentialsIdentity.class, provider.identityType());
    }
}
