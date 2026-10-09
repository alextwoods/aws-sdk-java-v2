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

package software.amazon.awssdk.test.fleet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import software.amazon.awssdk.http.HttpBody;
import software.amazon.awssdk.http.HttpClientCapabilities;
import software.amazon.awssdk.http.HttpExecutionOptions;
import software.amazon.awssdk.http.HttpTransportMode;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.SdkSyncHttpClient;
import software.amazon.awssdk.http.SyncHttpExchange;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;

/**
 * Drives one operation call through a V3 generated client (the fleet artifact on the test class path) or a V2
 * {@value #V2_VERSION_PROPERTY} client (released jars in an isolated class loader, {@link V2Baseline}), both purely
 * reflectively and with the same request members, over a capturing transport that answers with a canned response.
 * Nothing leaves the JVM. Both clients use region {@code us-east-1}, static test credentials and their default
 * endpoint resolution, so endpoint rules (for example S3 virtual hosting) are part of what is compared.
 */
final class Clients {
    static final String V2_VERSION_PROPERTY = "v3.v2Baseline.version";
    private static final String REGION = "us-east-1";
    private static final String ACCESS_KEY = "AKIDEXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

    /** Static test credentials, so the V3 clients never consult the default chain. */
    private static final IdentityProvider<AwsCredentialsIdentity> V3_CREDENTIALS = new IdentityProvider<>() {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return AwsCredentialsIdentity.class;
        }

