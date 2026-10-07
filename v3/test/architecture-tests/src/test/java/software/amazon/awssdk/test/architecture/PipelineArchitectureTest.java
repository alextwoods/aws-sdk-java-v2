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

package software.amazon.awssdk.test.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.test.architecture.ClassFile.Member;
import software.amazon.awssdk.test.architecture.ClassFile.MemberRef;
import software.amazon.awssdk.test.architecture.GenerationPathInspector.Scope;
import software.amazon.awssdk.test.architecture.GenerationPathInspector.Violation;

/**
 * Architecture enforcement of the single V3 AWS pipeline over production bytecode (task 9.6; Requirements 3.9, 10.1,
 * 10.5, 10.10, 10.11, 10.12, 12.15, 20.21, 20.22, 21.5). {@link ForbiddenGenerationPathsTest} already applies the
 * module-wide hot-path rules (V2 types, class-name lookup, reflection, service discovery, model loading) to every
 * runtime module; this test adds what is specific to the pipeline:
 * <ul>
 *   <li>exactly one engine: across every hot-path module, only {@code AwsCallEngine}'s two drivers and their shared
 *       stages invoke the HTTP SPI's exchanges and own retry tokens (apart from the
 *       {@linkplain #OTHER_HTTP_SPI_USERS allowlisted} leaf transports: credential retrieval and the SNS
 *       signing-certificate download), and nothing else drives the stages;</li>
 *   <li>no V2 types in the pipeline's runtime closure, without a hand-maintained list: every
 *       {@code software.amazon.awssdk} class the closure references is defined by a closure module, so a V2 class
 *       (V3 reuses V2 package names) cannot be referenced at all;</li>
 *   <li>reflection only at the {@linkplain #REFLECTION_SITES allowlisted} plan-time and class-initialization sites,
 *       none of which the call path can reach: a call-graph walk from {@code executeSync}/{@code executeAsync} reaches
 *       no reflective, class-loading or model-parsing site and no plan construction;</li>
 *   <li>no operation lookup by name or ID in the pipeline;</li>
 *   <li>hook contexts created only by the interceptor dispatch, and no timers or counters: the only waits are retry
 *       delays (the sync driver's sleep, the async driver's retry scheduler); the Telemetry SPI and clock reads only in
 *       the telemetry stage's classes, which the rest of the pipeline uses only at null-checked sites (task 16.2);</li>
 *   <li>one pipeline for every client projection (task 12.4; Requirements 10.4, 14.14): the dynamic client builds,
 *       plans and calls through the same {@code AwsClientBuildState}, {@code ClientRuntime} and
 *       {@code AwsCallEngine} entry points as generated clients, no projection assembles its own engine or performs
 *       a stage operation (serialization, endpoint or identity resolution, signing, deserialization) itself, and the
 *       dynamic client's runtime closure references no V2 type.</li>
 * </ul>
 * {@code DynamicIsolationRuntimeTest} runs generated and dynamic calls to prove the same stages at run time.
 * The runtime side (plans built once, no hook contexts or timers without interceptors or delays) is
 * {@code PipelineHotPathTest} in {@code aws-pipeline}.
 */
class PipelineArchitectureTest {
    private static final String PIPELINE = "aws-pipeline";
    private static final String PKG = "software.amazon.awssdk.pipeline.";
    private static final String ENGINE = PKG + "AwsCallEngine";
    private static final String ASYNC_CALL = PKG + "AsyncCall";
    private static final String CALL_STAGES = PKG + "CallStages";
    private static final String DISPATCH = PKG + "InterceptorDispatch";
    private static final String HOOK_CONTEXT = PKG + "HookContext";

    /** The HTTP SPI exchange methods and retry-token ownership methods: what an execution engine invokes. */
    private static final Map<String, Set<String>> ENGINE_METHODS = Map.of(
        "software.amazon.awssdk.http.SdkSyncHttpClient", Set.of("prepare"),
        "software.amazon.awssdk.http.SdkAsyncHttpClient", Set.of("execute"),
        "software.amazon.awssdk.retries.RetryStrategy", Set.of("acquireInitialToken", "refreshRetryToken"));

    /** The one engine: class to the engine methods it invokes. */
    private static final Map<String, Set<String>> ENGINE_CLASSES = Map.of(
        ENGINE, Set.of("software.amazon.awssdk.http.SdkSyncHttpClient#prepare"),
        ASYNC_CALL, Set.of("software.amazon.awssdk.http.SdkAsyncHttpClient#execute"),
        CALL_STAGES, Set.of("software.amazon.awssdk.retries.RetryStrategy#acquireInitialToken",
                            "software.amazon.awssdk.retries.RetryStrategy#refreshRetryToken"));

