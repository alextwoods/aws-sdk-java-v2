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

package software.amazon.awssdk.enhanced.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * The enhanced client is built only on public V3 APIs (design, "High-level libraries and launch parity"): every SDK
 * type its compiled classes reference outside its own packages is a public (non-{@code internal}) type of the
 * generated DynamoDB artifact, sdk-core or aws-core. The artifact-level edges are enforced by the dependency policy;
 * this checks the class level, which the artifact level cannot see (for example a reference to a generated
 * {@code internal.operation} descriptor or to a pipeline type reachable only transitively).
 */
class PublicApiBoundaryTest {
    private static final String OWN_PREFIX = "software/amazon/awssdk/enhanced/dynamodb/";
    private static final Pattern SDK_TYPE = Pattern.compile("software/amazon/awssdk/[A-Za-z0-9_/$]+");

    @Test
    void compiledClassesReferenceOnlyPublicTypesOfTheAllowedArtifacts() throws Exception {
        Path classes = location(DynamoDbEnhancedClient.class);
        Set<Path> allowed = Set.of(classes, location(DynamoDbClient.class), location(SdkBytes.class),
                                   location(AwsRequestOverrideConfiguration.class));
        Map<String, Set<String>> violations = new TreeMap<>();
        int scanned = 0;
        try (Stream<Path> files = Files.walk(classes)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".class")).toList()) {
                scanned++;
                String owner = classes.relativize(file).toString();
                for (String type : referencedSdkTypes(file)) {
                    String problem = check(type, allowed);
                    if (problem != null) {
                        violations.computeIfAbsent(owner, k -> new TreeSet<>()).add(type + ": " + problem);
                    }
                }
            }
        }
        assertThat(scanned).isGreaterThan(300);
        assertThat(violations).isEmpty();
    }

    private static String check(String type, Set<Path> allowed) throws URISyntaxException {
        if (type.startsWith(OWN_PREFIX)) {
            return null;
        }
        if (List.of(type.split("/")).contains("internal")) {
            return "internal package of another module";
        }
        Class<?> resolved;
        try {
            resolved = Class.forName(type.replace('/', '.'), false, PublicApiBoundaryTest.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return "does not resolve";
        }
        Path location = location(resolved);
        return allowed.contains(location) ? null : "comes from " + location;
    }

    private static Set<String> referencedSdkTypes(Path classFile) throws IOException {
        // Constant-pool UTF-8 entries hold every referenced class name and descriptor in internal form.
        String text = new String(Files.readAllBytes(classFile), StandardCharsets.ISO_8859_1);
        Set<String> types = new TreeSet<>();
        Matcher m = SDK_TYPE.matcher(text);
        while (m.find()) {
            types.add(m.group());
        }
        return types;
    }

    private static Path location(Class<?> type) throws URISyntaxException {
        CodeSource source = type.getProtectionDomain().getCodeSource();
        URL url = source.getLocation();
        return Path.of(url.toURI()).toAbsolutePath().normalize();
    }
}
