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

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** A local JDK HTTP server with scripted routes, recording every request. */
final class LocalServer implements AutoCloseable {
    record Request(String method, String path, String query, Map<String, List<String>> headers, String body) {
        String header(String name) {
            return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name))
                          .map(e -> e.getValue().get(0)).findFirst().orElse(null);
        }
    }

    record Response(int status, String body) {
    }

    private final HttpServer server;
    private final Map<String, Function<Request, Response>> routes = new ConcurrentHashMap<>();
    final List<Request> requests = new CopyOnWriteArrayList<>();

    LocalServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Request request = new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                                          exchange.getRequestURI().getRawQuery(), Map.copyOf(exchange.getRequestHeaders()),
                                          body);
            requests.add(request);
            Function<Request, Response> route = routes.get(request.method() + " " + request.path());
            Response response = route == null ? new Response(404, "") : route.apply(request);
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(response.status(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();
    }

    LocalServer route(String method, String path, Function<Request, Response> handler) {
        routes.put(method + " " + path, handler);
        return this;
    }

    LocalServer route(String method, String path, int status, String body) {
        return route(method, path, r -> new Response(status, body));
    }

    String endpoint() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    URI uri() {
        return URI.create(endpoint());
    }

    long count(String method, String path) {
        return requests.stream().filter(r -> r.method().equals(method) && r.path().equals(path)).count();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
