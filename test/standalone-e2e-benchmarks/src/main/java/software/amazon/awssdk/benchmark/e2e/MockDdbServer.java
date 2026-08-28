package software.amazon.awssdk.benchmark.e2e;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * Standalone mock DynamoDB HTTP server, run in its own JVM so no server CPU or allocation shows
 * up in client-side profiles (fairness fix #3).
 *
 * <p>Routes on the {@code X-Amz-Target} header (awsJson1.0 operation dispatch) and returns a canned,
 * byte-identical response per operation, precomputed at startup from {@link BenchmarkItems}:
 *
 * <pre>
 *   DynamoDB_20120810.GetItem        -&gt; {"Item": &lt;small item&gt;}
 *   DynamoDB_20120810.PutItem        -&gt; {}
 *   DynamoDB_20120810.BatchGetItem   -&gt; {"Responses":{"benchmark-table":[25 medium items]},"UnprocessedKeys":{}}
 *   DynamoDB_20120810.BatchWriteItem -&gt; {"UnprocessedItems":{}}
 * </pre>
 *
 * <p>The request body is read fully and discarded (realistic socket behavior, no parsing). Requests
 * without an {@code X-Amz-Target} header get {@code 200 "ok"} — used by the launch scripts as a
 * readiness probe ({@code GET /ping}). Unknown targets get a 400 so client-side mistakes fail fast.
 *
 * <p>{@code GET /stats} reports server-side counters as {@code key=value} lines. Once the client
 * runs operations concurrently, the server stops being free: it shares the host's cores, so a
 * throughput number can silently be a measurement of the server rather than of the client. These
 * counters make that visible instead of leaving it to be assumed — {@code queue_size} and
 * {@code busy_threads} vs {@code max_threads} say directly whether the server is saturated, and
 * {@code requests} cross-checks that it actually served the operations the client thinks it issued.
 * {@code /stats} is excluded from the request count so polling it does not corrupt that check.
 *
 * <p>Usage: {@code MockDdbServer [--port N] [--threads N]} (default port {@value #DEFAULT_PORT}).
 * Prints {@code READY port=N} on stdout once the listener is up, then runs until killed.
 */
public final class MockDdbServer {

    public static final int DEFAULT_PORT = 19080;
    /**
     * Handler threads. Jetty's own default, but set explicitly: the pool has to comfortably exceed
     * the client's concurrency (plus acceptor and selector threads) or the server queues requests
     * and the client measures that queue.
     */
    public static final int DEFAULT_MAX_THREADS = 200;
    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";
    private static final String TARGET_PREFIX = "DynamoDB_20120810.";
    private static final String STATS_PATH = "/stats";

    private MockDdbServer() {
    }

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        int maxThreads = DEFAULT_MAX_THREADS;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if ("--threads".equals(args[i]) && i + 1 < args.length) {
                maxThreads = Integer.parseInt(args[++i]);
            }
        }

        Map<String, byte[]> responses = new HashMap<>();
        responses.put(TARGET_PREFIX + "GetItem", utf8(BenchmarkItems.getItemResponseJson()));
        responses.put(TARGET_PREFIX + "PutItem", utf8(BenchmarkItems.putItemResponseJson()));
        responses.put(TARGET_PREFIX + "BatchGetItem", utf8(BenchmarkItems.batchGetItemResponseJson()));
        responses.put(TARGET_PREFIX + "BatchWriteItem", utf8(BenchmarkItems.batchWriteItemResponseJson()));

        QueuedThreadPool pool = new QueuedThreadPool(maxThreads, 8);
        pool.setName("mockddb");
        Server server = new Server(pool);
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});
        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.NO_SESSIONS);
        context.addServlet(new ServletHolder(new DdbServlet(responses, pool)), "/*");
        server.setHandler(context);
        server.start();

        System.out.printf("READY port=%d pid=%d threads=%d selectors=%d"
                          + " (GetItem=%dB PutItem=%dB BatchGetItem=%dB BatchWriteItem=%dB)%n",
                          port, ProcessHandle.current().pid(), maxThreads,
                          connector.getSelectorManager().getSelectorCount(),
                          responses.get(TARGET_PREFIX + "GetItem").length,
                          responses.get(TARGET_PREFIX + "PutItem").length,
                          responses.get(TARGET_PREFIX + "BatchGetItem").length,
                          responses.get(TARGET_PREFIX + "BatchWriteItem").length);
        System.out.flush();
        server.join();
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static final class DdbServlet extends HttpServlet {
        private static final byte[] OK = "ok".getBytes(StandardCharsets.UTF_8);
        private final Map<String, byte[]> responses;
        private final QueuedThreadPool pool;
        private final CpuTimeSource cpuSource;
        private final AtomicLong requests = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();

        DdbServlet(Map<String, byte[]> responses, QueuedThreadPool pool) {
            this.responses = responses;
            this.pool = pool;
            // The server measures its own CPU with the same source the client uses, so the two
            // numbers are directly comparable when deciding which process is the bottleneck.
            this.cpuSource = CpuTimeSources.bind("auto");
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (STATS_PATH.equals(req.getRequestURI())) {
                // Answered before the body read and before any counter is touched: polling stats
                // must not perturb the counters the client is about to compare against.
                writeStats(resp);
                return;
            }

            // Consume the request body so the connection can be reused.
            req.getInputStream().readAllBytes();

            String target = req.getHeader("X-Amz-Target");
            if (target == null) {
                // Readiness probe (GET /ping or anything without an operation header).
                resp.setStatus(200);
                resp.setContentType("text/plain");
                resp.setContentLength(OK.length);
                resp.getOutputStream().write(OK);
                return;
            }

            byte[] body = responses.get(target);
            if (body == null) {
                errors.incrementAndGet();
                resp.setStatus(400);
                resp.setContentType(CONTENT_TYPE);
                byte[] err = ("{\"__type\":\"com.amazon.coral.service#UnknownOperationException\",\"message\":\""
                              + target + "\"}").getBytes(StandardCharsets.UTF_8);
                resp.setContentLength(err.length);
                resp.getOutputStream().write(err);
                return;
            }

            requests.incrementAndGet();
            resp.setStatus(200);
            resp.setContentType(CONTENT_TYPE);
            resp.setContentLength(body.length);
            resp.getOutputStream().write(body);
        }

        /**
         * {@code key=value} lines rather than JSON: the client parses this with a two-line split and
         * adding a JSON library to the server would put its allocations in the same process we are
         * asking to stay cheap.
         */
        private void writeStats(HttpServletResponse resp) throws IOException {
            CpuTimeSource.Snapshot cpu = cpuSource.snapshot();
            StringBuilder sb = new StringBuilder(320);
            sb.append("requests=").append(requests.get()).append('\n')
              .append("errors=").append(errors.get()).append('\n')
              .append("cpu_ms=").append(cpu.totalNanos() / 1_000_000L).append('\n')
              .append("cpu_user_ms=")
              .append(cpu.hasSplit() ? Long.toString(cpu.userNanos() / 1_000_000L) : "").append('\n')
              .append("cpu_sys_ms=")
              .append(cpu.hasSplit() ? Long.toString(cpu.systemNanos() / 1_000_000L) : "").append('\n')
              // Saturation signals. `queue_size` and `low_on_threads` are the reliable ones: a
              // backlog means the client is measuring the server's queue rather than its own
              // pipeline. Read `busy_threads` with care — acceptor and selector threads come out of
              // this same pool and are always counted busy, so it has a non-zero floor at idle.
              .append("threads=").append(pool.getThreads()).append('\n')
              .append("busy_threads=").append(pool.getBusyThreads()).append('\n')
              .append("max_threads=").append(pool.getMaxThreads()).append('\n')
              .append("queue_size=").append(pool.getQueueSize()).append('\n')
              .append("low_on_threads=").append(pool.isLowOnThreads()).append('\n');
            byte[] out = sb.toString().getBytes(StandardCharsets.UTF_8);
            resp.setStatus(200);
            resp.setContentType("text/plain");
            resp.setContentLength(out.length);
            resp.getOutputStream().write(out);
        }
    }
}
