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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * {@link PerformanceReferences#V2_APPLICABILITY} equals the built V2 reference client's own applicability
 * ({@code V2ReferenceMain}'s protocol, path, kind and transport ids), and both accept exactly the same cells of the
 * full matrix. {@code V2ReferenceMain} is loaded from the reference's V2-only class path in an isolated class loader
 * (parent: the platform loader), so its V2 classes never meet this module's V3 classes. Needs the built
 * {@code test/sdk-benchmarks-v2-reference} (the reactor builds it first), like the forked smoke.
 */
class V2ReferenceApplicabilityTest {

    @Test
    void harnessApplicabilityEqualsTheV2ReferenceMain() throws Exception {
        PerformanceReferences.ClientLookup lookup =
            PerformanceReferences.v2Client(Path.of(System.getProperty("user.dir")).toAbsolutePath().getParent());
        assertNull(lookup.problem(), "build test/sdk-benchmarks-v2-reference first");
        assertMainMatches(lookup, PerformanceReferences.V2_APPLICABILITY);
    }

    /**
     * Loads {@code lookup}'s reference main from its own class path in an isolated class loader and checks that its
     * {@code PROTOCOLS}, {@code PATHS}, {@code KINDS}, {@code TRANSPORTS} and {@code appliesTo} equal
     * {@code expected} over the full matrix.
     */
    static void assertMainMatches(PerformanceReferences.ClientLookup lookup,
                                  PerformanceReferences.Applicability expected) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String entry : lookup.classpath().split(File.pathSeparator)) {
            urls.add(Path.of(entry).toUri().toURL());
        }
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]),
                                                        ClassLoader.getPlatformClassLoader())) {
            Class<?> main = Class.forName(lookup.mainClass(), false, loader);
            assertEquals(ids(expected.protocols().stream().map(BenchProtocol::id)), field(main, "PROTOCOLS"));
            assertEquals(ids(expected.paths().stream().map(ClientPath::id)), field(main, "PATHS"));
            assertEquals(ids(expected.kinds().stream().map(WorkloadKind::id)), field(main, "KINDS"));
            assertEquals(ids(expected.transports().stream().map(Transport::id)), field(main, "TRANSPORTS"));

            Class<?> cellId = Class.forName(CellId.class.getName(), false, loader);
            Method parse = cellId.getMethod("parse", String.class);
            Method appliesTo = main.getMethod("appliesTo", cellId);
            int covered = 0;
            for (BenchmarkCell cell : CellMatrix.full()) {
                boolean reference = (boolean) appliesTo.invoke(null, parse.invoke(null, cell.id()));
                assertEquals(expected.contains(cell), reference, cell.id());
                covered += reference ? 1 : 0;
            }
            assertTrue(covered > 0);
        }
    }

    private static Set<String> ids(java.util.stream.Stream<String> ids) {
        return ids.collect(Collectors.toCollection(TreeSet::new));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> field(Class<?> main, String name) throws ReflectiveOperationException {
        return new TreeSet<>((Set<String>) main.getField(name).get(null));
    }
}
