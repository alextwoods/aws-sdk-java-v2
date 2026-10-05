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

package software.amazon.awssdk.dynamic.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.dynamic.client.TestSupport.json;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.EdgeCasesMode;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.statistics.Statistics;
import software.amazon.awssdk.auth.api.SignedRequest;
import software.amazon.awssdk.core.client.config.ConfigurationSources;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.dynamic.client.DynamicInvocationException.Violation;
import software.amazon.awssdk.dynamic.client.RandomInputDocuments.Case;
import software.amazon.awssdk.dynamic.client.RandomInputDocuments.Mutation;
import software.amazon.awssdk.dynamic.client.TestSupport.TestTransport;
import software.amazon.awssdk.dynamic.model.DynamicModel;
import software.amazon.awssdk.dynamic.model.DynamicService;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
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
import software.amazon.smithy.java.core.schema.SerializableStruct;
import software.amazon.smithy.java.core.serde.document.Document;
import software.amazon.smithy.java.dynamicschemas.StructDocument;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.StructureShape;

/**
 * Property 53: Dynamic finite input validates before transport (Requirements 14.11, 14.19).
 *
 * <p>Each try generates a random runtime service model ({@link RandomInputModel}: nested and recursive structures,
 * unions, dense and sparse lists and maps, every scalar type, and required, default, client-optional and
 * idempotency-token members, with or without {@code @input}), loads it through {@code DynamicModel.fromJsonAst}, and
 * builds a sync and an async dynamic client over recording transports, with a recording interceptor, a counting retry
 * strategy and a counting credentials provider. It then generates a conforming input document by traversal of the
 * Smithy shapes and copies with 1 to 3 random schema mutations ({@link RandomInputDocuments}).
 *
 * <p>For every document the expected violation paths come from {@link ReferenceInputValidator}, an independent
 * traversal of the Smithy model shapes. The operation's {@code DynamicInputValidator} must report exactly those paths,
 * each with a non-empty problem. A conforming document must be transmitted exactly once by each client and its output
 * returned; a nonconforming one must be rejected by both clients with a non-retryable
 * {@link DynamicInvocationException} ({@code INVALID_INPUT}) listing exactly those paths, the async future already
 * failed when returned, and with no transport exchange, interceptor hook, retry token or identity resolution.
 */
class DynamicInputValidationPropertyTest {
    private static final int TRIES = 300;
    private static final int MUTATED_CASES = 6;

    // Feature: aws-sdk-java-v3, Property 53: Dynamic finite input validates before transport
    // Each try assembles a fresh model and builds two clients; each checks one conforming and six mutated documents
    // against the validator and both clients.
    @Property(tries = TRIES, edgeCases = EdgeCasesMode.NONE)
    @Label("Property 53: Dynamic finite input validates before transport")
    void dynamicFiniteInputValidatesBeforeTransport(@ForAll("models") RandomInputModel generated) throws Exception {
        DynamicModel model = DynamicModel.fromJsonAst(generated.jsonAst(), generated + ".json");
        Model smithy = model.model();
        StructureShape input = smithy.expectShape(generated.inputId(), StructureShape.class);
        ReferenceInputValidator oracle = new ReferenceInputValidator(smithy);
        Recorders recorders = new Recorders();
        TestTransport syncTransport = TestTransport.sync(r -> json(200, "{\"echo\":\"ok\"}"));
        TestTransport asyncTransport = TestTransport.async(r -> json(200, "{\"echo\":\"ok\"}"));
        try (DynamicClient sync = recorders.configure(DynamicClient.builder().model(model)).httpClient(syncTransport)
                                           .build();
             DynamicAsyncClient async = recorders.configure(DynamicAsyncClient.builder().model(model))
                                                 .httpClient(asyncTransport).build()) {
            DynamicService service = sync.service();
            DynamicInputValidator validator =
                DynamicServiceBinding.of(service).operation(RandomInputModel.OPERATION).validator();
            Document foreign = StructDocument.of(service.operation(RandomInputModel.OPERATION).schemas().output(),
                                                 Document.of(Map.of()));
            // Documents are derived from the model's seed, so a reported model reproduces its documents too.
            Random random = new Random(generated.seed() ^ 0x9E3779B97F4A7C15L);
            List<Case> cases = new RandomInputDocuments(smithy, input, random, foreign).cases(MUTATED_CASES);
            List<List<String>> expectations = new ArrayList<>();
            // Statistics first, so a failing try still reports every label.
            for (Case c : cases) {
                List<String> expected = oracle.violations(input, c.document());
                expectations.add(expected);
                Statistics.label("outcome").collect(expected.isEmpty() ? "conforming" : "nonconforming");
                for (Mutation mutation : c.mutations()) {
                    Statistics.label("mutation").collect(mutation);
                }
            }
            Statistics.label("outcome").coverage(coverage -> {
                coverage.check("conforming").count(n -> n >= TRIES);
                coverage.check("nonconforming").count(n -> n >= TRIES * 2);
            });
            Statistics.label("mutation").coverage(coverage -> {
                for (Mutation mutation : Mutation.values()) {
                    coverage.check(mutation).count(n -> n >= TRIES / 5);
                }
            });
            for (int i = 0; i < cases.size(); i++) {
                Case c = cases.get(i);
                List<String> expected = expectations.get(i);
                String context = generated + " case " + i + " mutations " + c.mutations() + "\ndocument: "
                                 + c.document() + "\nexpected violations: " + expected + "\nmodel:\n"
                                 + generated.jsonAst();
                if (i == 0) {
                    assertEquals(List.of(), expected, () -> "The generated conforming document must conform\n"
                                                            + context);
                }

                List<Violation> direct = validator.validate(c.document());
                assertEquals(expected, sortedPaths(direct), () -> "DynamicInputValidator paths differ\n"
                                                                  + "actual: " + direct + "\n" + context);
                assertProblems(direct, context);

                if (expected.isEmpty()) {
                    accepted(sync, async, syncTransport, asyncTransport, recorders, c.document(), context);
                } else {
                    rejected(sync, async, syncTransport, asyncTransport, recorders, c.document(), expected, context);
                }
            }
        }
    }