    /**
     * Allowlisted HTTP SPI users outside the pipeline that do not execute generated operations: module, class, the
     * engine methods it may invoke, and why. Each must stay a leaf transport (no retry-token ownership).
     */
    private static final List<HttpSpiUser> OTHER_HTTP_SPI_USERS = List.of(
        new HttpSpiUser("identity-providers", "software.amazon.awssdk.identity.providers.internal.IdentityHttpClient",
                        Set.of("software.amazon.awssdk.http.SdkSyncHttpClient#prepare"),
                        "Credential retrieval (instance metadata, container, and the natively implemented STS "
                        + "AssumeRole/AssumeRoleWithWebIdentity and SSO GetRoleCredentials calls): identity "
                        + "providers cannot depend on generated service artifacts, so they send single buffered "
                        + "requests through the sync HTTP SPI. It runs inside identity resolution, never instead of "
                        + "the pipeline, and owns no retry tokens."),
        new HttpSpiUser("sns-message-manager",
                        "software.amazon.awssdk.messagemanager.sns.internal.CertificateRetriever",
                        Set.of("software.amazon.awssdk.http.SdkSyncHttpClient#prepare"),
                        "SNS signing-certificate download (task 14.5; V2 also fetched it with its SdkHttpClient): one "
                        + "unsigned HTTPS GET of a public PEM file from the regional SNS host, which is not an AWS API "
                        + "operation and has no generated operation to run through the pipeline. It sends single "
                        + "requests, never retries, and owns no retry tokens."));

    /** An allowlisted HTTP SPI user that is not an execution engine. */
    private record HttpSpiUser(String module, String className, Set<String> methods, String reason) {
    }

    /**
     * Every reflective site of the pipeline, with why it is not per-call work. The test fails if a site is added or
     * one of these disappears.
     */
    private static final Map<String, String> REFLECTION_SITES = Map.of(
        DISPATCH + "#overrides",
        "Plan time: InterceptorDispatch.of computes once per plan which hooks each interceptor class overrides "
        + "(Class.getMethod, Method.getDeclaringClass). Allowed in ForbiddenGenerationPathsTest.ALLOWANCES.",
        PKG + "CallState#<clinit>",
        "Class initialization: MethodHandles.Lookup.findVarHandle for the atomic unconsumed-token field.",
        PKG + "TerminalState#<clinit>",
        "Class initialization: MethodHandles.Lookup.findVarHandle for the atomic outcome field.");

    /** Methods that build plans or dispatch plans: plan time only, never reachable from a call. */
    private static final Set<String> PLAN_TIME_METHODS = Set.of(
        PKG + "ClientRuntime#plan", PKG + "PipelinePlan#create", PKG + "PipelinePlan#<init>",
        PKG + "PipelinePlan$Builder#<init>", PKG + "PipelinePlan$Builder#build", PKG + "StageMetadata#of",
        PKG + "ContextLayout#builder", PKG + "ContextLayout$Builder#build", DISPATCH + "#of", DISPATCH + "#<init>",
        DISPATCH + "#select", DISPATCH + "#overrides");

    /** The entry points of a call; a presign call runs the same stages up to signing. */
    private static final Set<String> CALL_ROOTS = Set.of(ENGINE + "#executeSync", ENGINE + "#executeAsync",
                                                         ENGINE + "#presign");

    /** Methods every call reaches; proves the call-graph walk is not vacuous. */
    private static final Set<String> CALL_PATH_SAMPLE = Set.of(
        CALL_STAGES + "#begin", CALL_STAGES + "#beforeAttempts", CALL_STAGES + "#sign", CALL_STAGES + "#fail",
        ASYNC_CALL + "#start", DISPATCH + "#beforeExecution", DISPATCH + "#afterExecutionSucceeded",
        PKG + "CallState#<init>", PKG + "PipelinePlan#newCall");

    /** Clock, timer, scheduling and counter types: work an inactive extension must not do. */
    private static final List<String> CLOCK_AND_COUNTER_TYPES = List.of(
        "java.time.Clock", "java.time.InstantSource", "java.util.Timer", "java.util.TimerTask",
        "java.util.concurrent.ScheduledExecutorService", "java.util.concurrent.ScheduledThreadPoolExecutor",
        "java.util.concurrent.ScheduledFuture", "java.util.concurrent.atomic.AtomicLong",
        "java.util.concurrent.atomic.LongAdder", "java.util.concurrent.atomic.LongAccumulator",
        "java.util.concurrent.atomic.DoubleAdder", "java.util.concurrent.atomic.DoubleAccumulator");
    private static final Set<MemberRef> CLOCK_METHODS = Set.of(
        new MemberRef("java.lang.System", "nanoTime", "()J"),
        new MemberRef("java.lang.System", "currentTimeMillis", "()J"),
        new MemberRef("java.time.Instant", "now", "()Ljava/time/Instant;"));
    /** The Telemetry SPI: the V3 contract and the V2-compatible metrics API telemetry-spi ships. */
    private static final List<String> TELEMETRY_PACKAGES = List.of(
        "software.amazon.awssdk.telemetry.", "software.amazon.awssdk.metrics.", "software.amazon.awssdk.core.metrics.");
    private static final String PIPELINE_TELEMETRY = PKG + "PipelineTelemetry";
    private static final String CALL_TELEMETRY = PKG + "CallTelemetry";

