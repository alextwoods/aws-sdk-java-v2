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

package software.amazon.awssdk.benchmark.harness;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A running mock-service process. {@link #start} launches the spec, waits for the process's own {@code READY} line
 * (matching its pid), and from then on copies its output to a log file.
 */
public final class MockServiceProcess implements AutoCloseable {

    private final Process process;
    private final Map<String, String> ready;
    private final Path log;

    private MockServiceProcess(Process process, Map<String, String> ready, Path log) {
        this.process = process;
        this.ready = ready;
        this.log = log;
    }

    public static MockServiceProcess start(JvmLaunchSpec spec, Path log, Duration timeout)
            throws IOException, InterruptedException, TimeoutException {
        Files.createDirectories(log.toAbsolutePath().getParent());
        Process p = new ProcessBuilder(spec.command()).redirectErrorStream(true).start();
        p.getOutputStream().close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter out = Files.newBufferedWriter(log, StandardCharsets.UTF_8);
        CompletableFuture<Map<String, String>> readyLine = CompletableFuture.supplyAsync(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.write(line);
                    out.newLine();
                    out.flush();
                    if (line.startsWith("READY ")) {
                        return parseReady(line);
                    }
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        });
        Map<String, String> ready;
        try {
            ready = readyLine.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            ready = null;
        } catch (TimeoutException e) {
            p.destroyForcibly();
            out.close();
            throw new TimeoutException("mock service not READY within " + timeout + "; see " + log);
        }
        if (ready == null || !Long.toString(p.pid()).equals(ready.get("pid"))) {
            p.destroyForcibly();
            p.waitFor(10, TimeUnit.SECONDS);
            out.close();
            throw new IOException("mock service exited or printed a foreign READY line (" + ready + "); see " + log);
        }
        // Keep draining so the service never blocks on a full pipe.
        Thread drain = new Thread(() -> {
            try (reader; out) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.write(line);
                    out.newLine();
                }
            } catch (IOException ignored) {
                // process ended
            }
        }, "mock-service-log");
        drain.setDaemon(true);
        drain.start();
        return new MockServiceProcess(p, ready, log);
    }

    /** {@code READY k=v k=v ...} -> map. */
    static Map<String, String> parseReady(String line) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String token : line.substring("READY ".length()).trim().split("\\s+")) {
            int eq = token.indexOf('=');
            if (eq > 0) {
                m.put(token.substring(0, eq), token.substring(eq + 1));
            }
        }
        return m;
    }

    public long pid() {
        return process.pid();
    }

    public int port() {
        return Integer.parseInt(ready.get("port"));
    }

    public URI endpoint() {
        return URI.create("http://127.0.0.1:" + port());
    }

    public Map<String, String> ready() {
        return ready;
    }

    public Path log() {
        return log;
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public void close() throws InterruptedException {
        process.destroy();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
        }
    }
}
