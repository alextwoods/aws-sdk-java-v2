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

package software.amazon.awssdk.identity.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.GenerationMode;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.auth.api.AuthScheme;
import software.amazon.awssdk.auth.api.AuthSchemeOption;
import software.amazon.awssdk.auth.api.AuthSchemeSelector;
import software.amazon.awssdk.auth.api.HttpSigner;
import software.amazon.awssdk.auth.api.ModeledAuth;
import software.amazon.awssdk.auth.api.NoAuthAuthScheme;
import software.amazon.awssdk.auth.api.SelectedAuthScheme;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.core.client.config.SettingSource;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;
import software.amazon.awssdk.profiles.SystemSettings;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.knowledge.ServiceIndex.AuthSchemeMode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Property 44: Auth and identity precedence follows model and manifest.
 *
 * <p>For any modeled service/operation auth traits and eligible default identity-source subset, V3 selects only an
 * effective modeled auth scheme and evaluates identity sources in the exact manifest precedence.
 *
 * <p>Modeled auth ({@link #selectionFollowsTheCanonicalEffectiveAuthSchemes}): each try generates the auth-definition
 * traits a service applies, optional service and operation {@code @auth} traits (which may name defined but unapplied
 * schemes), {@code @optionalAuth}, the schemes and identity types the client has, and an auth scheme preference. The
 * traits are rendered as a Smithy IDL model and the oracle is Smithy's own {@code ServiceIndex.getEffectiveAuthSchemes(
 * service, operation, NO_AUTH_AWARE)} over that model; {@link ModeledAuth} built from the same traits must produce the
 * same ordered list. {@link AuthSchemeSelector} must then pick exactly the scheme a table-driven oracle derives from the
 * canonical list (preference reordering, then the first candidate the client supports and has an identity provider
 * for), or fail naming the operation and every rejected candidate. The scheme pool deliberately mixes letter case
 * ({@code test.auth#alpha}, {@code test.auth#Beta}), because Smithy orders shape IDs case-insensitively.
 *
 * <p>Identity precedence ({@link #defaultCredentialSourcesAreEvaluatedInManifestPrecedence}): every combination of the
 * six default credential sources being absent, configured but failing (network sources), or eligible is enumerated
 * exhaustively. The oracle is the Parity Manifest table itself, read here as a plain classpath resource and parsed
 * independently of {@link DefaultCredentialsProvider}. The resolved credentials must come from the first eligible
 * source in manifest order, and the network sources (web identity through STS, container, instance metadata) must be
 * contacted exactly in manifest order up to that source and never after it. Every source runs for real: JVM properties
 * and environment through {@link SystemSettings}, a profile file under a per-try home directory, and the network
 * sources over {@link UrlConnectionHttpClient} against a local {@link LocalServer} (STS's regional host is redirected
 * to it).
 *
 * <p><b>Validates: Requirements 13.1, 13.3</b>
 */
class AuthAndIdentityPrecedencePropertyTest {
    private static final String SERVICE = "test.svc#Svc";
    private static final String OPERATION = "test.svc#Op";
    private static final String NO_AUTH = "smithy.api#noAuth";

    /** Auth-definition traits the generated services may apply: AWS, prelude, and mixed-case custom schemes. */
    private static final List<String> SCHEMES = List.of("aws.auth#sigv4", "aws.auth#sigv4a", "smithy.api#httpBearerAuth",
                                                        "smithy.api#httpBasicAuth", "smithy.api#httpDigestAuth",
                                                        "test.auth#alpha", "test.auth#Beta", "test.auth#custom");

    /** Names an auth scheme preference may list: every scheme name, noAuth, and an unknown name. */
    private static final List<String> PREFERENCE_NAMES = Stream.concat(
        Stream.concat(SCHEMES.stream(), Stream.of(NO_AUTH)).map(id -> id.substring(id.indexOf('#') + 1)),
        Stream.of("unknownScheme")).toList();

    // smithy-aws-traits is not on this module's classpath, so the aws.auth definitions are declared here with the
    // shape IDs and selectors of the canonical traits.
    private static final String AWS_AUTH_DEFINITIONS = """
        $version: "2"
        namespace aws.auth

        @trait(selector: "service")
        @authDefinition
        structure sigv4 {
            name: String
        }

        @trait(selector: "service")
        @authDefinition
        structure sigv4a {
            name: String
        }
        """;

    private static final String TEST_AUTH_DEFINITIONS = """
        $version: "2"
        namespace test.auth

        @trait(selector: "service")
        @authDefinition
        structure alpha {}

        @trait(selector: "service")
        @authDefinition
        structure Beta {}

        @trait(selector: "service")
        @authDefinition
        structure custom {}
        """;

    /** The Parity Manifest table of the default credential source precedence (Requirement 13.3). */
    private static final String PRECEDENCE_MANIFEST = "META-INF/aws-sdk-java-v3/identity/default-credentials-precedence.txt";

    private static final String STS_ACCESS_DENIED =
        "<ErrorResponse><Error><Code>AccessDenied</Code><Message>denied</Message></Error></ErrorResponse>";

    // ---------------------------------------------------------------------------------------------------------------
    // Requirement 13.1: modeled auth selection
    // ---------------------------------------------------------------------------------------------------------------

    @Property(tries = 1_000)
    void selectionFollowsTheCanonicalEffectiveAuthSchemes(@ForAll("authScenarios") AuthScenario scenario) {
        List<String> canonical = canonicalEffectiveSchemes(scenario);

        ModeledAuth modeled = scenario.modeledAuth();
        assertEquals(canonical, modeled.effectiveSchemeIds(), () -> "effective schemes of " + scenario.idl());

        List<AuthSchemeOption> options = modeled.effectiveSchemeIds().stream()
                                                .map(id -> AuthSchemeOption.builder().schemeId(id).build())
                                                .toList();
        AuthSchemeSelector selector = AuthSchemeSelector.create(scenario.clientSchemes(), scenario.preference());
        IdentityProviders providers = scenario.identityProviders();

        List<String> candidates = preferenceOrder(canonical, scenario.preference());
        Optional<String> expected = candidates.stream().filter(scenario::usable).findFirst();
        Statistics.label("outcome").collect(expected.isPresent() ? "selected" : "rejected");
        Statistics.label("service @auth").collect(scenario.serviceAuth() != null);
        Statistics.label("operation @auth").collect(scenario.operationAuth() != null);

        if (expected.isPresent()) {
            SelectedAuthScheme<?> selected = selector.select(OPERATION, options, providers);
            String schemeId = selected.option().schemeId();
            assertTrue(canonical.contains(schemeId), schemeId + " is not an effective modeled scheme " + canonical);
            assertEquals(expected.get(), schemeId, () -> "candidates " + candidates + " for " + scenario);
            assertEquals(schemeId, selected.scheme().schemeId());
            if (!schemeId.equals(NO_AUTH)) {
                assertSame(scenario.expectedProvider(schemeId, providers), selected.identityProvider());
            }
        } else {
            SdkClientException e = assertThrows(SdkClientException.class,
                                                () -> selector.select(OPERATION, options, providers));
            String message = e.getMessage();
            assertTrue(message.contains(OPERATION), message);
            for (String candidate : candidates) {
                String reason = scenario.supported().contains(candidate) ? candidate + " has no identity provider"
                                                                         : candidate + " is not supported";
                assertTrue(message.contains(reason), () -> "missing '" + reason + "' in " + message);
            }
        }
    }

    @Provide
    Arbitrary<AuthScenario> authScenarios() {
        Arbitrary<List<String>> applied = Arbitraries.of(SCHEMES).list().uniqueElements().ofMaxSize(SCHEMES.size());
        Arbitrary<List<String>> authTrait = Arbitraries.of(SCHEMES).list().uniqueElements().ofMaxSize(5)
                                                       .injectNull(0.4);
        Arbitrary<List<String>> supported = Arbitraries.of(Stream.concat(SCHEMES.stream(), Stream.of(NO_AUTH)).toList())
                                                       .list().uniqueElements().ofMaxSize(SCHEMES.size() + 1);
        Arbitrary<Set<IdentityKind>> kinds = Arbitraries.of(IdentityKind.class).set();
        Arbitrary<List<String>> preference = Arbitraries.of(PREFERENCE_NAMES).list().ofMaxSize(4);
        return Combinators.combine(applied, authTrait, authTrait, Arbitraries.of(true, false),
                                   Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8), supported, kinds,
                                   preference)
                          .as((a, s, o, optional, name, sup, k, p) -> new AuthScenario(a, s, o, optional, name,
                                                                                    Set.copyOf(sup), Set.copyOf(k), p));
    }

    /** Smithy's NO_AUTH_AWARE effective auth schemes of the operation in the IDL rendering of {@code scenario}. */
    private static List<String> canonicalEffectiveSchemes(AuthScenario scenario) {
        Model model = Model.assembler()
                           .addUnparsedModel("aws-auth.smithy", AWS_AUTH_DEFINITIONS)
                           .addUnparsedModel("test-auth.smithy", TEST_AUTH_DEFINITIONS)
                           .addUnparsedModel("service.smithy", scenario.idl())
                           // An @auth trait may name defined but unapplied schemes, which Smithy's validators reject
                           // but ServiceIndex (and ModeledAuth) must tolerate.
                           .disableValidation()
                           .assemble()
                           .unwrap();
        return ServiceIndex.of(model)
                           .getEffectiveAuthSchemes(ShapeId.from(SERVICE), ShapeId.from(OPERATION),
                                                    AuthSchemeMode.NO_AUTH_AWARE)
                           .keySet().stream().map(ShapeId::toString).toList();
    }

    /**
     * The V2 auth scheme preference rule, written out: the candidates whose scheme name (after {@code #}) is preferred,
     * in preference order, then the rest in modeled order.
     */
    private static List<String> preferenceOrder(List<String> modeled, List<String> preference) {
        List<String> ordered = new ArrayList<>();
        for (String name : preference) {
            for (String id : modeled) {
                if (id.substring(id.indexOf('#') + 1).equals(name) && !ordered.contains(id)) {
                    ordered.add(id);
                }
            }
        }
        for (String id : modeled) {
            if (!ordered.contains(id)) {
                ordered.add(id);
            }
        }
        return ordered;
    }

    /** Identity types the generated clients may have providers for. */
    enum IdentityKind {
        AWS(AwsCredentialsIdentity.class),
        AWS_SESSION(AwsSessionCredentialsIdentity.class),
        TOKEN(TokenIdentity.class),
        TEST(TestIdentity.class);

        private final Class<? extends Identity> type;

        IdentityKind(Class<? extends Identity> type) {
            this.type = type;
        }

        /** The identity kind the client's scheme {@code schemeId} signs with. */
        static IdentityKind of(String schemeId) {
            if (schemeId.startsWith("aws.auth#")) {
                return AWS;
            }
            return schemeId.equals("smithy.api#httpBearerAuth") ? TOKEN : TEST;
        }
    }

    /** The identity of the generated non-AWS, non-bearer schemes. */
    interface TestIdentity extends Identity {
    }

    /**
     * One generated service/operation/client combination.
     *
     * @param applied the auth-definition traits applied to the service, in declaration order
     * @param serviceAuth the service's {@code @auth} trait, or null
     * @param operationAuth the operation's {@code @auth} trait, or null
     */
    record AuthScenario(List<String> applied, List<String> serviceAuth, List<String> operationAuth,
                        boolean optionalAuth, String signingName, Set<String> supported, Set<IdentityKind> kinds,
                        List<String> preference) {
        String idl() {
            StringBuilder idl = new StringBuilder("$version: \"2\"\nnamespace test.svc\n\n");
            for (String id : applied) {
                idl.append('@').append(id);
                if (id.startsWith("aws.auth#")) {
                    idl.append("(name: \"").append(signingName).append("\")");
                }
                idl.append('\n');
            }
            if (serviceAuth != null) {
                idl.append(authTrait(serviceAuth));
            }
            idl.append("service Svc {\n    version: \"2024-01-01\"\n    operations: [Op]\n}\n\n");
            if (operationAuth != null) {
                idl.append(authTrait(operationAuth));
            }
            if (optionalAuth) {
                idl.append("@optionalAuth\n");
            }
            return idl.append("operation Op {}\n").toString();
        }

        private static String authTrait(List<String> ids) {
            return ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(", ", "@auth([", "])\n"));
        }

        ModeledAuth modeledAuth() {
            ModeledAuth.Builder builder = ModeledAuth.builder().operationId(OPERATION);
            for (String id : applied) {
                builder.addServiceScheme(id, id.startsWith("aws.auth#") ? Map.of("name", signingName) : Map.of());
            }
            return builder.serviceAuthTrait(serviceAuth)
                          .operationAuthTrait(operationAuth)
                          .optionalAuth(optionalAuth)
                          .build();
        }

        List<AuthScheme<?>> clientSchemes() {
            List<AuthScheme<?>> schemes = new ArrayList<>();
            supported.stream().sorted().forEach(id -> schemes.add(
                id.equals(NO_AUTH) ? NoAuthAuthScheme.create() : TestScheme.of(id, IdentityKind.of(id).type)));
            return schemes;
        }

        IdentityProviders identityProviders() {
            IdentityProviders.Builder builder = IdentityProviders.builder();
            kinds.stream().sorted().forEach(kind -> builder.putIdentityProvider(provider(kind.type)));
            return builder.build();
        }

        /** Whether the client can use {@code schemeId}: it has the scheme and a provider of its identity. */
        boolean usable(String schemeId) {
            if (!supported.contains(schemeId)) {
                return false;
            }
            if (schemeId.equals(NO_AUTH)) {
                return true;
            }
            IdentityKind required = IdentityKind.of(schemeId);
            return kinds.contains(required) || required == IdentityKind.AWS && kinds.contains(IdentityKind.AWS_SESSION);
        }

        /** The provider selection must use: the exact identity type's, else (AWS) the session-credentials one. */
        IdentityProvider<?> expectedProvider(String schemeId, IdentityProviders providers) {
            IdentityKind required = IdentityKind.of(schemeId);
            IdentityKind kind = kinds.contains(required) ? required : IdentityKind.AWS_SESSION;
            return providers.providers().stream().filter(p -> p.identityType() == kind.type).findFirst()
                            .orElseThrow();
        }
    }

    private static <T extends Identity> IdentityProvider<T> provider(Class<T> type) {
        return new IdentityProvider<>() {
            @Override
            public Class<T> identityType() {
                return type;
            }

            @Override
            public CompletableFuture<T> resolveIdentity(ResolveIdentityRequest request) {
                return CompletableFuture.failedFuture(new UnsupportedOperationException("selection only"));
            }

            @Override
            public String toString() {
                return "Provider(" + type.getSimpleName() + ")";
            }
        };
    }

    private record TestScheme<T extends Identity>(String schemeId, Class<T> identityType) implements AuthScheme<T> {
        static <T extends Identity> TestScheme<T> of(String schemeId, Class<T> identityType) {
            return new TestScheme<>(schemeId, identityType);
        }

        @Override
        public HttpSigner<T> signer() {
            return request -> SignedRequest.unsigned(request.request());
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Requirement 13.3: default identity source precedence
    // ---------------------------------------------------------------------------------------------------------------

    /** The manifest names exactly the sources this test can configure, and the provider reads it verbatim. */
    @Example
    void manifestNamesEveryConfigurableSourceOnce() {
        List<String> manifest = manifestPrecedence();
        assertEquals(manifest.size(), Set.copyOf(manifest).size(), "duplicate source in " + manifest);
        assertEquals(Stream.of(SourceFixture.values()).map(s -> s.id).collect(Collectors.toSet()), Set.copyOf(manifest));
        assertEquals(manifest, DefaultCredentialsProvider.sourcePrecedence().stream()
                                                         .map(DefaultCredentialsProvider.Source::id).toList());
    }

    @Property(generation = GenerationMode.EXHAUSTIVE)
    void defaultCredentialSourcesAreEvaluatedInManifestPrecedence(
        @ForAll("identityScenarios") Map<SourceFixture, SourceState> states) throws IOException {
        List<SourceFixture> precedence = manifestPrecedence().stream().map(SourceFixture::of).toList();
        Optional<SourceFixture> winner = precedence.stream().filter(s -> states.get(s) == SourceState.ELIGIBLE)
                                                   .findFirst();
        List<SourceFixture> expectedContacts = new ArrayList<>();
        for (SourceFixture source : precedence) {
            if (source.network && states.get(source) != SourceState.ABSENT) {
                expectedContacts.add(source);
            }
            if (winner.isPresent() && winner.get() == source) {
                break;
            }
        }
        Statistics.label("winner").collect(winner.map(Enum::name).orElse("none"));

        Path home = Files.createTempDirectory("aws-identity-precedence");
        try (LocalServer server = new LocalServer(); UrlConnectionHttpClient http = UrlConnectionHttpClient.create()) {
            SystemSettings settings = configure(states, home, server);
            try (DefaultCredentialsProvider provider =
                     DefaultCredentialsProvider.builder()
                                               .settings(settings)
                                               .httpClient(new LoopbackTransport(http, server.uri().getPort()))
                                               .build()) {
                if (winner.isPresent()) {
                    assertEquals(winner.get().accessKeyId(), provider.resolveIdentity().join().accessKeyId(),
                                 () -> "states " + states);
                } else {
                    CompletionException e = assertThrows(CompletionException.class,
                                                         () -> provider.resolveIdentity().join());
                    assertTrue(e.getCause().getMessage().startsWith("Unable to load credentials from any of the "
                                                                    + "providers in the chain"), e.getCause().getMessage());
                }
            }
            assertEquals(expectedContacts, contactOrder(server), () -> "states " + states);
        } finally {
            deleteRecursively(home);
        }
    }

    @Provide
    Arbitrary<Map<SourceFixture, SourceState>> identityScenarios() {
        List<Arbitrary<SourceState>> perSource = new ArrayList<>();
        for (SourceFixture source : SourceFixture.values()) {
            perSource.add(source.network ? Arbitraries.of(SourceState.class)
                                         : Arbitraries.of(SourceState.ABSENT, SourceState.ELIGIBLE));
        }
        return Combinators.combine(perSource).as(values -> {
            Map<SourceFixture, SourceState> states = new EnumMap<>(SourceFixture.class);
            for (int i = 0; i < values.size(); i++) {
                states.put(SourceFixture.values()[i], values.get(i));
            }
            return states;
        });
    }

    /** The default credential sources, keyed by their Parity Manifest ID. */
    enum SourceFixture {
        SYSTEM_PROPERTIES("system-properties", false),
        ENVIRONMENT("environment", false),
        WEB_IDENTITY("web-identity", true),
        PROFILE("profile", false),
        CONTAINER("container", true),
        INSTANCE_PROFILE("instance-profile", true);

        private final String id;
        private final boolean network;

        SourceFixture(String id, boolean network) {
            this.id = id;
            this.network = network;
        }

        static SourceFixture of(String id) {
            return Stream.of(values()).filter(s -> s.id.equals(id)).findFirst()
                         .orElseThrow(() -> new AssertionError("The manifest names an unknown source " + id));
        }

        String accessKeyId() {
            return "AKID_" + name();
        }
    }

    /** Absent (not configured), configured but failing (network sources only), or eligible. */
    enum SourceState {
        ABSENT,
        FAILING,
        ELIGIBLE
    }

    private static SystemSettings configure(Map<SourceFixture, SourceState> states, Path home, LocalServer server)
        throws IOException {
        Map<String, String> properties = new HashMap<>();
        Map<String, String> environment = new HashMap<>();
        environment.put("HOME", home.toString());
        environment.put("AWS_REGION", "us-east-1");
        states.forEach((source, state) -> {
            if (state == SourceState.ABSENT) {
                if (source == SourceFixture.INSTANCE_PROFILE) {
                    environment.put("AWS_EC2_METADATA_DISABLED", "true");
                }
                return;
            }
            boolean eligible = state == SourceState.ELIGIBLE;
            String key = source.accessKeyId();
            switch (source) {
                case SYSTEM_PROPERTIES -> {
                    properties.put("aws.accessKeyId", key);
                    properties.put("aws.secretAccessKey", "S");
                }
                case ENVIRONMENT -> {
                    environment.put("AWS_ACCESS_KEY_ID", key);
                    environment.put("AWS_SECRET_ACCESS_KEY", "S");
                }
                case WEB_IDENTITY -> {
                    environment.put("AWS_WEB_IDENTITY_TOKEN_FILE", write(home.resolve("web-identity-token"), "jwt"));
                    environment.put("AWS_ROLE_ARN", "arn:aws:iam::123456789012:role/r");
                    server.route("POST", "/", eligible ? 200 : 403, eligible ? stsCredentials(key) : STS_ACCESS_DENIED);
                }
                case PROFILE -> write(home.resolve(".aws/credentials"),
                                      "[default]\naws_access_key_id=" + key + "\naws_secret_access_key=S\n");
                case CONTAINER -> {
                    environment.put("AWS_CONTAINER_CREDENTIALS_FULL_URI", server.endpoint() + "/creds");
                    server.route("GET", "/creds", eligible ? 200 : 403,
                                 eligible ? NetworkCredentialsProvidersTest.creds(key, 3600) : "");
                }
                case INSTANCE_PROFILE -> {
                    environment.put("AWS_EC2_METADATA_SERVICE_ENDPOINT", server.endpoint());
                    server.route("PUT", "/latest/api/token", eligible ? 200 : 400, eligible ? "imds-token" : "");
                    server.route("GET", "/latest/meta-data/iam/security-credentials/", 200, "role");
                    server.route("GET", "/latest/meta-data/iam/security-credentials/role", 200,
                                 NetworkCredentialsProvidersTest.creds(key, 3600));
                }
                default -> fail("unhandled source " + source);
            }
        });
        return new SystemSettings(SettingSource.fromMap(properties), SettingSource.fromMap(environment));
    }

    /** The network sources the requests reached, in order, with consecutive requests of one source merged. */
    private static List<SourceFixture> contactOrder(LocalServer server) {
        List<SourceFixture> contacts = new ArrayList<>();
        for (LocalServer.Request request : server.requests) {
            SourceFixture source;
            if (request.method().equals("POST") && request.path().equals("/")) {
                source = SourceFixture.WEB_IDENTITY;
            } else if (request.path().equals("/creds")) {
                source = SourceFixture.CONTAINER;
            } else if (request.path().startsWith("/latest/")) {
                source = SourceFixture.INSTANCE_PROFILE;
            } else {
                throw new AssertionError("Unexpected request " + request.method() + " " + request.path());
            }
            if (contacts.isEmpty() || contacts.get(contacts.size() - 1) != source) {
                contacts.add(source);
            }
        }
        return contacts;
    }

    private static String stsCredentials(String accessKeyId) {
        return "<AssumeRoleWithWebIdentityResponse xmlns=\"https://sts.amazonaws.com/doc/2011-06-15/\">"
               + "<AssumeRoleWithWebIdentityResult><Credentials><AccessKeyId>" + accessKeyId + "</AccessKeyId>"
               + "<SecretAccessKey>S</SecretAccessKey><SessionToken>T</SessionToken><Expiration>"
               + Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS) + "</Expiration>"
               + "</Credentials><AssumedRoleUser><Arn>arn:aws:sts::123456789012:assumed-role/r/s</Arn>"
               + "</AssumedRoleUser></AssumeRoleWithWebIdentityResult></AssumeRoleWithWebIdentityResponse>";
    }

    /** The Parity Manifest precedence: one source ID per line, blank lines and {@code #} comments ignored. */
    private static List<String> manifestPrecedence() {
        InputStream in = AuthAndIdentityPrecedencePropertyTest.class.getClassLoader()
                                                                    .getResourceAsStream(PRECEDENCE_MANIFEST);
        assertNotNull(in, PRECEDENCE_MANIFEST);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().map(String::trim).filter(l -> !l.isEmpty() && !l.startsWith("#")).toList();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static String write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            return Files.writeString(file, content).toString();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    /**
     * The test's transport: the real URLConnection client, with every request sent to the local server (so the STS
     * regional endpoint of the web identity source resolves locally). Caller-owned; closing it is a no-op.
     */
    private record LoopbackTransport(SdkSyncHttpClient delegate, int port) implements SdkSyncHttpClient {
        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return delegate.prepare(request.toBuilder().scheme("http").host("127.0.0.1").port(port).build(), options);
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return delegate.capabilities();
        }

        @Override
        public String clientName() {
            return "LoopbackTransport";
        }

        @Override
        public void close() {
        }
    }
}