    /**
     * The pipeline classes that may reference the Telemetry SPI (task 16.2), with why. Only the two telemetry classes
     * run per call, and they exist only for a client that configures a publisher.
     */
    private static final Map<String, String> TELEMETRY_CLASSES = Map.of(
        PIPELINE_TELEMETRY, "The client and operation telemetry plan, built only when a publisher is configured.",
        CALL_TELEMETRY, "The per-call telemetry state, created only by a plan that has a PipelineTelemetry.",
        PKG + "ClientRuntime", "Reads TelemetryOptions.TELEMETRY once, when the client runtime is built.",
        PKG + "client.AwsClientBuildState", "The client builders' telemetry and addMetricPublisher settings.");

    /** The only clock reads of the pipeline: telemetry timing, inside the telemetry classes. */
    private static final Set<String> CLOCK_SITES = Set.of(
        PIPELINE_TELEMETRY + "#newCall", CALL_TELEMETRY + "#attemptStarted", CALL_TELEMETRY + "#attemptEnded",
        CALL_TELEMETRY + "#callEnded");

    /**
     * Every use of the telemetry classes elsewhere in the pipeline, as {@code caller -> callee}. Each caller holds
     * the telemetry plan or call state in a field that is null without a configured publisher and calls it behind
     * that null check, so a client without telemetry creates no telemetry object, reads no clock and makes no
     * callback ({@code PipelineTelemetryTest}, {@code InactiveExtensionPropertyTest}; Requirement 21.5).
     */
    private static final Set<String> TELEMETRY_USE_SITES = Set.of(
        PKG + "ClientRuntime#<init> -> " + PIPELINE_TELEMETRY + "#forClient",
        PKG + "PipelinePlan$Builder#<init> -> " + PIPELINE_TELEMETRY + "#forOperation",
        PKG + "CallState#<init> -> " + PIPELINE_TELEMETRY + "#newCall",
        PKG + "AttemptState#<init> -> " + CALL_TELEMETRY + "#attemptStarted",
        PKG + "AttemptState#complete -> " + CALL_TELEMETRY + "#attemptEnded",
        CALL_STAGES + "#telemetryEnded -> " + CALL_TELEMETRY + "#callEnded");

    /** The only waits of a call, both retry delays: method to the use it may make. */
    private static final Map<String, String> WAIT_SITES = Map.of(
        ENGINE + "#sleep", "java.lang.Thread.sleep(JI)V",
        ASYNC_CALL + "#delay", "software.amazon.awssdk.retries.RetryScheduler.delay"
                               + "(Ljava/time/Duration;)Ljava/util/concurrent/CompletableFuture;");

    private static final String DYNAMIC_CLIENT = "dynamic-client";
    /** The generated-service fixture whose clients the dynamic client is compared with. */
    private static final String GENERATED_FIXTURE = "codegen-lifecycle-tests";
    private static final String CLIENT_PKG = PKG + "client.";

    /**
     * How a client projection builds and calls: the shared build state resolves the client's parts once, the
     * client's runtime plans each operation, and every call is one engine driver call (Requirement 14.14).
     */
    private static final Set<String> PROJECTION_ENTRY_POINTS = Set.of(
        CLIENT_PKG + "AwsClientBuildState#buildSync", CLIENT_PKG + "AwsClientBuildState#buildAsync",
        CLIENT_PKG + "AwsClientParts#runtime", CLIENT_PKG + "AwsClientParts#engine", PKG + "ClientRuntime#plan",
        ENGINE + "#executeSync", ENGINE + "#executeAsync");

    /** Assembling an engine or runtime outside {@code AwsClientBuildState}: a second way to put a pipeline together. */
    private static final Set<String> PIPELINE_ASSEMBLY = Set.of(
        ENGINE + "#builder", PKG + "ClientRuntime#builder", ENGINE + "$Builder#", PKG + "ClientRuntime$Builder#");

    /**
     * The stage operations the engine performs on protocol, endpoint and auth components: client projections never
     * invoke them, so no call serializes, resolves, signs or deserializes outside the engine's stages. The HTTP SPI
     * exchanges and retry tokens are {@link #ENGINE_METHODS}, checked across every hot-path module by
     * {@link #exactlyOneEngineInvokesTheHttpSpiAndOwnsRetryTokens}.
     */
    private static final Map<String, Set<String>> STAGE_OPERATIONS = Map.of(
        "software.amazon.awssdk.protocols.core.OperationProtocol",
        Set.of("serializeRequest", "isError", "deserializeResponse", "deserializeError", "deserializeStreamingOutput"),
        "software.amazon.awssdk.endpoints.EndpointPlan", Set.of("resolve"),
        "software.amazon.awssdk.auth.api.SelectedAuthScheme", Set.of("resolveIdentity", "sign"));

