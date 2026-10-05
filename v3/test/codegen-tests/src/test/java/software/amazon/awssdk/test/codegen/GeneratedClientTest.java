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

package software.amazon.awssdk.test.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Requirements 7.1-7.6, 7.11, 7.12, 8.1-8.3, 8.13 and 17.1: a synthetic awsJson1_0 service gets V2-shaped
 * {@code <Service>Client} and {@code <Service>AsyncClient} interfaces, builders and default implementations. A probe
 * written against the generated public API (so its overloads are checked by {@code javac}) builds both clients over a
 * fake echoing transport and calls every operation through the typed, consumer and no-argument overloads, maps a
 * modeled error, reads the resolved configuration, and runs overlapping calls from many threads.
 */
class GeneratedClientTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.gadgets";

    private static final String MODEL = """
        $version: "2"
        namespace example.gadgets

        @aws.protocols#awsJson1_0
        @aws.auth#sigv4(name: "gadgets")
        @aws.api#service(sdkId: "Gadgets", endpointPrefix: "gadgets")
        service Gadgets {
            version: "2024-01-01"
            operations: [GetGadget, ListGadgets, Ping, SignGadget]
        }

        /// The service's SigV4 auth applies.
        operation SignGadget {
            input := {
                name: String
            }
            output := {
                name: String
            }
        }

        @auth([])
        @readonly
        operation GetGadget {
            input := {
                @required
                name: String
            }
            output := {
                name: String
                size: Integer
            }
            errors: [GadgetNotFound]
        }

        @auth([])
        @readonly
        operation ListGadgets {
            input := {
                name: String
            }
            output := {
                name: String
            }
        }

        @auth([])
        operation Ping {
            output := {
                status: String
            }
        }

        @error("client")
        @httpError(404)
        structure GadgetNotFound {
            message: String
        }
        """;

    /** Compiled with the generated sources against their public API only (a different package). */
    private static final String PROBE = """
        package example.probe;

        import java.net.URI;
        import java.util.ArrayList;
        import java.util.List;
        import java.util.concurrent.CompletableFuture;
        import java.util.concurrent.CompletionException;
        import java.util.concurrent.ExecutorService;
        import java.util.concurrent.Executors;
        import java.util.concurrent.Future;
        import software.amazon.awssdk.core.client.config.SdkClientOption;
        import software.amazon.awssdk.http.SdkAsyncHttpClient;
        import software.amazon.awssdk.http.SdkSyncHttpClient;
        import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
        import software.amazon.awssdk.identity.spi.IdentityProvider;
        import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
        import software.amazon.awssdk.regions.Region;
        import PKG.GadgetsAsyncClient;
        import PKG.GadgetsClient;
        import PKG.model.GadgetNotFoundException;
        import PKG.model.GetGadgetRequest;

        public final class ClientProbe {
            private static final URI ENDPOINT = URI.create("https://gadgets.test");

            private ClientProbe() {
            }

            public static String sync(SdkSyncHttpClient http) throws Exception {
                try (GadgetsClient client = GadgetsClient.builder().region(Region.of("us-east-1"))
                                                         .endpointOverride(ENDPOINT).httpClient(http).build()) {
                    String typed = client.getGadget(GetGadgetRequest.builder().name("a").build()).name();
                    String consumer = client.getGadget(r -> r.name("b")).name();
                    String noArgument = String.valueOf(client.listGadgets().name());
                    String unit = String.valueOf(client.ping().status());
                    String error;
                    try {
                        client.getGadget(r -> r.name("missing"));
                        error = "none";
                    } catch (GadgetNotFoundException e) {
                        error = e.getClass().getSimpleName();
                    }
                    ExecutorService pool = Executors.newFixedThreadPool(8);
                    int mismatches = 0;
                    try {
                        List<Future<Boolean>> calls = new ArrayList<>();
                        for (int i = 0; i < 64; i++) {
                            String name = "n" + i;
                            calls.add(pool.submit(() -> name.equals(client.getGadget(r -> r.name(name)).name())));
                        }
                        for (Future<Boolean> call : calls) {
                            mismatches += call.get() ? 0 : 1;
                        }
                    } finally {
                        pool.shutdownNow();
                    }
                    return String.join(",", typed, consumer, noArgument, unit, error, client.serviceName(),
                                       String.valueOf(client.serviceClientConfiguration()
                                                            .get(SdkClientOption.CLIENT_ENDPOINT).orElse(null)),
                                       "mismatches=" + mismatches);
                }
            }

            /** Static test credentials, implemented against the identity SPI only. */
            static final IdentityProvider<AwsCredentialsIdentity> CREDENTIALS =
                new IdentityProvider<AwsCredentialsIdentity>() {
                    @Override
                    public Class<AwsCredentialsIdentity> identityType() {
                        return AwsCredentialsIdentity.class;
                    }

                    @Override
                    public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest r) {
                        return CompletableFuture.completedFuture(
                            AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"));
                    }
                };

            public static String signedSync(SdkSyncHttpClient http) throws Exception {
                try (GadgetsClient client = GadgetsClient.builder().region(Region.of("us-west-2"))
                                                         .credentialsProvider(CREDENTIALS)
                                                         .endpointOverride(ENDPOINT).httpClient(http).build()) {
                    return client.signGadget(r -> r.name("s")).name() + "," + client.getGadget(r -> r.name("a")).name();
                }
            }

            /** Without a credentials provider: the default chain. */
            public static String defaultChainSync(SdkSyncHttpClient http) throws Exception {
                try (GadgetsClient client = GadgetsClient.builder().region(Region.of("us-west-2"))
                                                         .endpointOverride(ENDPOINT).httpClient(http).build()) {
                    return client.signGadget(r -> r.name("s")).name();
                }
            }

            public static String signedAsync(SdkAsyncHttpClient http) throws Exception {
                try (GadgetsAsyncClient client = GadgetsAsyncClient.builder().region(Region.of("us-west-2"))
                                                                   .credentialsProvider(CREDENTIALS)
                                                                   .endpointOverride(ENDPOINT).httpClient(http)
                                                                   .build()) {
                    return client.signGadget(r -> r.name("s")).join().name() + ","
                           + client.getGadget(r -> r.name("a")).join().name();
                }
            }

            public static String async(SdkAsyncHttpClient http) throws Exception {
                try (GadgetsAsyncClient client = GadgetsAsyncClient.builder().region(Region.of("us-east-1"))
                                                                   .endpointOverride(ENDPOINT).httpClient(http)
                                                                   .build()) {
                    String typed = client.getGadget(GetGadgetRequest.builder().name("a").build()).join().name();
                    String consumer = client.getGadget(r -> r.name("b")).join().name();
                    String noArgument = String.valueOf(client.listGadgets().join().name());
                    String unit = String.valueOf(client.ping().join().status());
                    String error;
                    try {
                        client.getGadget(r -> r.name("missing")).join();
                        error = "none";
                    } catch (CompletionException e) {
                        error = e.getCause().getClass().getSimpleName();
                    }
                    List<CompletableFuture<Boolean>> calls = new ArrayList<>();
                    for (int i = 0; i < 64; i++) {
                        String name = "n" + i;
                        calls.add(client.getGadget(r -> r.name(name)).thenApply(o -> name.equals(o.name())));
                    }
                    int mismatches = 0;
                    for (CompletableFuture<Boolean> call : calls) {
                        mismatches += call.join() ? 0 : 1;
                    }
                    return String.join(",", typed, consumer, noArgument, unit, error, client.serviceName(),
                                       String.valueOf(client.serviceClientConfiguration()
                                                            .get(SdkClientOption.CLIENT_ENDPOINT).orElse(null)),
                                       "mismatches=" + mismatches);
                }
            }
        }
        """.replace("PKG", PACKAGE);

    private static final String EXPECTED = "a,b,null,null,GadgetNotFoundException,gadgets,https://gadgets.test,"
                                           + "mismatches=0";

    @TempDir
    static Path temp;

    private static AwsCodegenResult result;
    private static URLClassLoader loader;

    @BeforeAll
    static void generateAndCompile() throws Exception {
        Path models = Files.createDirectories(temp.resolve("model"));
        Files.writeString(models.resolve("gadgets.smithy"), MODEL, StandardCharsets.UTF_8);
        result = AwsCodegen.generate(new AwsCodegenRequest("example.gadgets#Gadgets", PACKAGE, models,
                                                           temp.resolve("out")), List.of());
        Path sources = temp.resolve("sources");
        copyTree(result.sourceRoot(), sources);
        Path probe = Files.createDirectories(sources.resolve("example/probe")).resolve("ClientProbe.java");
        Files.writeString(probe, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(sources, temp.resolve("classes")).loader();
    }

    @AfterAll
    static void close() throws Exception {
        loader.close();
    }

    @Test
    void theClientTypesAreGeneratedInTheServicePackage() {
        String root = PACKAGE.replace('.', '/') + "/";
        for (String file : List.of("GadgetsClient.java", "GadgetsAsyncClient.java", "GadgetsClientBuilder.java",
                                   "GadgetsAsyncClientBuilder.java", "DefaultGadgetsClient.java",
                                   "DefaultGadgetsAsyncClient.java", "DefaultGadgetsClientBuilder.java",
                                   "DefaultGadgetsAsyncClientBuilder.java", "GadgetsOperationBindings.java")) {
            assertTrue(result.generatedFiles().stream().anyMatch(p -> p.toString().replace('\\', '/')
                                                                         .equals(root + file)),
                       root + file + " was not generated: " + result.generatedFiles());
        }
    }

    @Test
    void theSyncClientHasTheV2ShapedSurface() throws Exception {
        Class<?> client = Class.forName(PACKAGE + ".GadgetsClient", false, loader);
        Class<?> request = Class.forName(PACKAGE + ".model.GetGadgetRequest", false, loader);
        Class<?> response = Class.forName(PACKAGE + ".model.GetGadgetResponse", false, loader);
        assertTrue(client.isInterface() && Modifier.isPublic(client.getModifiers()));
        assertTrue(AutoCloseable.class.isAssignableFrom(client));
        assertTrue(Modifier.isStatic(client.getMethod("create").getModifiers()));
        assertTrue(Modifier.isStatic(client.getMethod("builder").getModifiers()));
        assertEquals(response, client.getMethod("getGadget", request).getReturnType());
        assertEquals(response, client.getMethod("getGadget", Consumer.class).getReturnType());
        assertEquals(response, client.getMethod("getGadget", request, AwsRequestOverrideConfiguration.class)
                                     .getReturnType());
        // V2: the modeled errors, AwsServiceException, SdkClientException and the service exception are declared.
        List<String> declared = Arrays.stream(client.getMethod("getGadget", request).getExceptionTypes())
                                      .map(Class::getSimpleName).toList();
        assertEquals(List.of("GadgetNotFoundException", "AwsServiceException", "SdkClientException",
                             "GadgetsException"), declared);
        // No required input members: a no-argument overload; GetGadget has a required member: none.
        client.getMethod("listGadgets");
        assertFalse(Arrays.stream(client.getMethods())
                          .anyMatch(m -> m.getName().equals("getGadget") && m.getParameterCount() == 0));
        assertEquals(String.class, client.getMethod("serviceName").getReturnType());
        // The default implementation is not public API.
        Class<?> implementation = Class.forName(PACKAGE + ".DefaultGadgetsClient", false, loader);
        assertFalse(Modifier.isPublic(implementation.getModifiers()));
    }

    @Test
    void theAsyncClientReturnsFutures() throws Exception {
        Class<?> client = Class.forName(PACKAGE + ".GadgetsAsyncClient", false, loader);
        Class<?> request = Class.forName(PACKAGE + ".model.GetGadgetRequest", false, loader);
        assertEquals("java.util.concurrent.CompletableFuture",
                     client.getMethod("getGadget", request).getReturnType().getName());
        assertEquals(0, client.getMethod("getGadget", request).getExceptionTypes().length);
    }

    @Test
    void theDefaultClientHoldsOneFinalPlanFieldPerOperation() throws Exception {
        Class<?> implementation = Class.forName(PACKAGE + ".DefaultGadgetsClient", false, loader);
        List<String> plans = Arrays.stream(implementation.getDeclaredFields())
                                   .filter(f -> f.getType().getSimpleName().equals("PipelinePlan"))
                                   .peek(f -> assertTrue(Modifier.isFinal(f.getModifiers()), f.getName()))
                                   .map(java.lang.reflect.Field::getName).sorted().toList();
        assertEquals(List.of("getGadgetPlan", "listGadgetsPlan", "pingPlan", "signGadgetPlan"), plans);
    }

    @Test
    void theSyncClientCallsEveryOperationThroughTheEngine() throws Exception {
        EchoHttp http = new EchoHttp(HttpTransportMode.SYNC);
        assertEquals(EXPECTED, probe("sync", SdkSyncHttpClient.class, http));
        assertEquals(5 + 64, http.requests.size());
        assertTrue(http.requests.stream().allMatch(r -> "gadgets.test".equals(r.host())));
        assertFalse(http.closed, "a caller-owned HTTP client is not closed by the SDK client");
    }

    @Test
    void theAsyncClientCallsEveryOperationThroughTheEngine() throws Exception {
        EchoHttp http = new EchoHttp(HttpTransportMode.ASYNC);
        assertEquals(EXPECTED, probe("async", SdkAsyncHttpClient.class, http));
        assertEquals(5 + 64, http.requests.size());
        assertFalse(http.closed, "a caller-owned HTTP client is not closed by the SDK client");
    }

    /** Requirements 13.1 and 13.2: a SigV4 operation is signed with the model's signing name and client region. */
    @Test
    void sigV4OperationsAreSignedWithTheConfiguredCredentials() throws Exception {
        EchoHttp syncHttp = new EchoHttp(HttpTransportMode.SYNC);
        assertEquals("s,a", probe("signedSync", SdkSyncHttpClient.class, syncHttp));
        EchoHttp asyncHttp = new EchoHttp(HttpTransportMode.ASYNC);
        assertEquals("s,a", probe("signedAsync", SdkAsyncHttpClient.class, asyncHttp));
        for (EchoHttp http : List.of(syncHttp, asyncHttp)) {
            assertEquals(2, http.requests.size());
            String signed = http.requests.get(0).headers().firstValue("Authorization").orElse(null);
            assertTrue(signed != null && signed.startsWith("AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/")
                       && signed.contains("/us-west-2/gadgets/aws4_request") && signed.contains("Signature="),
                       "SigV4 Authorization header: " + signed);
            assertTrue(http.requests.get(0).headers().contains("X-Amz-Date"));
            assertFalse(http.requests.get(1).headers().contains("Authorization"),
                        "an @auth([]) operation is not signed");
        }
    }

    /**
     * Without {@code credentialsProvider}, a SigV4 client signs with the default credentials chain. The generated
     * builder exposes no configuration sources, so the chain's first source, the JVM properties, supplies the
     * credentials: they are set for this test and restored after it. The chain stops at the first source that
     * resolves, so neither the environment nor the profile file is read.
     */
    @Test
    void sigV4OperationsAreSignedWithTheDefaultCredentialsChain() throws Exception {
        String previousKey = System.getProperty("aws.accessKeyId");
        String previousSecret = System.getProperty("aws.secretAccessKey");
        EchoHttp http = new EchoHttp(HttpTransportMode.SYNC);
        try {
            System.setProperty("aws.accessKeyId", "AKIDDEFAULTCHAIN");
            System.setProperty("aws.secretAccessKey", "defaultChainSecretKey");
            assertEquals("s", probe("defaultChainSync", SdkSyncHttpClient.class, http));
        } finally {
            restore("aws.accessKeyId", previousKey);
            restore("aws.secretAccessKey", previousSecret);
        }
        assertEquals(1, http.requests.size());
        String signed = http.requests.get(0).headers().firstValue("Authorization").orElse(null);
        assertTrue(signed != null && signed.startsWith("AWS4-HMAC-SHA256 Credential=AKIDDEFAULTCHAIN/")
                   && signed.contains("/us-west-2/gadgets/aws4_request") && signed.contains("Signature="),
                   "SigV4 Authorization header: " + signed);
    }

    /** The generated builder of a SigV4 service defaults to the DefaultCredentialsProvider chain. */
    @Test
    void theDefaultBuilderOfASigV4ServiceDefaultsToTheCredentialsChain() throws Exception {
        String builder = Files.readString(result.sourceRoot().resolve(PACKAGE.replace('.', '/'))
                                                .resolve("DefaultGadgetsClientBuilder.java"));
        assertTrue(builder.contains(".withDefaultCredentials(sources -> DefaultCredentialsProvider.builder()"),
                   builder);
        assertTrue(builder.contains("import software.amazon.awssdk.identity.providers.DefaultCredentialsProvider;"),
                   builder);
    }

    private static void restore(String property, String previous) {
        if (previous == null) {
            System.clearProperty(property);
        } else {
            System.setProperty(property, previous);
        }
    }

    private static String probe(String method, Class<?> parameter, Object http) throws Exception {
        Method probe = Class.forName("example.probe.ClientProbe", true, loader).getMethod(method, parameter);
        try {
            return (String) probe.invoke(null, http);
        } catch (InvocationTargetException e) {
            throw new AssertionError("ClientProbe." + method + " failed", e.getCause());
        }
    }

    private static void copyTree(Path from, Path to) throws Exception {
        try (var walk = Files.walk(from)) {
            for (Path source : walk.toList()) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
    }

    /**
     * A sync and async transport that answers every request with its own JSON body (so outputs echo inputs member
     * by member), or with the modeled {@code GadgetNotFound} error when the request names {@code missing}.
     */
    static final class EchoHttp implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
        private final HttpTransportMode mode;
        volatile boolean closed;

        EchoHttp(HttpTransportMode mode) {
            this.mode = mode;
        }

        private SdkHttpResponse respond(SdkHttpRequest request) {
            requests.add(request);
            ByteBuffer buffer = request.body().buffered().asByteBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            String body = new String(bytes, StandardCharsets.UTF_8);
            if (body.contains("\"missing\"")) {
                return SdkHttpResponse.builder().statusCode(404)
                                      .body(HttpBody.fromUtf8("{\"__type\":\"example.gadgets#GadgetNotFound\","
                                                              + "\"message\":\"no such gadget\"}"))
                                      .build();
            }
            return SdkHttpResponse.builder().statusCode(200).body(HttpBody.fromUtf8(body.isEmpty() ? "{}" : body))
                                  .build();
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    return respond(request);
                }

                @Override
                public void abort() {
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public AsyncHttpExchange execute(SdkHttpRequest request, AsyncHttpResponseHandler handler,
                                         HttpExecutionOptions options) {
            SdkHttpResponse response = respond(request);
            AsyncExchangeCompletion exchange =
                new AsyncExchangeCompletion(clientName(), (reason, failure) -> handler.onError(failure));
            ByteBuffer body = response.body().buffered().asByteBuffer();
            handler.onHeaders(response.toBuilder().body(HttpBody.empty()).build());
            handler.onStream(publisherOf(body));
            exchange.complete();
            return exchange;
        }

        @Override
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(mode).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "EchoHttp";
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static Publisher<ByteBuffer> publisherOf(ByteBuffer bytes) {
        return subscriber -> subscriber.onSubscribe(new Subscription() {
            private boolean done;

            @Override
            public void request(long n) {
                if (done) {
                    return;
                }
                done = true;
                if (bytes.hasRemaining()) {
                    subscriber.onNext(bytes.asReadOnlyBuffer());
                }
                subscriber.onComplete();
            }

            @Override
            public void cancel() {
                done = true;
            }
        });
    }
}
