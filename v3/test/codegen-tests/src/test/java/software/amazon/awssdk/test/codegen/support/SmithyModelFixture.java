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

package software.amazon.awssdk.test.codegen.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * A canonical Smithy model split over several files, written to a model directory in a chosen file-creation order.
 *
 * <p>{@link #weather()} is a small AWS-style service: a JSON 1.0 / SigV4 service with two operations, operation and
 * service errors, a resource-free structure and trait applications in a separate file (so that trait order depends on
 * the file import order unless the generator fixes it). {@link #weatherDeclaredInReverse()} declares the same shapes,
 * operations and errors in reverse order within the same files.
 */
public final class SmithyModelFixture {

    public static final String SERVICE = "example.weather#Weather";
    public static final String PACKAGE = "software.amazon.awssdk.services.weather";
    /** Package directory of the generated service sources. */
    public static final String PACKAGE_PATH = PACKAGE.replace('.', '/');

    private static final String HEADER = "$version: \"2\"\nnamespace example.weather\n\n";

    private final Map<String, String> files;

    private SmithyModelFixture(Map<String, String> files) {
        this.files = Collections.unmodifiableMap(new LinkedHashMap<>(files));
    }

    /** A fixture with exactly {@code files} (relative path to content), created in map order by {@link #write}. */
    public static SmithyModelFixture of(Map<String, String> files) {
        return new SmithyModelFixture(files);
    }

    public static SmithyModelFixture weather() {
        return weather(false);
    }

    /** {@link #weather()} with every operation list, error list and shape definition in reverse order. */
    public static SmithyModelFixture weatherDeclaredInReverse() {
        return weather(true);
    }

    private static SmithyModelFixture weather(boolean reverse) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("weather.smithy", HEADER
                                    + "@aws.protocols#awsJson1_0\n"
                                    + "@aws.auth#sigv4(name: \"weather\")\n"
                                    + "service Weather {\n"
                                    + "    version: \"2006-03-01\"\n"
                                    + "    operations: " + list(reverse, "GetForecast", "ListCities") + "\n"
                                    + "    errors: " + list(reverse, "ServiceFault", "AccessDenied") + "\n"
                                    + "}\n");
        files.put("operations/forecast.smithy", HEADER + join(reverse,
            "operation GetForecast {\n"
            + "    input := { city: String }\n"
            + "    output := { forecast: Forecast }\n"
            + "    errors: " + list(reverse, "CityNotFound", "Throttled") + "\n"
            + "}\n",
            "structure Forecast {\n    summary: String\n    high: Integer\n}\n"));
        files.put("operations/cities.smithy", HEADER
                                              + "operation ListCities {\n"
                                              + "    input := { prefix: String }\n"
                                              + "    output := { cities: CityList }\n"
                                              + "}\n\n"
                                              + "list CityList { member: String }\n");
        files.put("errors.smithy", HEADER + join(reverse,
            "@error(\"client\")\nstructure CityNotFound { message: String }\n",
            "@error(\"client\")\nstructure Throttled { message: String }\n",
            "@error(\"server\")\nstructure ServiceFault { message: String }\n",
            "@error(\"client\")\nstructure AccessDenied { message: String }\n"));
        files.put("traits/documentation.smithy", HEADER + join(reverse,
            "apply Forecast @documentation(\"A forecast.\")\n",
            "apply Forecast @sensitive\n",
            "apply GetForecast @documentation(\"Gets a forecast.\")\n"));
        return new SmithyModelFixture(files);
    }

    /** Relative paths of the model files, in declaration order. */
    public List<String> files() {
        return List.copyOf(files.keySet());
    }

    /** Writes every file into {@code directory}, in declaration order. */
    public Path write(Path directory) {
        return write(directory, files());
    }

    /**
     * Writes every file into {@code directory}, creating them in {@code creationOrder} (a permutation of
     * {@link #files()}), so that directory listing order differs between otherwise identical model directories.
     */
    public Path write(Path directory, List<String> creationOrder) {
        if (creationOrder.size() != files.size() || !new TreeSet<>(creationOrder).equals(new TreeSet<>(files.keySet()))) {
            throw new IllegalArgumentException("Creation order " + creationOrder + " is not a permutation of "
                                               + files.keySet());
        }
        try {
            Files.createDirectories(directory);
            for (String file : creationOrder) {
                Path target = directory.resolve(file);
                Files.createDirectories(target.getParent());
                Files.writeString(target, Objects.requireNonNull(files.get(file)), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to write model fixture to " + directory, e);
        }
        return directory;
    }

    /** A copy of this fixture with {@code file} added or replaced. */
    public SmithyModelFixture with(String file, String content) {
        Map<String, String> copy = new LinkedHashMap<>(files);
        copy.put(file, content);
        return new SmithyModelFixture(copy);
    }

    /** Relative paths of every model file, reversed. */
    public List<String> reversedFiles() {
        List<String> reversed = new ArrayList<>(files.keySet());
        Collections.reverse(reversed);
        return reversed;
    }

    /** Every relative path of the fixture, sorted. */
    public Set<String> sortedFiles() {
        return new TreeSet<>(files.keySet());
    }

    private static String list(boolean reverse, String... names) {
        List<String> ordered = new ArrayList<>(List.of(names));
        if (reverse) {
            Collections.reverse(ordered);
        }
        return ordered.toString();
    }

    private static String join(boolean reverse, String... blocks) {
        List<String> ordered = new ArrayList<>(List.of(blocks));
        if (reverse) {
            Collections.reverse(ordered);
        }
        return String.join("\n", ordered);
    }
}