    /**
     * V2 packages of the legacy extension and execution model (signers, execution interceptors, credentials
     * providers, presigners, request handlers and the V2 HTTP pipeline). V3 defines none of them, so neither its
     * pipeline nor its dynamic client may reference them, even by a class V3 happened to define under that name.
     */
    private static final List<String> LEGACY_V2_EXTENSION_PACKAGES = List.of(
        "software.amazon.awssdk.core.interceptor.", "software.amazon.awssdk.core.signer.",
        "software.amazon.awssdk.auth.signer.", "software.amazon.awssdk.auth.credentials.",
        "software.amazon.awssdk.http.auth.", "software.amazon.awssdk.awscore.presigner.",
        "software.amazon.awssdk.awscore.interceptor.", "software.amazon.awssdk.core.internal.http.",
        "software.amazon.awssdk.core.client.handler.", "software.amazon.awssdk.awscore.client.handler.");

    private static V3Reactor reactor;
    private static Map<String, ClassFile> pipeline;

    @BeforeAll
    static void load() {
        reactor = V3Reactor.load();
        pipeline = reactor.classesOf(PIPELINE);
        assertTrue(pipeline.containsKey(ENGINE), "aws-pipeline must define " + ENGINE);
    }

    @Test
    void exactlyOneEngineInvokesTheHttpSpiAndOwnsRetryTokens() {
        Map<String, ClassFile> all = new TreeMap<>();
        Map<String, String> moduleOf = new TreeMap<>();
        for (String module : hotPathModules()) {
            reactor.classesOf(module).forEach((name, type) -> {
                all.put(name, type);
                moduleOf.put(name, module);
            });
        }
        Map<String, Set<String>> engines = new TreeMap<>();
        for (ClassFile type : all.values()) {
            for (Member method : type.methods()) {
                for (MemberRef ref : method.memberReferences()) {
                    for (Map.Entry<String, Set<String>> spi : ENGINE_METHODS.entrySet()) {
                        if (spi.getValue().contains(ref.name()) && isSubtype(ref.owner(), spi.getKey(), all)) {
                            engines.computeIfAbsent(type.name(), t -> new TreeSet<>())
                                   .add(spi.getKey() + "#" + ref.name());
                        }
                    }
                }
            }
        }
        Map<String, Set<String>> expected = new TreeMap<>(ENGINE_CLASSES);
        Map<String, String> expectedModules = new TreeMap<>();
        ENGINE_CLASSES.keySet().forEach(name -> expectedModules.put(name, PIPELINE));
        for (HttpSpiUser user : OTHER_HTTP_SPI_USERS) {
            assertFalse(user.reason().isBlank(), user.className());
            expected.put(user.className(), user.methods());
            expectedModules.put(user.className(), user.module());
        }
        assertEquals(expected, engines,
                     "Only AwsCallEngine's drivers and CallStages may transmit through the HTTP SPI or own retry "
                     + "tokens (Requirements 10.1, 10.5); a second execution path was introduced, or an allowlisted "
                     + "user changed");
        engines.keySet().forEach(name -> assertEquals(expectedModules.get(name), moduleOf.get(name), name));
    }