        @Override
        public CompletableFuture<AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(AwsCredentialsIdentity.create(ACCESS_KEY, SECRET_KEY));
        }
    };

    private Clients() {
    }

    /** A canned HTTP response. */
    record Canned(int status, Map<String, String> headers, byte[] body) {
    }

    /** A captured request before normalization: everything the differential can compare. */
    record Captured(String method, String host, String path, List<String> query, Map<String, List<String>> headers,
                    byte[] body) {
        String header(String name) {
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return String.join(",", e.getValue());
                }
            }
            return null;
        }
    }

    /**
     * The outcome of one call.
     *
     * @param request the request the transport received (null when the client failed before sending)
     * @param result the response-side observation: the output member value, or the exception class, error code and
     *               status code
     */
    record Outcome(Captured request, SortedMap<String, String> result) {
    }

    // -------------------------------------------------------------------------------------------------------------
    // Clients
    // -------------------------------------------------------------------------------------------------------------

    /** Calls {@code operation} on the V3 client of {@code packageName} (application class loader). */
    static Outcome v3(String packageName, String operation, Map<String, Object> input, String outputMember,
                      Function<Captured, Canned> responder) {
        ClassLoader loader = Clients.class.getClassLoader();
        V3Transport transport = new V3Transport(responder);
        Object builder = invokeStatic(clientInterface(loader, packageName), "builder");
        builder = call(builder, "region", region(loader));
        builder = call(builder, "credentialsProvider", V3_CREDENTIALS);
        builder = call(builder, "httpClient", transport);
        return execute(loader, invoke(builder, "build"), operation, input, outputMember, () -> transport.captured);
    }

    /** Calls {@code operation} on the V2 baseline client of {@code packageName}. */
    static Outcome v2(V2Baseline baseline, String packageName, String operation, Map<String, Object> input,
                      String outputMember, Function<Captured, Canned> responder) {
        ClassLoader loader = baseline.loader();
        Captured[] captured = new Captured[1];
        Object transport = baseline.transport(request -> {
            captured[0] = request;
            return responder.apply(request);
        });
        Object builder = invokeStatic(clientInterface(loader, packageName), "builder");
        builder = call(builder, "region", region(loader));
        Object credentials = invokeStatic(load(loader, "software.amazon.awssdk.auth.credentials.AwsBasicCredentials"),
                                          "create", ACCESS_KEY, SECRET_KEY);
        Object provider = invokeStatic(load(loader, "software.amazon.awssdk.auth.credentials.StaticCredentialsProvider"),
                                       "create", credentials);
        builder = call(builder, "credentialsProvider", provider);
        builder = call(builder, "httpClient", transport);
        return execute(loader, invoke(builder, "build"), operation, input, outputMember, () -> captured[0]);
    }

    private static Object region(ClassLoader loader) {
        return invokeStatic(load(loader, "software.amazon.awssdk.regions.Region"), "of", REGION);
    }

    /** The service's sync client interface: the one public {@code *Client} interface with a static builder(). */
    private static Class<?> clientInterface(ClassLoader loader, String packageName) {
        // The client interface name is <Prefix>Client; the prefix is the V2 naming of the SDK ID, found by probing
        // the package for a <Prefix>Client with a static builder() rather than re-deriving the naming.
        List<Class<?>> found = probe(loader, packageName, "Client");
        if (found.size() != 1) {
            throw new AssertionError("Expected one sync client interface in " + packageName + ", found " + found);
        }
        return found.get(0);
    }

    private static List<Class<?>> probe(ClassLoader loader, String packageName, String suffix) {
        List<Class<?>> result = new ArrayList<>();
        String resource = packageName.replace('.', '/');
        try {
            for (URL url : java.util.Collections.list(loader.getResources(resource))) {
                for (String name : entries(url, resource)) {
                    if (name.endsWith(suffix) && !name.endsWith("AsyncClient") && !name.contains("$")) {
                        Class<?> type = load(loader, packageName + "." + name);
                        if (type.isInterface() && hasStatic(type, "builder")) {
                            result.add(type);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private static List<String> entries(URL url, String resource) throws IOException {
        List<String> names = new ArrayList<>();
        if ("jar".equals(url.getProtocol())) {
            java.net.JarURLConnection connection = (java.net.JarURLConnection) url.openConnection();
            connection.setUseCaches(false);
            try (java.util.jar.JarFile jar = connection.getJarFile()) {
                jar.stream().map(java.util.jar.JarEntry::getName)
                   .filter(n -> n.startsWith(resource + "/") && n.endsWith(".class")
                                && n.indexOf('/', resource.length() + 1) < 0)
                   .forEach(n -> names.add(n.substring(resource.length() + 1, n.length() - ".class".length())));
            }
        }
        return names;
    }

    private static boolean hasStatic(Class<?> type, String method) {
        try {
            return Modifier.isStatic(type.getMethod(method).getModifiers());
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Outcome execute(ClassLoader loader, Object client, String operation, Map<String, Object> input,
                                   String outputMember, java.util.function.Supplier<Captured> captured) {
        String methodName = Character.toLowerCase(operation.charAt(0)) + operation.substring(1);
        // The plain (Request) overload, or the (Request, RequestBody | ResponseTransformer) overload of a streaming
        // operation; never the V3-only (Request, AwsRequestOverrideConfiguration) overload or a Consumer overload.
        Method method = null;
        for (Method m : client.getClass().getInterfaces()[0].getMethods()) {
            if (!m.getName().equals(methodName) || m.getParameterCount() == 0
                || !m.getParameterTypes()[0].getSimpleName().endsWith("Request")) {
                continue;
            }
            if (streamingParameter(m)) {
                method = m;
                break;
            }
            if (m.getParameterCount() == 1) {
                method = m;
            }
        }
        if (method == null) {
            // Reported, not thrown: a client that has no method for a modeled operation is a finding of the case.
            closeQuietly(client);
            SortedMap<String, String> missing = new TreeMap<>();
            missing.put("response.outcome", "no " + methodName + "(Request) method on the client");
            return new Outcome(null, missing);
        }
        Object request = request(loader, method.getParameterTypes()[0], input);
        List<Object> arguments = new ArrayList<>(List.of(request));
        if (method.getParameterCount() == 2) {
            Class<?> second = method.getParameterTypes()[1];
            if (second.getSimpleName().equals("RequestBody")) {
                Object body = input.values().stream().filter(v -> v.equals("fleet-differential-body")).findFirst()
                                   .orElse("");
                arguments.add(invokeStatic(second, "fromString", body));
            } else {
                arguments.add(invokeStatic(load(loader, "software.amazon.awssdk.core.sync.ResponseTransformer"),
                                           "toBytes"));
            }
        }
        SortedMap<String, String> result = new TreeMap<>();
        try (AutoCloseable ignored = (AutoCloseable) client) {
            Object response = method.invoke(client, arguments.toArray());
            if (response != null && response.getClass().getSimpleName().equals("ResponseBytes")) {
                response = invoke(response, "response");
            }
            result.put("response.outcome", "success");
            if (outputMember != null) {
                result.put("response.value", String.valueOf(getter(response, outputMember)));
            }
        } catch (InvocationTargetException e) {
            Throwable failure = e.getCause();
            result.put("response.outcome", "exception");
            result.put("response.exception", failure.getClass().getSimpleName());
            Object details = optional(failure, "awsErrorDetails");
            result.put("response.errorCode", String.valueOf(details == null ? null : optional(details, "errorCode")));
            result.put("response.statusCode", String.valueOf(optional(failure, "statusCode")));
        } catch (Exception e) {
            throw new AssertionError(method + " with " + arguments.stream().map(a -> a.getClass().getName() + "@"
                                                                                + a.getClass().getClassLoader())
                                                                      .toList(), e);
        }
        return new Outcome(captured.get(), result);
    }

    private static void closeQuietly(Object client) {
        try {
            ((AutoCloseable) client).close();
        } catch (Exception e) {
            // Nothing to release: the transport is in-memory.
        }
    }

    private static boolean streamingParameter(Method m) {
        String name = m.getParameterTypes()[m.getParameterCount() - 1].getSimpleName();
        return m.getParameterCount() == 2 && (name.equals("RequestBody") || name.equals("ResponseTransformer"));
    }

    /** Builds a request through its builder, setting each member through its (case-insensitive) setter. */
    private static Object request(ClassLoader loader, Class<?> type, Map<String, Object> input) {
        Object builder = invokeStatic(type, "builder");
        for (Map.Entry<String, Object> member : input.entrySet()) {
            Object value = member.getValue();
            if (value.equals("fleet-differential-body")) {
                continue;
            }
            Method setter = setter(builder.getClass(), member.getKey(), value);
            try {
                setter.setAccessible(true);
                setter.invoke(builder, value);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Setting " + member.getKey() + " on " + type.getName(), e);
            }
        }
        return invoke(builder, "build");
    }

    private static Method setter(Class<?> builderType, String member, Object value) {
        Class<?> wanted = value instanceof String ? String.class
                          : value instanceof Boolean ? Boolean.class
                          : value instanceof Integer ? Integer.class
                          : value instanceof Long ? Long.class
                          : value instanceof Instant ? Instant.class : value.getClass();
        for (Class<?> type : publicTypes(builderType)) {
            for (Method m : type.getMethods()) {
                if (m.getName().equalsIgnoreCase(member) && m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == wanted) {
                    return m;
                }
            }
        }
        throw new AssertionError("No " + member + "(" + wanted.getSimpleName() + ") setter on " + builderType);
    }

    private static List<Class<?>> publicTypes(Class<?> type) {
        List<Class<?>> result = new ArrayList<>();
        for (Class<?> t = type; t != null; t = t.getSuperclass()) {
            if (Modifier.isPublic(t.getModifiers())) {
                result.add(t);
            }
            for (Class<?> i : t.getInterfaces()) {
                result.add(i);
            }
        }
        return result;
    }

    private static Object getter(Object response, String member) {
        for (Class<?> type : publicTypes(response.getClass())) {
            for (Method m : type.getMethods()) {
                if (m.getName().equalsIgnoreCase(member) && m.getParameterCount() == 0) {
                    try {
                        return m.invoke(response);
                    } catch (ReflectiveOperationException e) {
                        throw new AssertionError(e);
                    }
                }
            }
        }
        throw new AssertionError("No " + member + "() getter on " + response.getClass());
    }

    private static Object optional(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------------------------------------------
    // Reflection helpers
    // -------------------------------------------------------------------------------------------------------------

    static Class<?> load(ClassLoader loader, String name) {
        try {
            return Class.forName(name, true, loader);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(name + " is not loadable from " + loader, e);
        }
    }

    static Object invokeStatic(Class<?> type, String method, Object... args) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(method) && Modifier.isStatic(m.getModifiers()) && accepts(m, args)) {
                try {
                    return m.invoke(null, args);
                } catch (InvocationTargetException e) {
                    throw new AssertionError(type.getName() + "." + method + " failed", e.getCause());
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
        }
        throw new AssertionError("No static " + method + " on " + type.getName());
    }

    static Object invoke(Object target, String method) {
        return call(target, method);
    }

    /** Invokes the public (interface-declared) instance method {@code method} that accepts {@code args}. */
    static Object call(Object target, String method, Object... args) {
        for (Class<?> type : publicTypes(target.getClass())) {
            for (Method m : type.getMethods()) {
                if (m.getName().equals(method) && !Modifier.isStatic(m.getModifiers()) && accepts(m, args)) {
                    try {
                        return m.invoke(target, args);
                    } catch (InvocationTargetException e) {
                        throw new AssertionError(target.getClass().getName() + "." + method + " failed",
                                                 e.getCause());
                    } catch (IllegalAccessException e) {
                        continue;
                    }
                }
            }
        }
        throw new AssertionError("No " + method + " accepting " + List.of(args) + " on " + target.getClass());
    }

    private static boolean accepts(Method m, Object[] args) {
        if (m.getParameterCount() != args.length) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            Class<?> p = m.getParameterTypes()[i];
            if (p == int.class) {
                p = Integer.class;
            }
            if (args[i] != null && !p.isInstance(args[i])) {
                return false;
            }
        }
        return true;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Transports
    // -------------------------------------------------------------------------------------------------------------

    /** The V3 capturing sync transport. */
    static final class V3Transport implements SdkSyncHttpClient {
        private final Function<Captured, Canned> responder;
        private Captured captured;

        V3Transport(Function<Captured, Canned> responder) {
            this.responder = responder;
        }

        @Override
        public SyncHttpExchange prepare(SdkHttpRequest request, HttpExecutionOptions options) {
            return new SyncHttpExchange() {
                @Override
                public SdkHttpResponse execute() {
                    Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                    for (int i = 0; i < request.headers().size(); i++) {
                        headers.computeIfAbsent(request.headers().name(i), k -> new ArrayList<>())
                               .add(request.headers().value(i));
                    }
                    captured = new Captured(request.method(), request.host(), request.encodedPath(),
                                            Wire.decodeQuery(request.encodedQuery()), headers,
                                            request.body().buffered().asByteArray());
                    Canned canned = responder.apply(captured);
                    SdkHttpResponse.Builder response = SdkHttpResponse.builder().statusCode(canned.status());
                    canned.headers().forEach(response::addHeader);
                    return response.body(HttpBody.fromBytes(canned.body())).build();
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
        public HttpClientCapabilities capabilities() {
            return HttpClientCapabilities.builder().mode(HttpTransportMode.SYNC).addProtocol(Protocol.HTTP1_1)
                                         .maxBufferedBytesPerExchange(1 << 20)
                                         .cancellationTimeout(Duration.ofSeconds(1)).build();
        }

        @Override
        public String clientName() {
            return "FleetDifferential";
        }

        @Override
        public void close() {
        }
    }

    /**
     * The frozen V2 baseline: the released V2 jars of {@code target/v2-baseline} in a class loader whose parent is the
     * platform loader, so no V3 class (V2 and V3 share class names) is visible to it.
     */
    static final class V2Baseline implements AutoCloseable {
        private final URLClassLoader loader;

        private V2Baseline(URLClassLoader loader) {
            this.loader = loader;
        }

        static V2Baseline open(Path directory) {
            List<URL> urls = new ArrayList<>();
            try (Stream<Path> jars = Files.list(directory)) {
                for (Path jar : jars.filter(p -> p.toString().endsWith(".jar")).sorted().toList()) {
                    urls.add(jar.toUri().toURL());
                }
            } catch (IOException e) {
                throw new UncheckedIOException("No V2 baseline at " + directory, e);
            }
            if (urls.isEmpty()) {
                throw new AssertionError("No V2 baseline jars in " + directory);
            }
            return new V2Baseline(new URLClassLoader("v2-baseline", urls.toArray(URL[]::new),
                                                     ClassLoader.getPlatformClassLoader()));
        }

        ClassLoader loader() {
            return loader;
        }

        /** A V2 {@code SdkHttpClient} (a proxy in the V2 loader) that captures each request and answers it. */
        Object transport(Function<Captured, Canned> responder) {
            Class<?> clientType = load(loader, "software.amazon.awssdk.http.SdkHttpClient");
            Class<?> executableType = load(loader, "software.amazon.awssdk.http.ExecutableHttpRequest");
            return Proxy.newProxyInstance(loader, new Class<?>[] {clientType}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "prepareRequest":
                        Object execute = args[0];
                        return Proxy.newProxyInstance(loader, new Class<?>[] {executableType}, (p, m, a) -> {
                            if (m.getName().equals("call")) {
                                return respond(execute, responder);
                            }
                            return null;
                        });
                    case "clientName":
                        return "FleetDifferential";
                    case "close":
                        return null;
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    case "toString":
                        return "FleetDifferentialV2Transport";
                    default:
                        if (method.isDefault()) {
                            return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                        }
                        return null;
                }
            });
        }

        @SuppressWarnings("unchecked")
        private Object respond(Object execute, Function<Captured, Canned> responder) throws IOException {
            Object request = invoke(execute, "httpRequest");
            Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            headers.putAll((Map<String, List<String>>) invoke(request, "headers"));
            List<String> query = new ArrayList<>();
            ((Map<String, List<String>>) invoke(request, "rawQueryParameters"))
                .forEach((name, values) -> values.forEach(v -> query.add(name + "=" + (v == null ? "" : v))));
            query.sort(null);
            byte[] body = new byte[0];
            java.util.Optional<?> provider = (java.util.Optional<?>) invoke(execute, "contentStreamProvider");
            if (provider.isPresent()) {
                try (InputStream in = (InputStream) invoke(provider.get(), "newStream")) {
                    body = in.readAllBytes();
                }
            }
            Captured captured = new Captured(String.valueOf(invoke(request, "method")).toUpperCase(Locale.ROOT),
                                             (String) invoke(request, "host"), (String) invoke(request, "encodedPath"),
                                             query, headers, body);
            Canned canned = responder.apply(captured);
            Object response = invokeStatic(load(loader, "software.amazon.awssdk.http.SdkHttpResponse"), "builder");
            response = call(response, "statusCode", canned.status());
            for (Map.Entry<String, String> header : canned.headers().entrySet()) {
                response = call(response, "putHeader", header.getKey(), header.getValue());
            }
            response = invoke(response, "build");
            Object stream = invokeStatic(load(loader, "software.amazon.awssdk.http.AbortableInputStream"), "create",
                                         new ByteArrayInputStream(canned.body()));
            Object result = invokeStatic(load(loader, "software.amazon.awssdk.http.HttpExecuteResponse"), "builder");
            result = call(result, "response", response);
            result = call(result, "responseBody", stream);
            return invoke(result, "build");
        }

        @Override
        public void close() throws IOException {
            loader.close();
        }
    }
}
