package software.amazon.awssdk.benchmark.e2e;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Probes how the SDK behaves when the service fails, so a bridged pipeline can be compared against
 * stock v2 on the two components a never-failing benchmark cannot exercise: the retry strategy and the
 * error enricher.
 *
 * <p>Both measured as free in {@code pipeline_benchmark2/components/20260904-2313}, which is the
 * expected shape — a retry wrapper costs nothing on a call that is not retried — but it left their
 * <em>behavior</em> unverified. This class supplies the failures.
 *
 * <p>For each fault in {@link Faults} it runs two cases against a freshly built client:
 *
 * <ul>
 *   <li><b>persistent</b> ({@code count=-1}): every attempt fails. Reports how many attempts the SDK
 *       made, how long it spent doing it (backoff is visible in the wall time), and exactly what it
 *       threw.
 *   <li><b>transient</b> ({@code count=2}): the first two attempts fail and the third succeeds.
 *       Distinguishes "retried and recovered" from "gave up" and from "never retried at all".
 * </ul>
 *
 * <p>A fresh client per case on purpose: v2's standard retry strategy holds a token bucket that
 * depletes across calls, so reusing one client would make each case depend on the ones before it. The
 * fault is armed over {@code /faults} immediately before the call, and the attempt count is read back
 * from {@code /stats}, so the count comes from the server rather than from anything the SDK reports
 * about itself.
 *
 * <p>Output is one {@code PROBE} line per case with named fields, for
 * {@code analysis/scripts/error_behavior_diff.py} to pair across jars. Every field is something a
 * customer can observe: the exception type they catch, the error code they switch on, the request ID
 * they put in a support ticket, whether {@code isThrottlingException()} routes them to a different code
 * path.
 *
 * <p>Usage: {@code ErrorBehaviorProbe --client v2-sync [--endpoint URI] [--faults a,b] [--reps N]}
 */
public final class ErrorBehaviorProbe {

    private static final String ACCESS_KEY = "benchmark-access-key";
    private static final String SECRET_KEY = "benchmark-secret-key";
    private static final String USAGE =
            "usage: ErrorBehaviorProbe [--client v2-sync] [--endpoint http://127.0.0.1:19080]\n"
            + "                          [--faults MODE,MODE|all] [--reps N] [--transient-count N]\n"
            + "                          [--backoff standard|immediate|fixed:MS] [--warmup] [--stacks]";

    /**
     * Whether to make one successful call on the fresh client before arming the fault.
     *
     * <p>Off by default so the reference sweep stays comparable across runs. On, it removes connection
     * setup and first-call class loading from {@code wall_ms} — around 200 ms on this host, judged from
     * the no-fault control — which is the difference between wall time being a signal and being a
     * measurement. Needed for {@code --backoff immediate}, where the quantity of interest is a few
     * milliseconds of per-attempt work and a 200 ms constant would bury it.
     */
    private static boolean warmup;

    /**
     * How the retry strategy should compute its delay between attempts.
     *
     * <p>{@code standard} is what a customer gets by default, and it is the wrong instrument for
     * measuring anything. v2's standard backoff is exponential with <em>full jitter</em>, so a delay is
     * uniform on {@code [0, base * 2^(n-1))} — for a throttled call, base 1 s, two delays, that is a
     * total uniform on roughly {@code [0, 3 s)}. Two arms running the identical policy routinely differ
     * by 4x on a single sample, which is exactly what the reference sweep shows and exactly why no
     * conclusion can be drawn from it.
     *
     * <p>{@code fixed:MS} and {@code immediate} remove the jitter, which converts wall time from noise
     * into a measurement:
     *
     * <ul>
     *   <li>{@code fixed:MS} is a <em>falsifiable check that the configured backoff survives
     *       bridging</em>. Both arms must sleep {@code MS} once per retry, so a 3-attempt case must take
     *       {@code 2 * MS} plus call work in both. A bridge that dropped, halved, or defaulted the
     *       customer's backoff shows up as a flat offset rather than something to be teased out of a
     *       distribution.
     *   <li>{@code immediate} sets the delay to zero, so wall time is the <em>cost of retrying</em> with
     *       no sleep in it: token bookkeeping, classification, and re-running serialization, signing and
     *       endpoint resolution per attempt. That is the number the timing benchmarks structurally cannot
     *       produce, because nothing fails there.
     * </ul>
     *
     * <p>Both are built from {@link software.amazon.awssdk.awscore.retry.AwsRetryStrategy}'s standard
     * strategy rather than a bare builder, so only the backoff differs from the default — the AWS retry
     * predicates, the circuit breaker and {@code maxAttempts} are untouched. The throttling backoff is
     * overridden alongside the ordinary one, because otherwise a throttling fault would still take the
     * jittered 1 s path and measure nothing.
     */
    private static String backoff = "standard";