    @Test
    void theStagesAreDrivenOnlyByTheEngine() {
        List<String> problems = new ArrayList<>();
        for (ClassFile type : pipeline.values()) {
            for (Member method : type.methods()) {
                for (MemberRef ref : method.memberReferences()) {
                    String caller = type.name() + "#" + method.name();
                    if (ref.owner().equals(CALL_STAGES) && !nestedIn(type.name(), CALL_STAGES)) {
                        boolean allowed = ref.name().equals("<init>") ? caller.equals(ENGINE + "#<init>")
                                          : nestedIn(type.name(), ENGINE) || nestedIn(type.name(), ASYNC_CALL);
                        if (!allowed) {
                            problems.add(caller + " drives CallStages." + ref.name());
                        }
                    }
                    if (ref.owner().equals(ASYNC_CALL) && ref.name().equals("<init>")
                        && !caller.equals(ENGINE + "#executeAsync")) {
                        problems.add(caller + " creates an AsyncCall");
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    /**
     * The pipeline's runtime closure, and the dynamic client's (which adds the dynamic model, every protocol, Modern
     * Auth and the identity providers), reference no V2 type (Requirements 10.5, 12.15, 14.14).
     */
    @ParameterizedTest
    @ValueSource(strings = {PIPELINE, DYNAMIC_CLIENT})
    void theRuntimeClosureReferencesNoV2Type(String root) {
        Set<String> closure = runtimeClosure(root);
        assertTrue(closure.contains(root) && closure.containsAll(Set.of(PIPELINE, "http-spi", "retries", "aws-core")),
                   closure.toString());
        if (root.equals(DYNAMIC_CLIENT)) {
            assertTrue(closure.containsAll(Set.of("dynamic-model", "aws-json-protocol", "auth-aws")),
                       closure.toString());
        }
        Set<String> defined = new HashSet<>();
        closure.forEach(module -> defined.addAll(reactor.classesOf(module).keySet()));
        Set<String> problems = new TreeSet<>();
        for (String module : closure) {
            for (ClassFile type : reactor.classesOf(module).values()) {
                if (startsWithAny(type.name(), LEGACY_V2_EXTENSION_PACKAGES)) {
                    problems.add(module + " defines " + type.name() + " in a V2 signer, interceptor, credentials or "
                                 + "execution package");
                }
                for (String reference : type.referencedClasses()) {
                    String element = reference.replaceFirst("^\\[+L?", "").replaceFirst(";$", "");
                    if (element.startsWith("software.amazon.awssdk.") && !defined.contains(element)) {
                        problems.add(module + ": " + type.name() + " references " + element
                                     + ", which no module of the " + root + " runtime closure defines");
                    }
                    if (startsWithAny(element, LEGACY_V2_EXTENSION_PACKAGES)) {
                        problems.add(module + ": " + type.name() + " references " + element + " (V2 signer, "
                                     + "interceptor, credentials or execution package)");
                    }
                }
            }
        }
        assertEquals(Set.of(), problems,
                     "V2 pipeline, interceptor, signer or HTTP adapter types on the V3 call path (Requirements 10.5, "
                     + "12.15)");
        List<Violation> v2 = new ArrayList<>();
        for (String module : closure) {
            for (Violation violation : GenerationPathInspector.inspectModule(
                module, Scope.RUNTIME, reactor.classesOf(module), GenerationPathInspector.AWS_CODEGEN)) {
                if (violation.rule() == GenerationPathRule.V2_EXECUTION_PIPELINE
                    || violation.rule() == GenerationPathRule.V2_HTTP_ADAPTER) {
                    v2.add(violation);
                }
            }
        }
        assertEquals(List.of(), v2);
    }

    @Test
    void reflectionClassLookupAndModelParsingOccurOnlyAtAllowlistedSites() {
        Set<String> sites = new TreeSet<>();
        for (Violation violation : GenerationPathInspector.inspectModule(
            PIPELINE, Scope.RUNTIME, pipeline, GenerationPathInspector.AWS_CODEGEN)) {
            assertEquals(GenerationPathRule.REFLECTIVE_MEMBER_ACCESS, violation.rule(), violation.toString());
            sites.add(violation.location());
        }
        sites.addAll(varHandleInitializers());
        assertEquals(new TreeSet<>(REFLECTION_SITES.keySet()), sites,
                     "Reflective sites of aws-pipeline changed; every site must be plan-time or class "
                     + "initialization and listed in REFLECTION_SITES with its reason (Requirements 3.9, 10.12, "
                     + "20.22)");
        assertTrue(ForbiddenGenerationPathsTest.ALLOWANCES.stream().anyMatch(
                       a -> a.module().equals(PIPELINE) && a.location().equals(DISPATCH + "#overrides")
                            && a.rule() == GenerationPathRule.REFLECTIVE_MEMBER_ACCESS),
                   "The plan-time override detection must be an explicit allowance of ForbiddenGenerationPathsTest");
    }

    @Test
    void theCallPathReachesNoPlanConstructionReflectionOrModelParsing() {
        Set<String> reached = reachable(CALL_ROOTS);
        assertTrue(reached.containsAll(CALL_PATH_SAMPLE), "call-graph walk is incomplete: " + reached);
        // Positive control: from plan construction the same walk does reach the plan-time reflective site.
        assertTrue(reachable(Set.of(PKG + "ClientRuntime#plan")).contains(DISPATCH + "#overrides"),
                   "call-graph walk misses lambdas or constructors");

        Set<String> forbidden = new TreeSet<>(PLAN_TIME_METHODS);
        forbidden.addAll(REFLECTION_SITES.keySet());
        Set<String> unknown = new TreeSet<>(PLAN_TIME_METHODS);
        unknown.removeIf(PipelineArchitectureTest::isDeclared);
        assertEquals(Set.of(), unknown, "PLAN_TIME_METHODS names methods aws-pipeline no longer declares");

        Set<String> hit = new TreeSet<>(reached);
        hit.retainAll(forbidden);
        assertEquals(Set.of(), hit,
                     "A call reaches plan construction or a reflective site (Requirements 10.11, 20.22)");
    }

    @Test
    void thePipelineHasNoOperationLookupByNameOrId() {
        Set<String> lookups = new TreeSet<>();
        Set<String> results = Set.of(PKG + "AwsOperation", PKG + "PipelinePlan",
                                     GenerationPathInspector.OPERATION_DESCRIPTOR);
        for (ClassFile type : pipeline.values()) {
            for (Member method : type.methods()) {
                String descriptor = method.descriptor();
                int close = descriptor.indexOf(')');
                Set<String> parameters = ClassFile.classNamesIn(descriptor.substring(0, close + 1));
                Set<String> returned = ClassFile.classNamesIn(descriptor.substring(close + 1));
                boolean byName = parameters.contains("java.lang.String")
                                 || parameters.contains("software.amazon.smithy.model.shapes.ShapeId");
                if (byName && returned.stream().anyMatch(results::contains)) {
                    lookups.add(type.name() + "#" + method.name() + descriptor);
                }
            }
        }
        assertEquals(Set.of(), lookups, "Operations reach the pipeline by static reference only (Requirements 3.9, "
                                        + "10.12)");
    }

    @Test
    void onlyTheDispatchCreatesHookContexts() {
        Set<String> creators = new TreeSet<>();
        for (ClassFile type : pipeline.values()) {
            for (Member method : type.methods()) {
                for (MemberRef ref : method.memberReferences()) {
                    if (ref.name().equals("<init>") && nestedIn(ref.owner(), HOOK_CONTEXT)
                        && !nestedIn(type.name(), HOOK_CONTEXT)) {
                        creators.add(type.name());
                    }
                }
            }
        }
        assertEquals(Set.of(DISPATCH), creators,
                     "Hook contexts must be created only by InterceptorDispatch, which creates none for a hook no "
                     + "interceptor implements (Requirements 10.10, 20.21)");
    }

    @Test
    void thePipelineUsesNoClocksTimersCountersOrTelemetryAndWaitsOnlyForRetryDelays() {
        Set<String> problems = new TreeSet<>();
        Map<String, Set<String>> waits = new TreeMap<>();
        Set<String> telemetryUses = new TreeSet<>();
        Set<String> clockReads = new TreeSet<>();
        for (ClassFile type : pipeline.values()) {
            boolean telemetryClass = TELEMETRY_CLASSES.keySet().stream().anyMatch(t -> nestedIn(type.name(), t));
            boolean runsTelemetry = nestedIn(type.name(), PIPELINE_TELEMETRY) || nestedIn(type.name(), CALL_TELEMETRY);
            for (String reference : type.referencedClasses()) {
                if (CLOCK_AND_COUNTER_TYPES.contains(reference)
                    || (!telemetryClass && startsWithAny(reference, TELEMETRY_PACKAGES))) {
                    problems.add(type.name() + " references " + reference);
                }
            }
            for (Member method : type.methods()) {
                for (MemberRef ref : method.memberReferences()) {
                    if (CLOCK_METHODS.contains(ref)) {
                        clockReads.add(type.name() + "#" + method.name());
                    }
                    boolean telemetryCallee = ref.owner().equals(PIPELINE_TELEMETRY)
                                              || ref.owner().equals(CALL_TELEMETRY);
                    if (telemetryCallee && !runsTelemetry && !ref.name().equals("<init>")) {
                        telemetryUses.add(type.name() + "#" + method.name() + " -> " + ref.owner() + "#" + ref.name());
                    }
                    boolean sleep = ref.owner().equals("java.lang.Thread") && ref.name().equals("sleep");
                    boolean schedule = ref.owner().equals("software.amazon.awssdk.retries.RetryScheduler");
                    if (sleep || schedule) {
                        waits.computeIfAbsent(type.name() + "#" + method.name(), m -> new TreeSet<>())
                             .add(ref.toString());
                    }
                }
            }
        }
        assertEquals(Set.of(), problems,
                     "No timers or counters in the pipeline, and the Telemetry SPI only in TELEMETRY_CLASSES "
                     + "(Requirements 10.10, 21.5)");
        assertEquals(new TreeSet<>(CLOCK_SITES), clockReads,
                     "The pipeline reads the clock only for telemetry, in PipelineTelemetry and CallTelemetry, which "
                     + "exist only when a publisher is configured (Requirement 21.5)");
        assertEquals(new TreeSet<>(TELEMETRY_USE_SITES), telemetryUses,
                     "The telemetry plan and call state are used only at the null-checked TELEMETRY_USE_SITES, so an "
                     + "inactive telemetry stage does no work (Requirements 10.10, 21.5)");
        TELEMETRY_CLASSES.forEach((name, reason) -> {
            assertTrue(pipeline.containsKey(name), name);
            assertFalse(reason.isBlank(), name);
        });
        Map<String, Set<String>> expected = new TreeMap<>();
        WAIT_SITES.forEach((site, use) -> expected.put(site, Set.of(use)));
        assertEquals(expected, waits, "The only waits of a call are its retry delays");
    }

    @Test
    void theDynamicClientBuildsAndCallsThroughTheSameEntryPointsAsGeneratedClients() {
        // Requirements 10.1, 10.4, 14.14: the dynamic client builds its parts through AwsClientBuildState, plans
        // through ClientRuntime and calls AwsCallEngine's two drivers, exactly as generated clients do; no client
        // projection assembles an engine or runtime of its own.
        assertTrue(reactor.generatedCodeModules().contains(GENERATED_FIXTURE), reactor.generatedCodeModules()
                                                                                       .toString());
        Map<String, Set<String>> used = new TreeMap<>();
        Set<String> assembly = new TreeSet<>();
        for (String module : projectionModules()) {
            for (ClassFile type : reactor.classesOf(module).values()) {
                for (Member method : type.methods()) {
                    for (MemberRef ref : method.memberReferences()) {
                        String target = ref.owner() + "#" + ref.name();
                        if (PROJECTION_ENTRY_POINTS.contains(target)) {
                            used.computeIfAbsent(module, m -> new TreeSet<>()).add(target);
                        }
                        if (PIPELINE_ASSEMBLY.contains(target) || PIPELINE_ASSEMBLY.contains(ref.owner() + "#")) {
                            assembly.add(module + ": " + type.name() + "#" + method.name() + " uses " + target);
                        }
                    }
                }
            }
        }
        assertEquals(new TreeSet<>(PROJECTION_ENTRY_POINTS), used.get(DYNAMIC_CLIENT),
                     "The dynamic client must build, plan and call through the shared pipeline entry points");
        assertEquals(used.get(GENERATED_FIXTURE), used.get(DYNAMIC_CLIENT),
                     "The dynamic and generated clients enter the pipeline differently");
        assertEquals(Set.of(), assembly, "A client projection assembles its own engine or runtime");
        PROJECTION_ENTRY_POINTS.forEach(site -> assertTrue(isDeclared(site), site));
    }

    @Test
    void clientProjectionsPerformNoStageOperationOutsideTheEngine() {
        // Requirements 10.1, 10.5, 14.14: generated and dynamic clients never serialize, resolve endpoints or
        // identities, sign or deserialize themselves; only the engine's stages do.
        Map<String, ClassFile> all = new TreeMap<>();
        hotPathModules().forEach(module -> all.putAll(reactor.classesOf(module)));
        Set<String> expected = new TreeSet<>();
        STAGE_OPERATIONS.forEach((owner, names) -> names.forEach(name -> expected.add(owner + "#" + name)));
        // Positive control: the engine's call path performs every listed operation.
        Set<String> performed = new TreeSet<>();
        for (String key : reachableMethods(CALL_ROOTS)) {
            for (MemberRef ref : method(key).memberReferences()) {
                performed.addAll(stageOperations(ref, all));
            }
        }
        assertEquals(expected, performed, "STAGE_OPERATIONS names operations the engine's call path does not perform");

        Set<String> bypasses = new TreeSet<>();
        for (String module : projectionModules()) {
            for (ClassFile type : reactor.classesOf(module).values()) {
                for (Member method : type.methods()) {
                    for (MemberRef ref : method.memberReferences()) {
                        stageOperations(ref, all).forEach(operation -> bypasses.add(
                            module + ": " + type.name() + "#" + method.name() + " invokes " + operation));
                    }
                }
            }
        }
        assertEquals(Set.of(), bypasses, "A client projection runs a pipeline stage outside AwsCallEngine");
    }

    @Test
    void theAllowlistsNameDeclaredMethodsWithReasons() {
        assertFalse(REFLECTION_SITES.values().stream().anyMatch(String::isBlank));
        REFLECTION_SITES.keySet().forEach(site -> assertTrue(isDeclared(site), site));
        WAIT_SITES.keySet().forEach(site -> assertTrue(isDeclared(site), site));
        CALL_ROOTS.forEach(site -> assertTrue(isDeclared(site), site));
    }

    // -------------------------------------------------------------------------------------------------------------

    /** Production modules whose classes can run on the generated-client call path. */
    private static Set<String> hotPathModules() {
        Set<String> result = new TreeSet<>();
        for (String module : reactor.productionModules()) {
            Scope scope = reactor.generatedCodeModules().contains(module) || reactor.serviceModules().contains(module)
                          ? Scope.GENERATED
                          : ForbiddenGenerationPathsTest.SCOPES_BY_KIND.get(reactor.policy().kindOf(module)
                                                                                 .orElseThrow());
            if (scope == Scope.GENERATED || scope == Scope.RUNTIME || scope == Scope.DYNAMIC) {
                result.add(module);
            }
        }
        assertTrue(result.contains(PIPELINE), result.toString());
        return result;
    }

    /** Client projections: generated services and generated-code modules, and the dynamic layer. */
    private static Set<String> projectionModules() {
        Set<String> result = new TreeSet<>(reactor.serviceModules());
        result.addAll(reactor.generatedCodeModules());
        reactor.policy().modules().values().stream().filter(m -> m.kind().equals("dynamic"))
               .forEach(m -> result.add(m.artifactId()));
        assertTrue(result.containsAll(Set.of(DYNAMIC_CLIENT, "dynamic-model", GENERATED_FIXTURE)), result.toString());
        return result;
    }

    /** The {@link #STAGE_OPERATIONS} {@code ref} invokes, resolving its owner's supertypes through {@code all}. */
    private static Set<String> stageOperations(MemberRef ref, Map<String, ClassFile> all) {
        Set<String> result = new TreeSet<>();
        STAGE_OPERATIONS.forEach((owner, names) -> {
            if (names.contains(ref.name()) && isSubtype(ref.owner(), owner, all)) {
                result.add(owner + "#" + ref.name());
            }
        });
        return result;
    }

    private static boolean startsWithAny(String name, List<String> prefixes) {
        return prefixes.stream().anyMatch(name::startsWith);
    }

    /** {@code module} and the reactor modules it depends on at compile or runtime scope, transitively. */
    private static Set<String> runtimeClosure(String module) {
        Map<String, PomFile> modules = reactor.artifactModules();
        Set<String> closure = new TreeSet<>();
        Deque<String> pending = new ArrayDeque<>(List.of(module));
        while (!pending.isEmpty()) {
            String next = pending.pop();
            if (!closure.add(next)) {
                continue;
            }
            for (PomFile.Dependency dependency : modules.get(next).dependencies()) {
                String scope = dependency.scope() == null ? "compile" : dependency.scope();
                if (modules.containsKey(dependency.artifactId()) && "software.amazon.awssdk".equals(
                    dependency.groupId()) && (scope.equals("compile") || scope.equals("runtime"))) {
                    pending.push(dependency.artifactId());
                }
            }
        }
        return closure;
    }

    /** The {@code <clinit>} methods that create {@code VarHandle}s (a fixed exemption of the inspector). */
    private static Set<String> varHandleInitializers() {
        Set<String> sites = new TreeSet<>();
        for (ClassFile type : pipeline.values()) {
            for (Member method : type.methods()) {
                for (MemberRef ref : method.memberReferences()) {
                    if (ref.owner().equals("java.lang.invoke.MethodHandles$Lookup") && ref.name().startsWith("find")) {
                        sites.add(type.name() + "#" + method.name());
                    }
                }
            }
        }
        return sites;
    }

    /**
     * The {@code class#method} names reachable from {@code roots} within aws-pipeline. Calls resolve by class
     * hierarchy analysis over the module's classes (the named owner, its supertypes and its subtypes); a constructor
     * call makes the class's instance methods reachable, because the JDK or another module may call them back; a
     * lambda or method reference makes its target reachable. Static initializers run once per class, not per call,
     * and are not followed. Calls into other modules end the walk: their classes are checked module-wide by
     * {@link ForbiddenGenerationPathsTest}.
     */
    private static Set<String> reachable(Set<String> roots) {
        Set<String> reached = new TreeSet<>();
        for (String key : reachableMethods(roots)) {
            int hash = key.indexOf('#');
            reached.add(key.substring(0, hash) + "#" + key.substring(hash + 1, key.indexOf('(', hash)));
        }
        return reached;
    }

    /** As {@link #reachable}, keyed by {@code class#name(descriptor)}. */
    private static Set<String> reachableMethods(Set<String> roots) {
        Deque<String> pending = new ArrayDeque<>();
        for (String root : roots) {
            String owner = root.substring(0, root.indexOf('#'));
            String name = root.substring(root.indexOf('#') + 1);
            pipeline.get(owner).methods().stream().filter(m -> m.name().equals(name))
                    .forEach(m -> pending.push(key(owner, m)));
        }
        Set<String> visited = new HashSet<>();
        while (!pending.isEmpty()) {
            String next = pending.pop();
            if (!visited.add(next)) {
                continue;
            }
            Member method = method(next);
            for (MemberRef ref : method.memberReferences()) {
                if (!ref.descriptor().startsWith("(") || !pipeline.containsKey(ref.owner())
                    || ref.name().equals("<clinit>")) {
                    continue;
                }
                for (ClassFile target : pipeline.values()) {
                    boolean related = isSubtype(target.name(), ref.owner(), pipeline)
                                      || isSubtype(ref.owner(), target.name(), pipeline);
                    if (!related || ref.name().equals("<init>") && !target.name().equals(ref.owner())) {
                        continue;
                    }
                    for (Member candidate : target.methods()) {
                        if (candidate.name().equals(ref.name()) && candidate.descriptor().equals(ref.descriptor())) {
                            pending.push(key(target.name(), candidate));
                        }
                    }
                }
                if (ref.name().equals("<init>")) {
                    for (Member instanceMethod : pipeline.get(ref.owner()).methods()) {
                        if (!instanceMethod.isStatic() && !instanceMethod.name().startsWith("<")) {
                            pending.push(key(ref.owner(), instanceMethod));
                        }
                    }
                }
            }
        }
        return visited;
    }

    private static String key(String owner, Member method) {
        return owner + "#" + method.name() + method.descriptor();
    }

    /** The aws-pipeline method with key {@code class#name(descriptor)}. */
    private static Member method(String key) {
        int hash = key.indexOf('#');
        String signature = key.substring(hash + 1);
        return pipeline.get(key.substring(0, hash)).methods().stream()
                       .filter(m -> (m.name() + m.descriptor()).equals(signature)).findFirst().orElseThrow();
    }

    private static boolean isDeclared(String site) {
        String owner = site.substring(0, site.indexOf('#'));
        String name = site.substring(site.indexOf('#') + 1);
        ClassFile type = pipeline.get(owner);
        return type != null && type.methods().stream().anyMatch(m -> m.name().equals(name));
    }

    /** True if {@code type} is {@code supertype} or extends or implements it through classes of {@code classes}. */
    private static boolean isSubtype(String type, String supertype, Map<String, ClassFile> classes) {
        if (type.equals(supertype)) {
            return true;
        }
        ClassFile file = classes.get(type);
        if (file == null) {
            return false;
        }
        if (!file.superName().isEmpty() && isSubtype(file.superName(), supertype, classes)) {
            return true;
        }
        return file.interfaces().stream().anyMatch(i -> isSubtype(i, supertype, classes));
    }

    /** True if {@code name} is {@code outer} or a class nested in it. */
    private static boolean nestedIn(String name, String outer) {
        return name.equals(outer) || name.startsWith(outer + "$");
    }
}
