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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.awscodegen.AwsCodegen;
import software.amazon.awssdk.awscodegen.AwsCodegenRequest;
import software.amazon.awssdk.awscodegen.AwsCodegenResult;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.resource.ManagedResource;
import software.amazon.awssdk.core.resource.ResourceOwnership;
import software.amazon.awssdk.http.AsyncExchangeCompletion;
import software.amazon.awssdk.http.AsyncHttpExchange;
import software.amazon.awssdk.http.AsyncHttpResponseHandler;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpQueryParameter;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkAsyncHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.pipeline.ExecutionInterceptor;
import software.amazon.awssdk.pipeline.HookContext;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.AcquireInitialTokenResponse;
import software.amazon.awssdk.retries.AttemptFailure;
import software.amazon.awssdk.retries.RetryDecision;
import software.amazon.awssdk.retries.RetryStrategy;
import software.amazon.awssdk.retries.RetryToken;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.test.codegen.support.CompiledSources;

/**
 * Property 20 (Requirements 7.9, 7.11): derived clients share the exact resolved configuration.
 *
 * <p>One synthetic restXml service with S3's shape ID and S3-like endpoint rules, generated and compiled once, gets
 * the sync and async clients, a {@code @paginated} {@code ListObjectsV2}, a {@code @waitable} {@code GetBucketStatus}
 * ({@code BucketReady}), {@code S3Utilities} and {@code S3Presigner}. Each try generates a client configuration:
 * region, endpoint override (or none), a distinct closeable caller-owned credentials provider, a distinct counting
 * retry strategy, one to three recording interceptors, a virtual-hostable bucket, the page count, the polls before the
 * bucket is ready, and whether the async completion executor is caller-owned or SDK-owned. The HTTP clients are
 * caller-owned fakes (the test classpath has no discoverable SDK HTTP implementation).
 *
 * <p>For the sync and the async client, each derived surface runs: {@code client.waiter()} (closed), the paginator,
 * a second {@code client.waiter()} (closed; the async one schedules on the client's own retry scheduler), and, sync,
 * {@code client.utilities().getUrl}. The oracle is the client's own {@code serviceClientConfiguration()}:
 * <ul>
 *   <li>identity: every signed call of the paginator and of every waiter poll reaches every interceptor with
 *   {@code options().clientConfiguration()} the same instance the client returns, which is unchanged afterwards; the
 *   configuration holds the try's region, endpoint override and credentials provider instance; every call acquires
 *   its initial token from the try's retry strategy instance;</li>
 *   <li>decisions: every transmitted request goes to the host the endpoint rules give the bucket, with a SigV4
 *   credential scope of the try's access key and region; the utility URL and the presigned URL have the same host,
 *   and the presigned credential the same key and region;</li>
 *   <li>ownership: closing the waiters leaves the client usable; closing the client leaves the caller's HTTP client,
 *   executor and credentials provider open and recorded as caller-owned, and shuts down an SDK-owned executor.</li>
 * </ul>
 * The presigner is built from its own builder ({@code S3Presigner.builder()} takes no client), so it is not documented
 * as sharing the client's configuration object: it is checked for equal values and decisions, not identity. The
 * utilities have no {@code close()}.
 */
class DerivedClientConfigurationPropertyTest {
    private static final String PACKAGE = "software.amazon.awssdk.test.codegen.derived";

