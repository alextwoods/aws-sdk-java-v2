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

package software.amazon.awssdk.sizereport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Builds the Size_Report (Requirement 36) from the Baseline_Build and feature build JARs.
 *
 * <p>The Baseline_Build directory is written by {@code scripts/schema-serde/build-size-baseline.sh}: it holds
 * {@code jars/<artifactId>.jar} (each service and its {@code software.amazon.awssdk} runtime dependencies) and
 * {@code build-info.properties}. The feature directory holds the same JARs from the feature build.
 *
 * <p>{@link #generate()} fails if the two builds were not compiled with the same JDK version and compiler options
 * (Requirement 36.7), as far as the JARs show it: the Baseline_Build's recorded JDK must be the JDK running the
 * report, the service JARs' {@code Build-Jdk-Spec} must match, and the classes must have the same
 * {@link ClassFileFingerprint}.
 */
@SdkInternalApi
public final class SizeReport {
    static final String BUILD_SCRIPT = "scripts/schema-serde/build-size-baseline.sh";

    private final Path baselineDir;
    private final Path featureJarsDir;
    private final Map<String, String> services;
    private final Path disabledCustomizations;
    private final String featureJavaVersion;
    private final String featureJavaVendor;

    private SizeReport(Builder builder) {
        this.baselineDir = Objects.requireNonNull(builder.baselineDir, "baselineDir");
        this.featureJarsDir = Objects.requireNonNull(builder.featureJarsDir, "featureJarsDir");
        this.services = Collections.unmodifiableMap(new LinkedHashMap<>(builder.services));
        this.disabledCustomizations = Objects.requireNonNull(builder.disabledCustomizations, "disabledCustomizations");
        this.featureJavaVersion = Objects.requireNonNull(builder.featureJavaVersion, "featureJavaVersion");
        this.featureJavaVendor = Objects.requireNonNull(builder.featureJavaVendor, "featureJavaVendor");
        if (services.isEmpty()) {
            throw new IllegalArgumentException("The size report needs at least one service.");
        }
    }

    /**
     * A builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parse a service list of the form {@code artifactId=Display name,artifactId=Display name}.
     */
    public static Map<String, String> parseServices(String spec) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String item : spec.split(",")) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0 || eq == trimmed.length() - 1) {
                throw new IllegalArgumentException("Expected artifactId=Display name, got '" + trimmed + "' in '"
                                                   + spec + "'");
            }
            result.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
        }
        return result;
    }

    /**
     * Build the report and render it as Markdown.
     *
     * @throws IllegalStateException if an input is missing or the builds were not compiled the same way
     */
    public String generate() throws IOException {
        Properties buildInfo = loadBuildInfo();
        Map<String, JarContents> baselineJars = readJars(baselineDir.resolve("jars"), "Baseline_Build");
        Map<String, JarContents> featureJars = readJars(featureJarsDir, "feature build");

        String baselineJavaVersion = buildInfo.getProperty("java.version", "");
        String baselineJavaVendor = buildInfo.getProperty("java.vendor", "");
        if (!baselineJavaVersion.equals(featureJavaVersion) || !baselineJavaVendor.equals(featureJavaVendor)) {
            throw new IllegalStateException(
                "The Baseline_Build in " + baselineDir + " was compiled with JDK " + baselineJavaVersion + " ("
                + baselineJavaVendor + "), but this build runs JDK " + featureJavaVersion + " (" + featureJavaVendor
                + "). Requirement 36.7 needs the same JDK: rerun " + BUILD_SCRIPT + " with this JDK.");
        }

        List<ServiceComparison> comparisons = new ArrayList<>();
        List<byte[]> baselineClasses = new ArrayList<>();
        List<byte[]> featureClasses = new ArrayList<>();
        for (Map.Entry<String, String> service : services.entrySet()) {
            JarContents baseline = requireJar(baselineJars, service.getKey(), "Baseline_Build",
                                              baselineDir.resolve("jars"));
            JarContents feature = requireJar(featureJars, service.getKey(), "feature build", featureJarsDir);
            if (!Objects.equals(baseline.buildJdkSpec(), feature.buildJdkSpec())) {
                throw new IllegalStateException(
                    service.getKey() + ": the Baseline_Build JAR was built by JDK " + baseline.buildJdkSpec()
                    + " and the feature JAR by JDK " + feature.buildJdkSpec() + " (Build-Jdk-Spec). Rebuild one of"
                    + " them with the other's JDK (Requirement 36.7).");
            }
            baselineClasses.addAll(baseline.classes().values());
            featureClasses.addAll(feature.classes().values());
            comparisons.add(new ServiceComparison(service.getKey(), service.getValue(), ServiceSizes.measure(baseline),
                                                  ServiceSizes.measure(feature)));
        }
        ClassFileFingerprint baselineFingerprint = ClassFileFingerprint.of(baselineClasses);
        ClassFileFingerprint featureFingerprint = ClassFileFingerprint.of(featureClasses);
        if (!baselineFingerprint.equals(featureFingerprint)) {
            throw new IllegalStateException("The service class files show different compiler options (Requirement"
                                            + " 36.7). Baseline_Build: " + baselineFingerprint + "; feature build: "
                                            + featureFingerprint);
        }
        ModuleComparison modules = ModuleComparison.compare(baselineJars, featureJars, services.keySet());

        Map<String, List<String>> disabled = new LinkedHashMap<>();
        for (Map.Entry<String, String> service : services.entrySet()) {
            disabled.put(service.getValue(), DisabledCustomizations.forService(disabledCustomizations,
                                                                               service.getValue()));
        }

        return new SizeReportWriter(buildInfo, featureJavaVersion, featureJavaVendor, baselineFingerprint,
                                    featureFingerprint, disabled, comparisons, modules).render();
    }

    private Properties loadBuildInfo() throws IOException {
        Path file = baselineDir.resolve("build-info.properties");
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException(
                "No Baseline_Build in " + baselineDir + ". Build it once with " + BUILD_SCRIPT + " --modules "
                + String.join(",", services.keySet()) + " (see test/size-report/README.md), or point"
                + " -Dsize-report.baselineDir at an existing one.");
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        }
        List<String> built = Arrays.asList(properties.getProperty("baseline.modules", "").split(","));
        List<String> missing = new ArrayList<>();
        for (String service : services.keySet()) {
            if (!built.contains(service)) {
                missing.add(service);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                "The Baseline_Build in " + baselineDir + " does not include " + missing + ". Rerun " + BUILD_SCRIPT
                + " --modules " + String.join(",", services.keySet()) + ".");
        }
        return properties;
    }

    private static Map<String, JarContents> readJars(Path dir, String build) throws IOException {
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("No " + build + " JAR directory at " + dir);
        }
        Map<String, JarContents> result = new TreeMap<>();
        try (DirectoryStream<Path> jars = Files.newDirectoryStream(dir, "*.jar")) {
            for (Path jar : jars) {
                String file = jar.toFile().getName();
                result.put(file.substring(0, file.length() - ".jar".length()), JarContents.read(jar));
            }
        }
        return result;
    }

    private static JarContents requireJar(Map<String, JarContents> jars, String artifactId, String build, Path dir) {
        JarContents jar = jars.get(artifactId);
        if (jar == null) {
            throw new IllegalStateException("No " + build + " JAR for " + artifactId + " in " + dir);
        }
        return jar;
    }

    /**
     * Inputs of a {@link SizeReport}.
     */
    public static final class Builder {
        private Path baselineDir;
        private Path featureJarsDir;
        private Map<String, String> services = new LinkedHashMap<>();
        private Path disabledCustomizations;
        private String featureJavaVersion;
        private String featureJavaVendor;

        private Builder() {
        }

        /**
         * The directory written by {@code build-size-baseline.sh}.
         */
        public Builder baselineDir(Path baselineDir) {
            this.baselineDir = baselineDir;
            return this;
        }

        /**
         * The directory of feature build JARs, named {@code <artifactId>.jar}.
         */
        public Builder featureJarsDir(Path featureJarsDir) {
            this.featureJarsDir = featureJarsDir;
            return this;
        }

        /**
         * The reported services, artifactId to display name, in report order.
         */
        public Builder services(Map<String, String> services) {
            this.services = new LinkedHashMap<>(services);
            return this;
        }

        /**
         * {@code docs/design/schema-serde/disabled-customizations.md}.
         */
        public Builder disabledCustomizations(Path disabledCustomizations) {
            this.disabledCustomizations = disabledCustomizations;
            return this;
        }

        /**
         * The JDK that compiled the feature build: the {@code java.version} and {@code java.vendor} of the JDK that
         * runs the build.
         */
        public Builder featureJdk(String javaVersion, String javaVendor) {
            this.featureJavaVersion = javaVersion;
            this.featureJavaVendor = javaVendor;
            return this;
        }

        public SizeReport build() {
            return new SizeReport(this);
        }
    }

    /**
     * Reads the disabled-customizations table (Requirement 33.7) for Requirement 36.7.
     */
    static final class DisabledCustomizations {
        private DisabledCustomizations() {
        }

        /**
         * The rows of the table whose service is {@code displayName}, as {@code customization (reason)}.
         */
        static List<String> forService(Path file, String displayName) throws IOException {
            if (!Files.isRegularFile(file)) {
                throw new IllegalStateException("No disabled-customizations table at " + file);
            }
            List<String> result = new ArrayList<>();
            boolean inTable = false;
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!inTable) {
                    inTable = line.startsWith("| Service |");
                    continue;
                }
                if (line.startsWith("|---")) {
                    continue;
                }
                if (!line.startsWith("|")) {
                    break;
                }
                String[] cells = line.substring(1).split("\\|");
                if (cells.length >= 3 && cells[0].trim().equals(displayName)) {
                    result.add(cells[1].trim() + " (" + cells[2].trim() + ")");
                }
            }
            return result;
        }
    }
}