    @Provide
    Arbitrary<RandomInputModel> models() {
        return Combinators.combine(Arbitraries.longs(), Arbitraries.integers().between(0, 5))
                          .as(RandomInputModel::generate);
    }

    /** A conforming document: each client transmits it exactly once and returns the modeled output. */
    private static void accepted(DynamicClient sync, DynamicAsyncClient async, TestTransport syncTransport,
                                 TestTransport asyncTransport, Recorders recorders, Document document,
                                 String context) throws Exception {
        int syncSent = syncTransport.transmissions();
        int tokens = recorders.tokens.get();
        int executions = recorders.executions.get();
        Document output;
        try {
            output = sync.call(RandomInputModel.OPERATION, document);
        } catch (SdkException e) {
            throw new AssertionError("A conforming document was not accepted by the sync client\n" + context, e);
        }
        assertEquals(syncSent + 1, syncTransport.transmissions(), () -> "sync transmissions\n" + context);
        assertEquals("ok", output.getMember("echo").asString(), () -> "sync output\n" + context);
        assertEquals(tokens + 1, recorders.tokens.get(), () -> "sync retry tokens\n" + context);
        assertEquals(executions + 1, recorders.executions.get(), () -> "sync beforeExecution\n" + context);

        int asyncSent = asyncTransport.transmissions();
        Document asyncOutput;
        try {
            asyncOutput = async.callAsync(RandomInputModel.OPERATION, document).get(30, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new AssertionError("A conforming document was not accepted by the async client\n" + context,
                                     e.getCause());
        }
        assertEquals(asyncSent + 1, asyncTransport.transmissions(), () -> "async transmissions\n" + context);
        assertEquals("ok", asyncOutput.getMember("echo").asString(), () -> "async output\n" + context);
        assertEquals(tokens + 2, recorders.tokens.get(), () -> "async retry tokens\n" + context);
    }

    /** A nonconforming document: both clients reject it with exactly the expected paths, before anything runs. */
    private static void rejected(DynamicClient sync, DynamicAsyncClient async, TestTransport syncTransport,
                                 TestTransport asyncTransport, Recorders recorders, Document document,
                                 List<String> expected, String context) {
        int syncSent = syncTransport.transmissions();
        int asyncSent = asyncTransport.transmissions();
        int hooks = recorders.hooks.get();
        int tokens = recorders.tokens.get();
        int identities = recorders.identities.get();

        DynamicInvocationException e = assertThrows(DynamicInvocationException.class,
                                                    () -> sync.call(RandomInputModel.OPERATION, document),
                                                    () -> "sync call\n" + context);
        assertRejection(e, expected, "sync", context);

        CompletableFuture<Document> future = async.callAsync(RandomInputModel.OPERATION, document);
        assertTrue(future.isDone() && future.isCompletedExceptionally(),
                   () -> "The async future must already be failed when returned\n" + context);
        Throwable failure = future.exceptionNow();
        assertRejection(assertInstanceOf(DynamicInvocationException.class, failure, () -> "async failure\n" + context),
                        expected, "async", context);

        assertEquals(syncSent, syncTransport.transmissions(), () -> "sync transport exchanges\n" + context);
        assertEquals(asyncSent, asyncTransport.transmissions(), () -> "async transport exchanges\n" + context);
        assertEquals(hooks, recorders.hooks.get(), () -> "interceptor hooks\n" + context);
        assertEquals(tokens, recorders.tokens.get(), () -> "retry tokens\n" + context);
        assertEquals(identities, recorders.identities.get(), () -> "identity resolutions\n" + context);
    }

    private static void assertRejection(DynamicInvocationException e, List<String> expected, String mode,
                                        String context) {
        assertEquals(DynamicInvocationException.Reason.INVALID_INPUT, e.reason(), () -> mode + " reason\n" + context);
        assertFalse(e.retryable(), () -> mode + " retryable\n" + context);
        assertEquals(RandomInputModel.SERVICE, e.service(), () -> mode + " service\n" + context);
        assertEquals(RandomInputModel.OPERATION, e.operationName(), () -> mode + " operation\n" + context);
        assertEquals(expected, sortedPaths(e.violations()),
                     () -> mode + " violation paths differ\nactual: " + e.violations() + "\n" + context);
        assertProblems(e.violations(), context);
    }

    private static void assertProblems(List<Violation> violations, String context) {
        for (Violation violation : violations) {
            assertFalse(violation.problem().isBlank(), () -> "empty problem for " + violation.path() + "\n" + context);
        }
    }

    private static List<String> sortedPaths(List<Violation> violations) {
        return violations.stream().map(Violation::path).sorted().toList();
    }

    /** Counts every interceptor hook, retry token and identity resolution of one try's clients. */
    private static final class Recorders {
        final AtomicInteger hooks = new AtomicInteger();
        final AtomicInteger executions = new AtomicInteger();
        final AtomicInteger tokens = new AtomicInteger();
        final AtomicInteger identities = new AtomicInteger();

        <B extends DynamicClientBuilder<B, ?>> B configure(B builder) {
            return builder.region(Region.US_WEST_2)
                          .endpointOverride(URI.create("https://gen.test"))
                          .configurationSources(ConfigurationSources.none())
                          .credentialsProvider(new CountingCredentials(identities))
                          .retryStrategy(new CountingRetryStrategy(StandardRetryStrategy.create(), tokens))
                          .addExecutionInterceptor(new CountingInterceptor(hooks, executions));
        }
    }

    private record CountingCredentials(AtomicInteger resolutions) implements IdentityProvider<AwsCredentialsIdentity> {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            resolutions.incrementAndGet();
            return CompletableFuture.completedFuture(
                AwsCredentialsIdentity.create("AKIDEXAMPLE", "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"));
        }
    }