    private static final String MODEL = """
        $version: "2"
        namespace com.amazonaws.s3

        use smithy.waiters#waitable

        @aws.protocols#restXml(noErrorWrapping: true)
        @aws.auth#sigv4(name: "s3")
        @aws.api#service(sdkId: "S3", endpointPrefix: "s3")
        @smithy.rules#endpointRuleSet({
            version: "1.0"
            parameters: {
                Bucket: { type: "String", required: false, documentation: "The bucket." }
                Region: { type: "String", builtIn: "AWS::Region", required: true, documentation: "The region." }
                Endpoint: { type: "String", builtIn: "SDK::Endpoint", required: false, documentation: "Override." }
                ForcePathStyle: {
                    type: "Boolean", builtIn: "AWS::S3::ForcePathStyle", required: true, default: false,
                    documentation: "Path-style addressing."
                }
            }
            rules: [
                {
                    conditions: [
                        { fn: "isSet", argv: [{ ref: "Bucket" }] }
                        { fn: "isSet", argv: [{ ref: "Endpoint" }] }
                        { fn: "booleanEquals", argv: [{ ref: "ForcePathStyle" }, true] }
                    ]
                    endpoint: { url: "{Endpoint}/{Bucket}" }
                    type: "endpoint"
                }
                {
                    conditions: [
                        { fn: "isSet", argv: [{ ref: "Bucket" }] }
                        { fn: "isSet", argv: [{ ref: "Endpoint" }] }
                        { fn: "parseURL", argv: [{ ref: "Endpoint" }], assign: "url" }
                    ]
                    endpoint: { url: "{url#scheme}://{Bucket}.{url#authority}{url#path}" }
                    type: "endpoint"
                }
                {
                    conditions: [
                        { fn: "isSet", argv: [{ ref: "Bucket" }] }
                        { fn: "booleanEquals", argv: [{ ref: "ForcePathStyle" }, false] }
                        { fn: "aws.isVirtualHostableS3Bucket", argv: [{ ref: "Bucket" }, false] }
                    ]
                    endpoint: { url: "https://{Bucket}.s3.{Region}.amazonaws.com" }
                    type: "endpoint"
                }
                {
                    conditions: [{ fn: "isSet", argv: [{ ref: "Bucket" }] }]
                    endpoint: { url: "https://s3.{Region}.amazonaws.com/{Bucket}" }
                    type: "endpoint"
                }
                {
                    conditions: []
                    endpoint: { url: "https://s3.{Region}.amazonaws.com" }
                    type: "endpoint"
                }
            ]
        })
        service AmazonS3 {
            version: "2006-03-01"
            operations: [GetObject, ListObjectsV2, GetBucketStatus]
        }

        @readonly
        @http(method: "GET", uri: "/{Bucket}/{Key+}")
        operation GetObject {
            input := {
                @required
                @httpLabel
                @smithy.rules#contextParam(name: "Bucket")
                Bucket: String

                @required
                @httpLabel
                Key: String
            }
            output := {}
        }

        @readonly
        @paginated(inputToken: "ContinuationToken", outputToken: "NextContinuationToken")
        @http(method: "GET", uri: "/{Bucket}")
        operation ListObjectsV2 {
            input := {
                @required
                @httpLabel
                @smithy.rules#contextParam(name: "Bucket")
                Bucket: String

                @httpQuery("continuation-token")
                ContinuationToken: String
            }
            output := {
                NextContinuationToken: String
                KeyCount: Integer
            }
        }

        @readonly
        @waitable(
            BucketReady: {
                minDelay: 1
                acceptors: [
                    {
                        state: "success"
                        matcher: { output: { path: "Status", expected: "READY", comparator: "stringEquals" } }
                    }
                    {
                        state: "retry"
                        matcher: { output: { path: "Status", expected: "PENDING", comparator: "stringEquals" } }
                    }
                ]
            }
        )
        @http(method: "HEAD", uri: "/{Bucket}")
        operation GetBucketStatus {
            input := {
                @required
                @httpLabel
                @smithy.rules#contextParam(name: "Bucket")
                Bucket: String
            }
            output := {
                @httpHeader("x-amz-bucket-status")
                Status: String
            }
        }
        """;

