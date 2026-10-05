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

package software.amazon.awssdk.awscodegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AwsCodegenIntegrationsTest {

    @TempDir
    Path temp;

    @Test
    void ordersByOrderThenIdentifierIndependentOfEnumerationOrder() {
        List<AwsCodegenIntegration> integrations = new ArrayList<>(List.of(
            new Simple("aws.b", 0), new Simple("aws.a", 0), new Simple("aws.first", Integer.MIN_VALUE),
            new Simple("aws.last", Integer.MAX_VALUE), new Simple("aws.a-b", 0), new Simple("aws.a.b", 0),
            new Simple("aws.mid", 5)));
        List<String> expected = List.of("aws.first", "aws.a", "aws.a-b", "aws.a.b", "aws.b", "aws.mid", "aws.last");

        Random random = new Random(42);
        for (int i = 0; i < 20; i++) {
            Collections.shuffle(integrations, random);
            assertEquals(expected, identifiers(AwsCodegenIntegrations.order(integrations)));
        }
    }

    @Test
    void rejectsDuplicateIdentifiersNamingBothIntegrations() {
        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> AwsCodegenIntegrations.order(
            List.of(new Simple("aws.dup", 0), new Other("aws.dup"))));
        assertTrue(e.getMessage().contains(Other.class.getName() + " and " + Simple.class.getName()
                                           + " share identifier 'aws.dup'"), e.getMessage());
    }

    @Test
    void rejectsMalformedIdentifiers() {
        for (String identifier : new String[] {null, "", "Aws.core", "aws..core", "aws.", ".aws", "1aws", "aws core",
                                               "aws_core"}) {
            AwsCodegenException e = assertThrows(AwsCodegenException.class,
                                                 () -> AwsCodegenIntegrations.order(List.of(new Simple(identifier, 0))),
                                                 String.valueOf(identifier));
            assertTrue(e.getMessage().contains("invalid identifier '" + identifier + "'"), e.getMessage());
        }
    }

    @Test
    void rejectsSmithyOrderingMechanismsAndRenamedIntegrations() {
        AwsCodegenIntegration prioritized = new Simple("aws.priority", 0) {
            @Override
            public byte priority() {
                return 1;
            }
        };
        AwsCodegenIntegration before = new Simple("aws.before", 0) {
            @Override
            public List<String> runBefore() {
                return List.of("aws.other");
            }
        };
        AwsCodegenIntegration after = new Simple("aws.after", 0) {
            @Override
            public List<String> runAfter() {
                return List.of("aws.other");
            }
        };
        AwsCodegenIntegration renamed = new Simple("aws.renamed", 0) {
            @Override
            public String name() {
                return "something-else";
            }
        };

        AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> AwsCodegenIntegrations.order(
            List.of(prioritized, before, after, renamed)));
        for (String identifier : List.of("aws.priority", "aws.before", "aws.after")) {
            assertTrue(e.getMessage().contains("'" + identifier + "'") && e.getMessage().contains("runAfter ordering"),
                       e.getMessage());
        }
        assertTrue(e.getMessage().contains("overrides name() as 'something-else'"), e.getMessage());
    }

    @Test
    void discoversServiceLoaderRegistrationsInExecutionOrder() throws IOException {
        Path services = Files.createDirectories(temp.resolve("META-INF/services"));
        Files.writeString(services.resolve(AwsCodegenIntegration.class.getName()),
                          "# comment\n" + Second.class.getName() + "\n" + First.class.getName() + "\n");
        try (URLClassLoader loader = new URLClassLoader(new URL[] {temp.toUri().toURL()}, getClass().getClassLoader())) {
            assertEquals(List.of("aws.test.first", "aws.test.second"),
                         identifiers(AwsCodegenIntegrations.discover(loader)));
        }
    }

    @Test
    void unloadableRegistrationsFailWithATypedDiagnostic() throws IOException {
        Path services = Files.createDirectories(temp.resolve("META-INF/services"));
        Files.writeString(services.resolve(AwsCodegenIntegration.class.getName()), "com.example.Missing\n");
        try (URLClassLoader loader = new URLClassLoader(new URL[] {temp.toUri().toURL()}, getClass().getClassLoader())) {
            AwsCodegenException e = assertThrows(AwsCodegenException.class, () -> AwsCodegenIntegrations.discover(loader));
            assertTrue(e.getMessage().contains("com.example.Missing"), e.getMessage());
        }
    }

    @Test
    void awsCodegenRegistersNoIntegrationsOfItsOwnYet() {
        assertEquals(List.of(), AwsCodegenIntegrations.discover(AwsCodegen.class.getClassLoader()));
    }

    private static List<String> identifiers(List<AwsCodegenIntegration> integrations) {
        return integrations.stream().map(AwsCodegenIntegration::identifier).toList();
    }

    static class Simple implements AwsCodegenIntegration {
        private final String identifier;
        private final int order;

        Simple(String identifier, int order) {
            this.identifier = identifier;
            this.order = order;
        }

        @Override
        public String identifier() {
            return identifier;
        }

        @Override
        public int order() {
            return order;
        }
    }

    static final class Other extends Simple {
        Other(String identifier) {
            super(identifier, 0);
        }
    }

    public static final class First extends Simple {
        public First() {
            super("aws.test.first", 0);
        }
    }

    public static final class Second extends Simple {
        public Second() {
            super("aws.test.second", 0);
        }
    }
}
