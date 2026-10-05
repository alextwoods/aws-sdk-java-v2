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

package software.amazon.awssdk.smithy.runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Supplier;

/**
 * A class loader that hosts a runtime classpath in isolation from the test classpath and records every class the JVM
 * or library code asks it to resolve.
 *
 * <p>Its parent is the platform class loader, so JDK classes are delegated while every other class, including every
 * Smithy class, must be resolved by this loader. The JVM calls {@link #loadClass(String, boolean)} on the defining
 * loader when it resolves a symbolic class reference, and reflective or {@link java.util.ServiceLoader} lookups made
 * with this loader (it is installed as the thread context class loader while a scenario runs) go through the same
 * method, so the recorded set is the complete set of non-JDK classes the scenario resolved or tried to resolve.
 */
final class IsolatedRuntimeClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final Set<String> requested = new ConcurrentSkipListSet<>();

    IsolatedRuntimeClassLoader(List<Path> classpath) {
        super("smithy-runtime-isolated", toUrls(classpath), ClassLoader.getPlatformClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        requested.add(name);
        return super.loadClass(name, resolve);
    }

    /** The binary names of every class this loader was asked to load, in lexical order. */
    SortedSet<String> requestedClasses() {
        return new TreeSet<>(requested);
    }

    /**
     * Instantiates the named {@link Supplier} inside this loader and runs it with this loader as the thread context
     * class loader. Only JDK types cross the loader boundary.
     */
    String run(String supplierClassName) {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(this);
        try {
            Class<?> type = Class.forName(supplierClassName, true, this);
            if (type.getClassLoader() != this) {
                throw new IllegalStateException(supplierClassName + " was not defined by the isolated loader");
            }
            Object supplier = type.getDeclaredConstructor().newInstance();
            return (String) ((Supplier<?>) supplier).get();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to run " + supplierClassName, e);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static URL[] toUrls(List<Path> classpath) {
        URL[] urls = new URL[classpath.size()];
        for (int i = 0; i < urls.length; i++) {
            try {
                urls[i] = classpath.get(i).toUri().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(classpath.get(i).toString(), e);
            }
        }
        return urls;
    }
}
