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

package software.amazon.awssdk.test.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.abort;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import software.amazon.awssdk.test.reactor.JdkInstallations.Jdk;

/**
 * Materializes Maven build fixtures: a clean checkout of {@code v3/} (git-visible files only, so no build output from
 * the outer build) plus the fixture projects under {@code src/test/fixtures}, copied beside it to
 * {@code <checkout>/fixtures/<fixture>} so that they inherit the real V3 parent POM through their relativePath while
 * the checked-out {@code v3/} tree stays exactly what git would check out (V3 architecture tests discover modules by
 * walking that tree).
 */
final class BuildFixtures {
    /** Name of the stand-alone Java 21 fixture project under {@code src/test/fixtures}. */
    static final String BASELINE_FIXTURE = "jdk-baseline-fixture";

    static final Path V3_ROOT = realPath(System.getProperty("v3.root"));

    private BuildFixtures() {
    }

    /**
     * The pinned checkout of the AWS models repository, relative to the repository root. It is an external input
     * outside {@code v3/} that modules such as {@code core/endpoints} read in place (through
     * {@code ${project.basedir}/../../../.kiro/reference/aws-models}), so it is not part of the clean checkout.
     */
    static final String CANONICAL_MODELS = ".kiro/reference/aws-models";

    /** Creates {@code <work>/checkout} (replacing any previous content) and returns it. */
    static Path checkout(Path work) throws IOException, InterruptedException {
        Path checkout = work.resolve("checkout");
        CleanCheckout.create(V3_ROOT.getParent(), V3_ROOT.getFileName().toString(), checkout);
        linkCanonicalModels(checkout);
        Path fixtures = Path.of(System.getProperty("v3.fixtures.directory"));
        copyTree(fixtures.resolve(BASELINE_FIXTURE), baselineFixture(checkout));
        assertEquals(List.of(), classFiles(checkout), "a clean checkout contains no class files");
        return checkout;
    }

    /** Arguments that build the baseline fixture project, followed by {@code extraArguments}. */
    static List<String> baselineFixtureArguments(String... extraArguments) {
        List<String> arguments = new ArrayList<>(List.of("-f", "fixtures/" + BASELINE_FIXTURE + "/pom.xml", "verify"));
        arguments.addAll(List.of(extraArguments));
        return arguments;
    }

    /**
     * One dynamic test per JDK, or, when there is none, one aborted (skipped) test whose message carries
     * {@code skipReason} and the discovery report, so that the skip and its reason appear in the surefire report.
     */
    static Stream<DynamicTest> perJdk(JdkInstallations discovered, List<Jdk> jdks, String skipReason, JdkCase test) {
        if (jdks.isEmpty()) {
            return Stream.of(dynamicTest("no matching JDK", () -> abort(skipReason + discovered.describe())));
        }
        return jdks.stream().map(jdk -> dynamicTest(jdk.toString(), () -> test.run(jdk)));
    }

    /** A fixture case run with one JDK. */
    @FunctionalInterface
    interface JdkCase {
        void run(Jdk jdk) throws Exception;
    }

    static Path baselineFixture(Path checkout) {
        return checkout.resolve("fixtures").resolve(BASELINE_FIXTURE);
    }

    static List<Path> classFiles(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.getFileName().toString().endsWith(".class")).toList();
        }
    }

    /**
     * Exposes the outer repository's pinned canonical models at the same location relative to the checkout's
     * {@code v3/}, as a symbolic link (the models are read-only inputs and too large to copy). When the outer
     * repository has no models, nothing is linked and the forked build reports the missing input exactly as the outer
     * build would.
     */
    private static void linkCanonicalModels(Path checkout) throws IOException {
        Path models = V3_ROOT.getParent().resolve(CANONICAL_MODELS);
        if (!Files.isDirectory(models)) {
            return;
        }
        Path link = checkout.resolve(CANONICAL_MODELS);
        Files.createDirectories(link.getParent());
        Files.createSymbolicLink(link, models.toRealPath());
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : walk.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Path realPath(String path) {
        try {
            return Path.of(path).toRealPath();
        } catch (IOException e) {
            throw new IllegalStateException("v3.root " + path + " does not exist", e);
        }
    }
}