    /**
     * Whether to dump each failure's full cause chain to stderr.
     *
     * <p>The {@code PROBE} line names only the top exception and its immediate cause, which is the
     * right granularity for diffing but not for diagnosing: retry classification walks the whole
     * chain, so "why was this not retried" is usually a question about a link three deep. Off by
     * default so it cannot pollute the machine-read output.
     */
    private static boolean stacks;

    private final URI endpoint;
    private final HttpClient control;

    private ErrorBehaviorProbe(URI endpoint) {
        this.endpoint = endpoint;
        // See BenchmarkTls.sslContext(): needed if the endpoint is https, ignored if it is not.
        this.control = HttpClient.newBuilder()
                                 .connectTimeout(Duration.ofSeconds(5))
                                 .sslContext(BenchmarkTls.sslContext())
                                 .build();
    }

    public static void main(String[] args) throws Exception {
        String client = "v2-sync";
        URI endpoint = URI.create("http://127.0.0.1:" + MockDdbServer.DEFAULT_PORT);
        String faultsArg = "all";
        int reps = 1;
        int transientCount = 2;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--client" -> client = args[++i];
                case "--endpoint" -> endpoint = URI.create(args[++i]);
                case "--faults" -> faultsArg = args[++i];
                case "--reps" -> reps = Integer.parseInt(args[++i]);
                case "--transient-count" -> transientCount = Integer.parseInt(args[++i]);
                case "--backoff" -> backoff = args[++i];
                case "--warmup" -> warmup = true;
                case "--stacks" -> stacks = true;
                case "--help", "-h" -> {
                    System.out.println(USAGE);
                    return;
                }
                default -> throw new IllegalArgumentException("unknown argument: " + args[i] + "\n" + USAGE);
            }
        }

        List<String> modes = new ArrayList<>();
        if ("all".equals(faultsArg)) {
            Faults.modes().forEach(modes::add);
        } else {
            for (String mode : faultsArg.split(",")) {
                if (Faults.get(mode) == null) {
                    throw new IllegalArgumentException("unknown fault mode: " + mode);
                }
                modes.add(mode);
            }
        }

        System.out.printf("=== build: %s%n", BuildProvenance.get().summary());
        ErrorBehaviorProbe probe = new ErrorBehaviorProbe(endpoint);
        probe.waitForServer();

        // A single successful call first, so the "no fault" row is a control: if the bridged and the
        // baseline client disagree here, nothing below can be attributed to error handling.
        for (int rep = 1; rep <= reps; rep++) {
            probe.run(client, "none", -1, "control", rep);
            for (String mode : modes) {
                probe.run(client, mode, -1, "persistent", rep);
                probe.run(client, mode, transientCount, "transient", rep);
            }
        }
        probe.arm("none", 0);
        System.out.println("DONE");
    }

    private void run(String client, String mode, long count, String caseName, int rep) throws Exception {
        try (DynamoDbClient ddb = build(client)) {
            if (warmup) {
                // Safe despite the note below, because arming zeroes the server's request counter: this
                // call happens before the fault is armed, so it neither consumes a fault nor inflates
                // `attempts`. Worth the extra round trip only when wall_ms is being read as a number
                // rather than as a signal — see --warmup.
                arm("none", 0);
                try {
                    getItem(ddb);
                } catch (RuntimeException e) {
                    throw new IllegalStateException("warmup call failed; wall_ms would be meaningless", e);
                }
            }
            arm(mode, count);
            // Without --warmup there is no warmup call, because one would consume the armed fault. The
            // connection-setup and first-call class-loading cost is then inside wall_ms — equally for
            // both arms, but at ~200 ms it dwarfs any per-attempt difference, so wall_ms is only worth
            // reading as a backoff signal, a retried case against an unretried one, and not as a latency.
            long start = System.nanoTime();
            String outcome;
            String detail;
            try {
                getItem(ddb);
                outcome = "ok";
                detail = describeNoError();
            } catch (RuntimeException e) {
                outcome = "throw";
                detail = describe(e);
                if (stacks) {
                    System.err.printf("--- STACK %s/%s (%s)%n", mode, caseName, client);
                    e.printStackTrace();
                }
            }
            long elapsedNanos = System.nanoTime() - start;
            long wallMs = elapsedNanos / 1_000_000L;
            // Microseconds as well as milliseconds: with --backoff immediate the whole quantity is 2-3 ms,
            // so integer milliseconds quantize it into two adjacent buckets and a real difference becomes
            // unresolvable. wall_ms stays because the behavior diff keys off it and reads it as a 2x
            // signal, where sub-millisecond precision would be false precision.
            long wallUs = elapsedNanos / 1_000L;
            long attempts = stat("requests");
            long faultsServed = stat("faults_served");

            System.out.printf(Locale.US,
                              "PROBE client=%s rep=%d fault=%s case=%s armed=%d backoff=%s attempts=%d"
                              + " faults_served=%d wall_ms=%d wall_us=%d outcome=%s %s%n",
                              client, rep, mode, caseName, count, backoff, attempts, faultsServed,
                              wallMs, wallUs, outcome, detail);
        }
    }

    /**
     * The fields a customer can actually observe about a failure.
     *
     * <p>{@code exception} is the fully qualified name rather than the simple name because the whole
     * question for the bridge is whether a modeled DynamoDB exception still arrives as
     * {@code services.dynamodb.model.ProvisionedThroughputExceededException} and not as some
     * {@code core} or bridge type that no customer {@code catch} block names (ledger 1.1).
     */
    private static String describe(RuntimeException e) {
        StringBuilder sb = new StringBuilder(200);
        sb.append("exception=").append(e.getClass().getName());
        sb.append(" cause=").append(e.getCause() == null ? "-" : e.getCause().getClass().getName());
        if (e instanceof AwsServiceException aws) {
            sb.append(" errorCode=").append(nullSafe(aws.awsErrorDetails() == null
                                                     ? null : aws.awsErrorDetails().errorCode()));
            sb.append(" statusCode=").append(aws.statusCode());
            sb.append(" requestId=").append(nullSafe(aws.requestId()));
            sb.append(" extendedRequestId=").append(nullSafe(aws.extendedRequestId()));
            sb.append(" retryable=").append(aws.retryable());
            sb.append(" throttling=").append(aws.isThrottlingException());
            sb.append(" clockSkew=").append(aws.isClockSkewException());
            sb.append(" serviceName=").append(nullSafe(aws.awsErrorDetails() == null
                                                       ? null : aws.awsErrorDetails().serviceName()));
            sb.append(" rawResponse=").append(aws.awsErrorDetails() == null
                                              || aws.awsErrorDetails().rawResponse() == null ? "absent" : "present");
        } else if (e instanceof SdkException sdk) {
            sb.append(" errorCode=- statusCode=- requestId=- extendedRequestId=-");
            sb.append(" retryable=").append(sdk.retryable());
            sb.append(" throttling=- clockSkew=- serviceName=- rawResponse=-");
        } else {
            sb.append(" errorCode=- statusCode=- requestId=- extendedRequestId=-");
            sb.append(" retryable=- throttling=- clockSkew=- serviceName=- rawResponse=-");
        }
        sb.append(" message=").append(sanitize(e.getMessage()));
        return sb.toString();
    }

    // Same field set, so a successful case lines up column-for-column with a failing one in the diff.
    private static String describeNoError() {
        return "exception=- cause=- errorCode=- statusCode=- requestId=- extendedRequestId=-"
               + " retryable=- throttling=- clockSkew=- serviceName=- rawResponse=- message=-";
    }

    private static String nullSafe(String s) {
        return s == null || s.isEmpty() ? "-" : s;
    }

    /**
     * Messages go on a space-separated line and end up in a CSV column, and they routinely contain
     * spaces, newlines and commas. Whitespace becomes underscores so the field stays one token and
     * commas become semicolons so the CSV needs no quoting; the length cap keeps a multi-kilobyte
     * marshalling error from swamping the log.
     */
    private static String sanitize(String message) {
        if (message == null) {
            return "-";
        }
        String flat = message.replaceAll("\\s+", "_").replace(',', ';');
        return flat.length() > 160 ? flat.substring(0, 160) + "..." : flat;
    }

    private DynamoDbClient build(String client) {
        if (!"v2-sync".equals(client)) {
            // Deliberately narrow: the comparison this probe exists for is v2-on-bridge against
            // v2-on-v2, which is the same client name on two different jars. Adding a `smithy` arm
            // would compare a different API's exceptions and answer a different question.
            throw new IllegalArgumentException("unsupported client for this probe: " + client
                                               + " (only v2-sync)");
        }
        ClientOverrideConfiguration.Builder override = ClientOverrideConfiguration.builder();
        if ("standard".equals(backoff)) {
            override.retryStrategy(RetryMode.STANDARD);
        } else {
            override.retryStrategy(deterministicRetryStrategy());
        }
        return DynamoDbClient.builder()
                             .endpointOverride(endpoint)
                             .region(Region.US_EAST_1)
                             .credentialsProvider(StaticCredentialsProvider.create(
                                     AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                             .httpClientBuilder(software.amazon.awssdk.http.apache5.Apache5HttpClient.builder()
                                                                                                    .maxConnections(1))
                             .overrideConfiguration(override.build())
                             .build();
    }

    /**
     * v2's standard AWS strategy with the jitter taken out of both backoffs. See {@link #backoff}.
     *
     * <p>Passed to {@code retryStrategy(...)} rather than {@code retryPolicy(...)} on purpose: the
     * policy path is not bridged at all (ledger 3.4), so configuring it here would silently leave the
     * bridged arm on smithy's own default and the two arms would be running different policies while
     * appearing to be configured identically.
     */
    private static RetryStrategy deterministicRetryStrategy() {
        BackoffStrategy fixed;
        if ("immediate".equals(backoff)) {
            // retryImmediately() rather than fixedDelayWithoutJitter(ZERO): v2 validates the duration as
            // positive, so the zero case has its own factory and passing ZERO to the other one throws.
            fixed = BackoffStrategy.retryImmediately();
        } else if (backoff.startsWith("fixed:")) {
            long ms = Long.parseLong(backoff.substring("fixed:".length()));
            fixed = ms == 0 ? BackoffStrategy.retryImmediately()
                            : BackoffStrategy.fixedDelayWithoutJitter(Duration.ofMillis(ms));
        } else {
            throw new IllegalArgumentException("unknown --backoff: " + backoff + "\n" + USAGE);
        }
        return AwsRetryStrategy.standardRetryStrategy()
                               .toBuilder()
                               .backoffStrategy(fixed)
                               .throttlingBackoffStrategy(fixed)
                               .build();
    }

    // ---- server control ------------------------------------------------------

    private void arm(String mode, long count) throws Exception {
        String body = get("/faults?mode=" + mode + "&count=" + count);
        if (!body.startsWith("ok ")) {
            throw new IllegalStateException("could not arm fault " + mode + ": " + body);
        }
    }

    private static void getItem(DynamoDbClient ddb) {
        ddb.getItem(r -> r.tableName(BenchmarkItems.TABLE_NAME)
                          .key(Map.of("pk", AttributeValue.fromS(BenchmarkItems.SMALL_KEY))));
    }

    private long stat(String key) throws Exception {
        for (String line : get("/stats").split("\n")) {
            int eq = line.indexOf('=');
            if (eq > 0 && line.substring(0, eq).equals(key)) {
                return Long.parseLong(line.substring(eq + 1).trim());
            }
        }
        throw new IllegalStateException("no stat named " + key);
    }

    private String get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(endpoint.resolve(path))
                                        .timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = control.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("GET " + path + " -> " + response.statusCode()
                                            + ": " + response.body());
        }
        return response.body();
    }

    private void waitForServer() throws Exception {
        for (int i = 0; i < 60; i++) {
            try {
                get("/stats");
                return;
            } catch (IOException | IllegalStateException e) {
                Thread.sleep(500);
            }
        }
        throw new IllegalStateException("mock server not reachable at " + endpoint);
    }
}
