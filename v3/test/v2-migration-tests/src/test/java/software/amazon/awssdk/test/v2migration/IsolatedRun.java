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

package software.amazon.awssdk.test.v2migration;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs a corpus project's behavioral entry point ({@code public static String run(String endpoint)}) in its own class
 * loader over exactly its compiled classes and its SDK class path, with the platform class loader as parent, so the
 * V2 baseline and the V3 candidate never share a class and nothing from this test's class path leaks in.
 */
final class IsolatedRun {
    private static final long TIMEOUT_SECONDS = 120;

    /** The transcript the entry point returned, or the failure it threw. */
    record Outcome(String transcript, String failure) {
        boolean succeeded() {
            return failure == null;
        }
    }

    private IsolatedRun() {
    }

    static Outcome run(Path classes, List<Path> classpath, String entryPoint, String endpoint) {
        List<URL> urls = new ArrayList<>();
        try {
            urls.add(classes.toUri().toURL());
            for (Path p : classpath) {
                urls.add(p.toUri().toURL());
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "corpus-" + entryPoint);
            t.setDaemon(true);
            return t;
        });
        try (URLClassLoader loader = new URLClassLoader("corpus", urls.toArray(URL[]::new),
                                                        ClassLoader.getPlatformClassLoader())) {
            Future<String> result = executor.submit(() -> {
                Thread.currentThread().setContextClassLoader(loader);
                Method run = Class.forName(entryPoint, true, loader).getMethod("run", String.class);
                return (String) run.invoke(null, endpoint);
            });
            return new Outcome(result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS), null);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() instanceof InvocationTargetException ite ? ite.getCause() : e.getCause();
            return new Outcome(null, stackTrace(cause));
        } catch (TimeoutException e) {
            return new Outcome(null, "The entry point did not return within " + TIMEOUT_SECONDS + " seconds");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Outcome(null, "Interrupted");
        } catch (IOException e) {
            return new Outcome(null, stackTrace(e));
        } finally {
            executor.shutdownNow();
        }
    }

    private static String stackTrace(Throwable t) {
        StringWriter out = new StringWriter();
        t.printStackTrace(new PrintWriter(out));
        return out.toString();
    }
}