    private record CountingRetryStrategy(RetryStrategy delegate, AtomicInteger tokens) implements RetryStrategy {
        @Override
        public int maxAttempts() {
            return delegate.maxAttempts();
        }

        @Override
        public AcquireInitialTokenResponse acquireInitialToken(AcquireInitialTokenRequest request) {
            tokens.incrementAndGet();
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

    /** Counts every hook of {@link ExecutionInterceptor}; modifying hooks return their input unchanged. */
    private record CountingInterceptor(AtomicInteger hooks, AtomicInteger executions) implements ExecutionInterceptor {
        @Override
        public void beforeExecution(HookContext.BeforeExecution context) {
            hooks.incrementAndGet();
            executions.incrementAndGet();
        }

        @Override
        public SerializableStruct modifyRequest(HookContext.ModifyRequest context) {
            hooks.incrementAndGet();
            return context.request();
        }

        @Override
        public void beforeAttempt(HookContext.BeforeAttempt context) {
            hooks.incrementAndGet();
        }

        @Override
        public void afterSerialization(HookContext.AfterSerialization context) {
            hooks.incrementAndGet();
        }

        @Override
        public SdkHttpRequest modifyBeforeSigning(HookContext.ModifyBeforeSigning context) {
            hooks.incrementAndGet();
            return context.httpRequest();
        }

        @Override
        public SignedRequest modifyAfterSigning(HookContext.ModifyAfterSigning context) {
            hooks.incrementAndGet();
            return context.signedRequest();
        }

        @Override
        public void beforeTransmission(HookContext.BeforeTransmission context) {
            hooks.incrementAndGet();
        }

        @Override
        public void afterTransmission(HookContext.AfterTransmission context) {
            hooks.incrementAndGet();
        }

        @Override
        public SdkHttpResponse modifyHttpResponse(HookContext.ModifyHttpResponse context) {
            hooks.incrementAndGet();
            return context.httpResponse();
        }

        @Override
        public HttpBody modifyResponseBody(HookContext.ModifyResponseBody context) {
            hooks.incrementAndGet();
            return context.body();
        }

        @Override
        public void beforeDeserialization(HookContext.BeforeDeserialization context) {
            hooks.incrementAndGet();
        }

        @Override
        public Throwable modifyAttemptException(HookContext.ModifyAttemptException context) {
            hooks.incrementAndGet();
            return context.failure();
        }

        @Override
        public void afterAttempt(HookContext.AfterAttempt context) {
            hooks.incrementAndGet();
        }

        @Override
        public SerializableStruct modifyResponse(HookContext.ModifyResponse context) {
            hooks.incrementAndGet();
            return context.response();
        }

        @Override
        public Throwable modifyException(HookContext.ModifyException context) {
            hooks.incrementAndGet();
            return context.failure();
        }

        @Override
        public void afterExecution(HookContext.AfterExecution context) {
            hooks.incrementAndGet();
        }
    }
}