    private static final String PROBE = """
        package example.probe;

        import java.net.URI;
        import java.time.Duration;
        import java.util.LinkedHashMap;
        import java.util.List;
        import java.util.Map;
        import java.util.concurrent.CompletableFuture;
        import java.util.concurrent.Executor;
        import java.util.concurrent.TimeUnit;
        import java.util.concurrent.atomic.AtomicInteger;
        import org.reactivestreams.Subscriber;
        import org.reactivestreams.Subscription;
        import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
        import software.amazon.awssdk.http.SdkAsyncHttpClient;
        import software.amazon.awssdk.http.SdkSyncHttpClient;
        import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
        import software.amazon.awssdk.identity.spi.IdentityProvider;
        import software.amazon.awssdk.pipeline.ExecutionInterceptor;
        import software.amazon.awssdk.regions.Region;
        import software.amazon.awssdk.retries.RetryStrategy;
        import PKG.S3AsyncClient;
        import PKG.S3AsyncClientBuilder;
        import PKG.S3Client;
        import PKG.S3ClientBuilder;
        import PKG.model.GetBucketStatusRequest;
        import PKG.model.ListObjectsV2Request;
        import PKG.model.ListObjectsV2Response;
        import PKG.presigner.S3Presigner;
        import PKG.waiters.S3AsyncWaiter;
        import PKG.waiters.S3Waiter;

        public final class Probe {
            private static final WaiterOverrideConfiguration FAST = WaiterOverrideConfiguration.builder()
                .minDelay(Duration.ofMillis(1)).maxDelay(Duration.ofMillis(3)).waitTimeout(Duration.ofSeconds(10))
                .build();

            private Probe() {
            }

            /** Waiter (closed), paginator, waiter (closed) and utilities of one sync client, which it then closes. */
            public static Map<String, Object> sync(Region region, URI endpoint,
                                                   IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                                   RetryStrategy retry, List<ExecutionInterceptor> interceptors,
                                                   SdkSyncHttpClient http, String bucket) {
                S3ClientBuilder builder = S3Client.builder().region(region).credentialsProvider(credentials)
                                                  .retryStrategy(retry).httpClient(http);
                if (endpoint != null) {
                    builder.endpointOverride(endpoint);
                }
                interceptors.forEach(builder::addExecutionInterceptor);
                Map<String, Object> out = new LinkedHashMap<>();
                GetBucketStatusRequest status = GetBucketStatusRequest.builder().bucket(bucket).build();
                try (S3Client client = builder.build()) {
                    out.put("configuration", client.serviceClientConfiguration());
                    try (S3Waiter waiter = client.waiter()) {
                        out.put("attempts1", waiter.waitUntilBucketReady(status, FAST).attemptsExecuted());
                    }
                    int pages = 0;
                    for (ListObjectsV2Response page
                        : client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(bucket).build())) {
                        pages++;
                    }
                    out.put("pages", pages);
                    try (S3Waiter waiter = client.waiter()) {
                        out.put("attempts2", waiter.waitUntilBucketReady(status, FAST).attemptsExecuted());
                    }
                    out.put("url", client.utilities().getUrl(r -> r.bucket(bucket).key("k")).toString());
                    out.put("configurationAfter", client.serviceClientConfiguration());
                }
                return out;
            }

            /** Waiter (closed), paginator and waiter (closed) of one async client, which it then closes. */
            public static Map<String, Object> async(Region region, URI endpoint,
                                                    IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                                    RetryStrategy retry, List<ExecutionInterceptor> interceptors,
                                                    SdkAsyncHttpClient http, Executor executor, String bucket)
                throws Exception {
                S3AsyncClientBuilder builder = S3AsyncClient.builder().region(region).credentialsProvider(credentials)
                                                            .retryStrategy(retry).httpClient(http);
                if (endpoint != null) {
                    builder.endpointOverride(endpoint);
                }
                if (executor != null) {
                    builder.asyncCompletionExecutor(executor);
                }
                interceptors.forEach(builder::addExecutionInterceptor);
                Map<String, Object> out = new LinkedHashMap<>();
                GetBucketStatusRequest status = GetBucketStatusRequest.builder().bucket(bucket).build();
                try (S3AsyncClient client = builder.build()) {
                    out.put("configuration", client.serviceClientConfiguration());
                    try (S3AsyncWaiter waiter = client.waiter()) {
                        out.put("attempts1", waiter.waitUntilBucketReady(status, FAST).get(10, TimeUnit.SECONDS)
                                                   .attemptsExecuted());
                    }
                    CompletableFuture<Integer> done = new CompletableFuture<>();
                    AtomicInteger pages = new AtomicInteger();
                    client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(bucket).build())
                          .subscribe(new Subscriber<ListObjectsV2Response>() {
                              @Override
                              public void onSubscribe(Subscription s) {
                                  s.request(Long.MAX_VALUE);
                              }

                              @Override
                              public void onNext(ListObjectsV2Response page) {
                                  pages.incrementAndGet();
                              }

                              @Override
                              public void onError(Throwable t) {
                                  done.completeExceptionally(t);
                              }

                              @Override
                              public void onComplete() {
                                  done.complete(pages.get());
                              }
                          });
                    out.put("pages", done.get(10, TimeUnit.SECONDS));
                    try (S3AsyncWaiter waiter = client.waiter()) {
                        out.put("attempts2", waiter.waitUntilBucketReady(status, FAST).get(10, TimeUnit.SECONDS)
                                                   .attemptsExecuted());
                    }
                    out.put("configurationAfter", client.serviceClientConfiguration());
                }
                return out;
            }

            /** The URL of a presigned GetObject from a presigner built with the client's settings. */
            public static String presign(Region region, URI endpoint,
                                         IdentityProvider<? extends AwsCredentialsIdentity> credentials,
                                         List<ExecutionInterceptor> interceptors, String bucket) {
                S3Presigner.Builder builder = S3Presigner.builder().region(region).credentialsProvider(credentials);
                if (endpoint != null) {
                    builder.endpointOverride(endpoint);
                }
                interceptors.forEach(builder::addExecutionInterceptor);
                try (S3Presigner presigner = builder.build()) {
                    return presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                                            .getObjectRequest(g -> g.bucket(bucket).key("k")))
                                    .url().toString();
                }
            }
        }
        """.replace("PKG", PACKAGE);

