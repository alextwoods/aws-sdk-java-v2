package software.amazon.awssdk.benchmark.smithyjava.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Lightweight Jetty server that returns a fixed response for all requests.
 * Used to benchmark the client pipeline without network variance.
 */
public final class MockHttpServer {

    private final Server server;
    private final int port;

    public MockHttpServer(byte[] responseBody, String contentType) throws IOException {
        this(responseBody, contentType, new String[0][], 0);
    }

    /**
     * Create a mock server on a specific port (0 = ephemeral) with specific headers.
     */
    public MockHttpServer(byte[] responseBody, String contentType, String[][] headers, int fixedPort)
            throws IOException {
        port = fixedPort > 0 ? fixedPort : getUnusedPort();
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});

        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(new FixedResponseServlet(responseBody, contentType, headers)), "/*");
        server.setHandler(context);
    }

    /**
     * Create a mock server that also returns specific headers.
     */
    public MockHttpServer(byte[] responseBody, String contentType, String[][] headers) throws IOException {
        port = getUnusedPort();
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});

        ServletContextHandler context = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(new FixedResponseServlet(responseBody, contentType, headers)), "/*");
        server.setHandler(context);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

    public URI getHttpUri() {
        return URI.create("http://localhost:" + port);
    }

    public int getPort() {
        return port;
    }

    public static byte[] loadFixture(String path) throws IOException {
        try (InputStream is = MockHttpServer.class.getClassLoader().getResourceAsStream("fixtures/" + path)) {
            if (is == null) {
                throw new IOException("Fixture not found: fixtures/" + path);
            }
            return is.readAllBytes();
        }
    }

    private static int getUnusedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static final class FixedResponseServlet extends HttpServlet {
        private final byte[] body;
        private final String contentType;
        private final String[][] headers;

        private FixedResponseServlet(byte[] body, String contentType) {
            this(body, contentType, new String[0][]);
        }

        private FixedResponseServlet(byte[] body, String contentType, String[][] headers) {
            this.body = body;
            this.contentType = contentType;
            this.headers = headers;
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Consume request body to be realistic
            req.getInputStream().readAllBytes();
            resp.setStatus(200);
            resp.setContentLength(body.length);
            resp.setContentType(contentType);
            for (String[] header : headers) {
                resp.setHeader(header[0], header[1]);
            }
            resp.getOutputStream().write(body);
        }
    }
}
