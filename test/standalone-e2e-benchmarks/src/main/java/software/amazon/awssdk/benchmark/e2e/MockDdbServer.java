package software.amazon.awssdk.benchmark.e2e;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

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
 * <p>Usage: {@code MockDdbServer [--port N]} (default port {@value #DEFAULT_PORT}).
 * Prints {@code READY port=N} on stdout once the listener is up, then runs until killed.
 */
public final class MockDdbServer {

    public static final int DEFAULT_PORT = 19080;
    private static final String CONTENT_TYPE = "application/x-amz-json-1.0";
    private static final String TARGET_PREFIX = "DynamoDB_20120810.";

    private MockDdbServer() {
    }

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
                i++;
            }
        }

        Map<String, byte[]> responses = new HashMap<>();
        responses.put(TARGET_PREFIX + "GetItem", utf8(BenchmarkItems.getItemResponseJson()));
        responses.put(TARGET_PREFIX + "PutItem", utf8(BenchmarkItems.putItemResponseJson()));
        responses.put(TARGET_PREFIX + "BatchGetItem", utf8(BenchmarkItems.batchGetItemResponseJson()));
        responses.put(TARGET_PREFIX + "BatchWriteItem", utf8(BenchmarkItems.batchWriteItemResponseJson()));

        Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[] {connector});
        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.NO_SESSIONS);
        context.addServlet(new ServletHolder(new DdbServlet(responses)), "/*");
        server.setHandler(context);
        server.start();

        System.out.printf("READY port=%d pid=%d (GetItem=%dB PutItem=%dB BatchGetItem=%dB BatchWriteItem=%dB)%n",
                          port, ProcessHandle.current().pid(),
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

        DdbServlet(Map<String, byte[]> responses) {
            this.responses = responses;
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
                resp.setStatus(400);
                resp.setContentType(CONTENT_TYPE);
                byte[] err = ("{\"__type\":\"com.amazon.coral.service#UnknownOperationException\",\"message\":\""
                              + target + "\"}").getBytes(StandardCharsets.UTF_8);
                resp.setContentLength(err.length);
                resp.getOutputStream().write(err);
                return;
            }

            resp.setStatus(200);
            resp.setContentType(CONTENT_TYPE);
            resp.setContentLength(body.length);
            resp.getOutputStream().write(body);
        }
    }
}