    private static final Pattern AUTHORIZATION =
        Pattern.compile("Credential=([^/]+)/\\d{8}/([^/]+)/s3/aws4_request");
    private static final Pattern PRESIGNED_CREDENTIAL =
        Pattern.compile("X-Amz-Credential=([^%&]+)%2F\\d{8}%2F([^%&]+)%2Fs3%2Faws4_request");

    private static URLClassLoader loader;
    private static Method sync;
    private static Method async;
    private static Method presign;

    @BeforeContainer
    static void generateAndCompile() throws Exception {
        Path work = Files.createTempDirectory(Files.createDirectories(Path.of("target", "derived-configuration")),
                                              "fixture-");
        Path models = Files.createDirectories(work.resolve("model"));
        Files.writeString(models.resolve("s3like.smithy"), MODEL, StandardCharsets.UTF_8);
        AwsCodegenResult result = AwsCodegen.generate(
            new AwsCodegenRequest("com.amazonaws.s3#AmazonS3", PACKAGE, models, work.resolve("out")), List.of());
        Path probe = Files.createDirectories(result.sourceRoot().resolve("example/probe")).resolve("Probe.java");
        Files.writeString(probe, PROBE, StandardCharsets.UTF_8);
        loader = CompiledSources.compile(result.sourceRoot(), work.resolve("classes")).loader();
        Class<?> type = Class.forName("example.probe.Probe", true, loader);
        sync = type.getMethod("sync", Region.class, URI.class, IdentityProvider.class, RetryStrategy.class,
                              List.class, SdkSyncHttpClient.class, String.class);
        async = type.getMethod("async", Region.class, URI.class, IdentityProvider.class, RetryStrategy.class,
                               List.class, SdkAsyncHttpClient.class, Executor.class, String.class);
        presign = type.getMethod("presign", Region.class, URI.class, IdentityProvider.class, List.class,
                                 String.class);
    }

    @AfterContainer
    static void close() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    @Property(tries = 150, edgeCases = EdgeCasesMode.NONE)
    void derivedClientsShareTheClientsExactConfiguration(@ForAll("scenarios") Scenario scenario) throws Exception {
        Credentials credentials = new Credentials(scenario.accessKey());
        List<Recorder> recorders = IntStream.range(0, scenario.interceptors()).mapToObj(i -> new Recorder()).toList();
        List<ExecutionInterceptor> interceptors = List.copyOf(recorders);
        Region region = Region.of(scenario.region());
        URI endpoint = scenario.endpoint().map(URI::create).orElse(null);
        int calls = 2 * (scenario.pendingPolls() + 1) + scenario.pages();

        // Sync client: waiter, paginator, waiter, utilities.
        FakeS3 syncHttp = new FakeS3(HttpTransportMode.SYNC, scenario);
        CountingRetry syncRetry = new CountingRetry();
        Map<String, Object> syncOut = invoke(syncHttp, sync, region, endpoint, credentials, syncRetry, interceptors,
                                             syncHttp, scenario.bucket());
        SdkClientConfiguration syncConfiguration = (SdkClientConfiguration) syncOut.get("configuration");
        assertDerivedCalls("sync", scenario, syncOut, syncConfiguration, syncHttp, syncRetry, recorders, calls,
                           credentials);
        URI url = URI.create((String) syncOut.get("url"));
        assertEquals(scenario.expectedHost(), url.getHost(), "utilities host");
        assertEquals(scenario.endpoint().map(e -> URI.create(e).getPort()).orElse(-1), url.getPort(),
                     "utilities port");
        assertTrue(url.getPath().endsWith("/k"), url.toString());
        assertTrue(syncConfiguration.resources().contains(ManagedResource.callerOwned(syncHttp)),
                   "the caller's sync HTTP client is recorded as caller-owned: " + syncConfiguration.resources());
        assertFalse(syncHttp.closed, "closing the sync client leaves the caller's HTTP client open");
        recorders.forEach(r -> r.seen.clear());

        // Async client: waiter, paginator, waiter; the completion executor is caller- or SDK-owned.
        FakeS3 asyncHttp = new FakeS3(HttpTransportMode.ASYNC, scenario);
        CountingRetry asyncRetry = new CountingRetry();
        ExecutorService callerExecutor = scenario.callerOwnedExecutor() ? Executors.newFixedThreadPool(2) : null;
        try {
            Map<String, Object> asyncOut = invoke(asyncHttp, async, region, endpoint, credentials, asyncRetry,
                                                  interceptors, asyncHttp, callerExecutor, scenario.bucket());
            SdkClientConfiguration asyncConfiguration = (SdkClientConfiguration) asyncOut.get("configuration");
            assertDerivedCalls("async", scenario, asyncOut, asyncConfiguration, asyncHttp, asyncRetry, recorders,
                               calls, credentials);
            List<ManagedResource<?>> resources = asyncConfiguration.resources();
            assertTrue(resources.contains(ManagedResource.callerOwned(asyncHttp)), resources.toString());
            assertFalse(asyncHttp.closed, "closing the async client leaves the caller's HTTP client open");
            List<ExecutorService> sdkExecutors = resources.stream()
                                                          .filter(r -> r.ownership() == ResourceOwnership.SDK_OWNED)
                                                          .map(ManagedResource::resource)
                                                          .filter(ExecutorService.class::isInstance)
                                                          .map(ExecutorService.class::cast).toList();
            if (callerExecutor != null) {
                assertTrue(resources.contains(ManagedResource.callerOwned(callerExecutor)), resources.toString());
                assertEquals(List.of(), sdkExecutors, "a caller's executor replaces the SDK's");
                assertFalse(callerExecutor.isShutdown(), "closing the client leaves the caller's executor running");
            } else {
                assertEquals(1, sdkExecutors.size(), "one SDK-owned completion executor: " + resources);
                assertTrue(sdkExecutors.get(0).isShutdown(), "closing the client shuts its SDK-owned executor down");
            }
        } finally {
            if (callerExecutor != null) {
                callerExecutor.shutdownNow();
            }
        }
        recorders.forEach(r -> r.seen.clear());

        // Presigner: built from its own builder, so equal values and decisions, not the client's instance.
        String presigned = invoke(syncHttp, presign, region, endpoint, credentials, interceptors, scenario.bucket());
        URI presignedUrl = URI.create(presigned);
        assertEquals(scenario.expectedHost(), presignedUrl.getHost(), "presigned host");
        Matcher credential = PRESIGNED_CREDENTIAL.matcher(presigned);
        assertTrue(credential.find(), presigned);
        assertEquals(scenario.accessKey(), credential.group(1), "presigned access key");
        assertEquals(scenario.region(), credential.group(2), "presigned signing region");
        for (Recorder recorder : recorders) {
            assertEquals(1, recorder.seen.size(), "each interceptor sees the one presign call");
            Seen seen = recorder.seen.get(0);
            assertEquals(scenario.accessKey(), seen.accessKey());
            SdkClientConfiguration presignerConfiguration = seen.configuration();
            assertEquals(syncConfiguration.get(AwsClientOption.AWS_REGION),
                         presignerConfiguration.get(AwsClientOption.AWS_REGION), "presigner region");
            assertEquals(syncConfiguration.get(SdkClientOption.CLIENT_ENDPOINT),
                         presignerConfiguration.get(SdkClientOption.CLIENT_ENDPOINT), "presigner endpoint");
            assertSame(credentials, presignerConfiguration.get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER)
                                                          .orElse(null), "presigner credentials provider");
        }
        assertFalse(credentials.closed, "no client, waiter or presigner closes the caller's credentials provider");
    }

    /**
     * One client's derived calls: identity of the configuration every call observed, the resolved values, the retry
     * strategy, the transmitted decisions, and that each closed waiter left the client usable.
     */
    private static void assertDerivedCalls(String client, Scenario scenario, Map<String, Object> out,
                                           SdkClientConfiguration configuration, FakeS3 http, CountingRetry retry,
                                           List<Recorder> recorders, int calls, Credentials credentials) {
        assertNotNull(configuration, client + " configuration");
        assertSame(configuration, out.get("configurationAfter"), client + ": serviceClientConfiguration() is stable");
        assertEquals(Optional.of(Region.of(scenario.region())), configuration.get(AwsClientOption.AWS_REGION),
                     client + " region");
        assertEquals(scenario.endpoint().map(URI::create), configuration.get(SdkClientOption.CLIENT_ENDPOINT),
                     client + " endpoint override");
        assertSame(credentials, configuration.get(AwsClientOption.CREDENTIALS_IDENTITY_PROVIDER).orElse(null),
                   client + " credentials provider");

        // Each closed waiter left the client open: the later paginator and waiter still ran.
        assertEquals(scenario.pendingPolls() + 1, out.get("attempts1"), client + " first waiter attempts");
        assertEquals(scenario.pages(), out.get("pages"), client + " pages");
        assertEquals(scenario.pendingPolls() + 1, out.get("attempts2"), client + " second waiter attempts");
        assertEquals(List.of(), http.violations, client + " transport violations");
        assertEquals(calls, http.requests.size(), client + " transmitted calls");
        assertEquals(calls, retry.acquired.get(), client + ": every call acquires from the client's retry strategy");

        for (Recorder recorder : recorders) {
            assertEquals(calls, recorder.seen.size(), client + ": every call reaches every interceptor");
            for (Seen seen : recorder.seen) {
                assertSame(configuration, seen.configuration(), client + ": a call observed another configuration");
                assertEquals(scenario.accessKey(), seen.accessKey(), client + " identity");
            }
        }
        for (SdkHttpRequest request : http.requests) {
            assertEquals(scenario.expectedHost(), request.host(), client + " " + request.method() + " host");
            String authorization = request.headers().firstValue("Authorization").orElse("");
            Matcher matcher = AUTHORIZATION.matcher(authorization);
            assertTrue(matcher.find(), client + " authorization: " + authorization);
            assertEquals(scenario.accessKey(), matcher.group(1), client + " signing access key");
            assertEquals(scenario.region(), matcher.group(2), client + " signing region");
        }
    }

    @Provide
    Arbitrary<Scenario> scenarios() {
        Arbitrary<String> region = Arbitraries.of("us-east-1", "us-west-2", "eu-central-1", "ap-southeast-2");
        Arbitrary<Optional<String>> endpoint = Arbitraries.of(Optional.empty(), Optional.of("https://s3.test"),
                                                              Optional.of("https://storage.example:8443/base"));
        Arbitrary<String> accessKey = Arbitraries.strings().withCharRange('A', 'Z').withCharRange('0', '9')
                                                 .ofLength(12).map(s -> "AKID" + s);
        Arbitrary<String> bucket = Arbitraries.strings().withCharRange('a', 'z').withCharRange('0', '9')
                                              .ofMinLength(2).ofMaxLength(12).map(s -> "b" + s);
        return Combinators.combine(region, endpoint, accessKey, Arbitraries.integers().between(1, 3), bucket,
                                   Arbitraries.integers().between(1, 4), Arbitraries.integers().between(0, 3),
                                   Arbitraries.of(true, false))
                          .as(Scenario::new);
    }

    private static <T> T invoke(FakeS3 http, Method method, Object... arguments) throws Exception {
        try {
            @SuppressWarnings("unchecked")
            T value = (T) method.invoke(null, arguments);
            return value;
        } catch (InvocationTargetException e) {
            throw new AssertionError("Probe." + method.getName() + " failed; transport violations: "
                                     + http.violations, e.getCause());
        }
    }

    /**
     * One try's client configuration and service state: {@code pages} list pages, and {@code pendingPolls}
     * {@code PENDING} statuses before {@code READY} in each wait.
     */
    record Scenario(String region, Optional<String> endpoint, String accessKey, int interceptors, String bucket,
                    int pages, int pendingPolls, boolean callerOwnedExecutor) {
        /** The host the endpoint rules give the (virtual-hostable) bucket. */
        String expectedHost() {
            return endpoint.map(e -> bucket + "." + URI.create(e).getHost())
                           .orElse(bucket + ".s3." + region + ".amazonaws.com");
        }
    }

    record Seen(SdkClientConfiguration configuration, String accessKey) {
    }

    /** Records, per signed attempt, the configuration the call reads through to and the signing identity. */
    static final class Recorder implements ExecutionInterceptor {
        final List<Seen> seen = new CopyOnWriteArrayList<>();

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            String accessKey = context.identity().map(i -> ((AwsCredentialsIdentity) i).accessKeyId()).orElse(null);
            seen.add(new Seen(context.options().clientConfiguration(), accessKey));
            return context.signedRequest();
        }
    }

    /** A caller-owned, closeable static credentials provider that records whether anything closed it. */
    static final class Credentials implements IdentityProvider<AwsCredentialsIdentity>, AutoCloseable {
        private final AwsCredentialsIdentity identity;
        volatile boolean closed;

        Credentials(String accessKey) {
            this.identity = AwsCredentialsIdentity.create(accessKey, "secret");
        }

        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(identity);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    /** The standard strategy, counting the calls that acquire an initial token from this instance. */
    static final class CountingRetry implements RetryStrategy {
        final AtomicInteger acquired = new AtomicInteger();
        private final RetryStrategy delegate = StandardRetryStrategy.create();

        @Override
        public int maxAttempts() {
            return delegate.maxAttempts();
        }

        @Override
        public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
            acquired.incrementAndGet();
            return delegate.acquireInitialToken(request);
        }

        @Override
        public RetryDecision refreshRetryToken(RetryToken token, AttemptFailure failure) {
            return delegate.refreshRetryToken(token, failure);
        }

        @Override
        public RetryToken recordSuccess(RetryToken token) {
            return delegate.recordSuccess(token);
        }

        @Override
        public RetryToken recordFailure(RetryToken token) {
            return delegate.recordFailure(token);
        }
    }

    /**
     * A strict fake S3. {@code GET} is {@code ListObjectsV2}: call {@code k} must carry token {@code t<k>} (none for
     * the first) and returns {@code t<k+1>} until the last page. {@code HEAD} is {@code GetBucketStatus}: each wait
     * sees {@code pendingPolls} {@code PENDING} statuses, then {@code READY}. Anything else is a violation.
     */
    static final class FakeS3 implements SdkSyncHttpClient, SdkAsyncHttpClient {
        final List<SdkHttpRequest> requests = new CopyOnWriteArrayList<>();
        final List<String> violations = new CopyOnWriteArrayList<>();
        private final HttpTransportMode mode;
        private final Scenario scenario;
        private final AtomicInteger listCalls = new AtomicInteger();
        private final AtomicInteger polls = new AtomicInteger();
        volatile boolean closed;

        FakeS3(HttpTransportMode mode, Scenario scenario) {
            this.mode = mode;
            this.scenario = scenario;
        }

        private SdkHttpResponse respond(SdkHttpRequest request) {
            requests.add(request);
            if (request.method().equals("HEAD")) {
                boolean pending = polls.getAndIncrement() % (scenario.pendingPolls() + 1) < scenario.pendingPolls();
                return SdkHttpResponse.builder().statusCode(200)
                                      .addHeader("x-amz-bucket-status", pending ? "PENDING" : "READY").build();
            }
            if (!request.method().equals("GET")) {
                return violation("unexpected " + request.method() + " " + request.rawTarget());
            }
            int call = listCalls.getAndIncrement();
            String token = request.queryParameters().stream().filter(p -> p.rawName().equals("continuation-token"))
                                  .map(HttpQueryParameter::rawValue).findFirst().orElse(null);
            String expected = call == 0 ? null : "t" + call;
            if (call >= scenario.pages() || !Objects.equals(expected, token)) {
                return violation("list call " + call + " carries " + token + ": " + request.rawTarget());
            }
            String next = call + 1 < scenario.pages() ? "<NextContinuationToken>t" + (call + 1)
                                                        + "</NextContinuationToken>" : "";
            return SdkHttpResponse.builder().statusCode(200)
                                  .body(HttpBody.fromUtf8("<ListObjectsV2Output>" + next + "<KeyCount>0</KeyCount>"
                                                          + "</ListObjectsV2Output>"))
                                  .build();
        }

        private SdkHttpResponse violation(String message) {
            violations.add(message);
            return SdkHttpResponse.builder().statusCode(400).body(HttpBody.fromUtf8("<Error/>")).build();
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
            return "FakeS3";
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
